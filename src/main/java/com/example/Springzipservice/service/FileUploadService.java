package com.example.Springzipservice.service;

import com.example.Springzipservice.config.Constants;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileUploadService {

    public Flux<DataBuffer> processFiles(List<FilePart> fileParts) {
        ConcurrentMap<String, Boolean> processedFiles = new ConcurrentHashMap<>();

        return Flux.fromIterable(fileParts)
                .flatMap(filePart -> {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    ZipOutputStream zos = new ZipOutputStream(baos);
                    return filePart.content()
                            .collectList() // Collect the DataBuffers into a List
                            .flatMap(dataBuffers -> {
                                try {
                                    String filenameWithType = filePart.filename();
                                    String filename = filenameWithType.substring(0, filenameWithType.lastIndexOf('.')); // Filename without extension
                                    String fileType = filenameWithType.substring(filenameWithType.lastIndexOf('.') + 1); // File extension
                                    if (processedFiles.putIfAbsent(filenameWithType, true) != null) {
                                        System.out.println("Skipping duplicate file: " + filenameWithType);
                                        return Mono.empty(); // Skip this file if it has already been processed
                                    }
                                    String uniqueFilename = filename + "." + fileType;
                                    zos.putNextEntry(new ZipEntry(uniqueFilename));
                                    for (DataBuffer dataBuffer : dataBuffers) {
                                        zos.write(dataBuffer.asInputStream().readAllBytes());
                                    }
                                    zos.closeEntry();

                                    System.out.println("Added file: " + filenameWithType + " to zip: " + uniqueFilename);
                                    zos.close();


                                    // Save the zipped file to the output directory of the project
                                    Path path = Paths.get(Constants.OUTPUT_DIRECTORY, filename + ".zip");
                                    Files.createDirectories(path.getParent()); // Create the directory if it does not exist
                                    Files.write(path, baos.toByteArray());
                                    System.out.println("Zipped file saved to: " + path);
                                    return Mono.just(dataBuffers); // Emit the DataBuffers again
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .flatMapMany(Flux::fromIterable) // Convert the Mono<List<DataBuffer>> to Flux<DataBuffer>
                            .map(dataBuffer -> new DefaultDataBufferFactory().wrap(baos.toByteArray()))
                            .onErrorResume(e -> {
                                // Handle the exception here. You can log the error, return a default value, etc.
                                System.err.println("An error occurred while processing the file: " + e.getMessage());
                                return Flux.empty(); // Return an empty Flux in case of error
                            });
                });
    }
}