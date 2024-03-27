package com.example.Springzipservice.service;

import com.example.Springzipservice.config.StorageProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferLimitException;
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
import java.util.concurrent.atomic.AtomicLong;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@Slf4j
@RequiredArgsConstructor
public class FileUploadService {
    private final StorageProperties storageProperties;

    private final long MAX_SIZE = 262144; // 256 KB

    public Mono<Boolean> isValid(List<FilePart> fileParts) {
        return calculateTotalSize(fileParts)
                .map(totalSize -> totalSize <= MAX_SIZE);
    }

    public Mono<Long> calculateTotalSize(List<FilePart> fileParts) {
        return Flux.fromIterable(fileParts)
                .flatMap(FilePart::content)
                .map(DataBuffer::readableByteCount)
                .reduce(0L, Long::sum);
    }

  public Flux<DataBuffer> processFiles(List<FilePart> fileParts) {
    ConcurrentMap<String, Boolean> processedFiles = new ConcurrentHashMap<>();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ZipOutputStream zos = new ZipOutputStream(baos);
    AtomicLong zippedSize = new AtomicLong(0);

    // Calculate the total size of all files
    Mono<Long> totalSizeMono = Flux.fromIterable(fileParts)
            .flatMap(filePart -> filePart.content())
            .map(dataBuffer -> (long) dataBuffer.readableByteCount())
            .reduce(Long::sum);

    return totalSizeMono.flatMapMany(totalSize ->
            Flux.fromIterable(fileParts)
                    .concatMap(filePart -> { // Use concatMap instead of flatMap to ensure sequential processing
                        return filePart.content()
                                .collectList() // Collect the DataBuffers into a List
                                .flatMap(dataBuffers -> {
                                    try {
                                        String filenameWithType = filePart.filename();
                                        if (processedFiles.putIfAbsent(filenameWithType, true) != null) {
                                            log.info("Skipping duplicate file: " + filenameWithType);
                                            return Mono.empty(); // Skip this file if it has already been processed
                                        }
                                        zos.putNextEntry(new ZipEntry(filenameWithType));

                                        // Read and write the file in chunks
                                        byte[] bytes = new byte[1024];
                                        int length;
                                        for (DataBuffer dataBuffer : dataBuffers) {
                                            try (InputStream is = dataBuffer.asInputStream()) {
                                                while ((length = is.read(bytes)) != -1) {
                                                    zos.write(bytes, 0, length);
                                                    zippedSize.addAndGet(length);

                                                    // Calculate progress
                                                    double progress = (double) zippedSize.get() / totalSize;
                                                    log.info(String.format("Progress: %.2f%%\n", progress * 100));
                                                    log.info("Zipped file size: " + zippedSize.get() + " bytes");
                                                }
                                            }
                                        }

                                        log.info("Added file: " + filenameWithType + " to zip: allFiles.zip");
                                        log.info(" File at: " + storageProperties.getLocation());
                                        zos.closeEntry(); // Close the entry here
                                        return Mono.just(dataBuffers); // Emit the DataBuffers again
                                    } catch (IOException e) {
                                        return Mono.error(new RuntimeException(e));
                                    }
                                })
                                .flatMapIterable(dataBuffers -> dataBuffers); // Convert the Mono<List<DataBuffer>> to Flux<DataBuffer>
                    })
                    .doOnComplete(() -> {
                        try {
                            // Close the ZipOutputStream after all files have been processed
                            zos.close();

                            // Save the zipped file to the output directory of the project
                            Path path = Paths.get(storageProperties.getLocation()+"/allFiles.zip");
                            Files.createDirectories(path.getParent()); // Create the directory if it does not exist
                            Files.write(path, baos.toByteArray());
                            log.info("Zipped file saved to: " + path);
                        } catch (IOException e) {
                            log.error("An error occurred while saving the zip file: " + e.getMessage(), e);
                        }
                    })
                    .doOnError(e -> {
                        try {
                            zos.close();
                        } catch (IOException ioException) {
                            log.error("An error occurred while closing the ZipOutputStream: ", ioException);
                        }
                        log.error("An error occurred during file processing: " + e.getMessage(), e);
                    })
    );
}
}