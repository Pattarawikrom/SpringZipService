package com.example.Springzipservice.service;

import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

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
                            .doOnNext(dataBuffer -> {
                                try {
                                    String filename = filePart.filename();
                                    if (processedFiles.putIfAbsent(filename, true) != null) {
                                        System.out.println("Skipping duplicate file: " + filename);
                                        return; // Skip this file if it has already been processed
                                    }
                                    String uniqueFilename = filename + "_" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmss"));
                                    zos.putNextEntry(new ZipEntry(uniqueFilename));
                                    zos.write(dataBuffer.asInputStream().readAllBytes());
                                    zos.closeEntry();

                                    System.out.println("Added file: " + filename + " to zip: " + uniqueFilename);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .doOnComplete(() -> {
                                try {
                                    zos.close();
                                    // Save the zipped file to the output directory of the project
                                    Path path = Paths.get(System.getProperty("user.dir"), "src/main/java/com/example/Springzipservice/output", filePart.filename() + ".zip");
                                    Files.createDirectories(path.getParent()); // Create the directory if it does not exist
                                    Files.write(path, baos.toByteArray());
                                    System.out.println("Zipped file saved to: " + path);
                                } catch (IOException e) {
                                    throw new RuntimeException(e);
                                }
                            })
                            .map(dataBuffer -> new DefaultDataBufferFactory().wrap(baos.toByteArray()))
                            .onErrorResume(e -> {
                                System.err.println("An error occurred while processing the file: " + e.getMessage());
                                return Flux.empty();
                            });
                });
    }
}
