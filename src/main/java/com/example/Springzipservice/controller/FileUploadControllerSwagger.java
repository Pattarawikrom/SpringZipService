package com.example.Springzipservice.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.RequestPart;
import reactor.core.publisher.Flux;

import java.util.List;

public interface FileUploadControllerSwagger {

    @Operation(summary = "Upload multiple files and get them as a zip")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Files uploaded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "413", description = "File size exceeds the maximum limit"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    Flux<DataBuffer> upload(@RequestPart("files") List<FilePart> fileParts);
}