package com.example.Springzipservice;

import com.example.Springzipservice.config.StorageProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(StorageProperties.class)
public class SpringZipServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(SpringZipServiceApplication.class, args);
    }

}
