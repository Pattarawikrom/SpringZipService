package com.example.Springzipservice.controller;

import com.example.Springzipservice.service.FileUploadService;
import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.info.Info;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;

import java.util.List;

@OpenAPIDefinition(info = @Info(title = "File Upload API", version = "1.0", description = "API for Uploading files"))
@RestController
@RequiredArgsConstructor
public class FileUploadController {

    private final FileUploadService fileUploadService;

    @Operation(summary = "Upload multiple files and get them as a zip")
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Flux<DataBuffer> upload(@RequestPart("files") List<FilePart> fileParts) {
        return fileUploadService.processFiles(fileParts);
    }
    @ExceptionHandler(ServerWebInputException.class)
    @ResponseStatus(code = HttpStatus.BAD_REQUEST)
    public String handleServerWebInputException(ServerWebInputException ex) {
        return ex.getMessage();
    }
    @ExceptionHandler(MaxUploadSizeExceededException.class)
    @ResponseStatus(HttpStatus.PAYLOAD_TOO_LARGE)
    public String handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return "File size exceeds the maximum limit.";
    }
}