package com.portal26.ingest.configs;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.DefaultCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class AwsConfig {

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(System.getenv().getOrDefault("AWS_REGION", "ap-south-1")))
                .credentialsProvider(DefaultCredentialsProvider.create())
                .build();
    }
}

