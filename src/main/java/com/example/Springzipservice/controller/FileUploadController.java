package com.example.Springzipservice.controller;

import com.example.Springzipservice.service.FileUploadService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.List;

@OpenAPIDefinition(info = @Info(title = "File Upload API", version = "1.0", description = "APIs for Uploading files"))
@RestController
public class FileUploadController {

    private final FileUploadService fileUploadService;

    public FileUploadController(FileUploadService fileUploadService) {
        this.fileUploadService = fileUploadService;
    }

    @Operation(summary = "Upload multiple files and get them as a zip")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Flux<DataBuffer> upload(@RequestPart("files") List<FilePart> fileParts) {
        return fileUploadService.processFiles(fileParts);
    }
}