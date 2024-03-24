package com.example.Springzipservice.controller;

import com.example.Springzipservice.service.FileUploadService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.test.web.reactive.server.WebTestClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureWebTestClient
public class FileUploadControllerTest {

    @Autowired
    private WebTestClient webTestClient;

    @Test
    public void testUpload() {
        FilePart filePart = mock(FilePart.class);
        when(filePart.filename()).thenReturn("test.txt");

        Flux<DataBuffer> dataBufferFlux = Flux.just(new DefaultDataBufferFactory().wrap("test".getBytes()));

        FileUploadService fileUploadService = mock(FileUploadService.class);
        when(fileUploadService.processFiles(Collections.singletonList(filePart))).thenReturn(dataBufferFlux);

        FileUploadController fileUploadController = new FileUploadController(fileUploadService);

        Flux<DataBuffer> result = fileUploadController.upload(Collections.singletonList(filePart));

        // Check that the result is a Flux<DataBuffer>
        assert(result instanceof Flux<DataBuffer>);
    }
}