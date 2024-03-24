package com.example.Springzipservice.service;

import com.example.Springzipservice.config.Constants;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
public class FileUploadService {

    public Flux<DataBuffer> processFiles(List<FilePart> fileParts) {
        ConcurrentMap<String, Boolean> processedFiles = new ConcurrentHashMap<>();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ZipOutputStream zos = new ZipOutputStream(baos);

        return Flux.fromIterable(fileParts)
                .flatMap(filePart -> {
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

                                    // Read and write the file in chunks
                                    // This is done to avoid loading the entire file into memory
                                    byte[] bytes = new byte[1024];
                                    int length;
                                    for (DataBuffer dataBuffer : dataBuffers) {
                                        try (InputStream is = dataBuffer.asInputStream()) {
                                            while ((length = is.read(bytes)) != -1) {
                                                zos.write(bytes, 0, length);
                                            }
                                        }
                                    }

                                    zos.closeEntry();

                                    System.out.println("Added file: " + filenameWithType + " to zip: allFiles.zip");
                                    return Mono.just(dataBuffers); // Emit the DataBuffers again
                                } catch (IOException e) {
                                    return Mono.error(new RuntimeException(e));
                                }
                            })
                            .flatMapIterable(dataBuffers -> dataBuffers); // Convert the Mono<List<DataBuffer>> to Flux<DataBuffer>
                })
                .doOnComplete(() -> {
                    try {
                        zos.close();
                        // Save the zipped file to the output directory of the project
                        Path path = Paths.get(Constants.OUTPUT_DIRECTORY, "allFiles.zip");
                        Files.createDirectories(path.getParent()); // Create the directory if it does not exist
                        Files.write(path, baos.toByteArray());
                        System.out.println("Zipped file saved to: " + path);
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                });
    }
}