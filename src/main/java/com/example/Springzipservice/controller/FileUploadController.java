package com.example.Springzipservice.controller;

import com.example.Springzipservice.service.FileUploadService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ServerWebInputException;
import reactor.core.publisher.Flux;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class FileUploadController implements FileUploadControllerSwagger {

    private final FileUploadService fileUploadService;

    @Override
    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public Flux<DataBuffer> upload(List<FilePart> fileParts) {
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