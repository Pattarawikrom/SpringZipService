package com.example.Springzipservice.controller;

import com.example.Springzipservice.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileUploadController implements FileUploadControllerSwagger {

    private final FileUploadService fileUploadService;

    @Override
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Flux<DataBuffer> upload(@RequestPart("files") List<FilePart> fileParts) {
        return fileUploadService.isValid(fileParts)
            .flatMapMany(isValid -> {
                if (!isValid) {
                    throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "Total file size exceeds the maximum limit.");
                }

                return fileUploadService.processFiles(fileParts);
            });
    }
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Flux<DataBuffer>> handleResponseStatusException(ResponseStatusException ex) {
        DataBuffer buffer = new DefaultDataBufferFactory().wrap(ex.getReason().getBytes(StandardCharsets.UTF_8));
        return new ResponseEntity<>(Flux.just(buffer), ex.getStatusCode());
    }
}