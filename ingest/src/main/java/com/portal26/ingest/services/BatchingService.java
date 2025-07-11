package com.portal26.ingest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal26.ingest.models.IngestRequest;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchingService {

    private final IngestQueueService queueService;
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;
    private final String bucketName;
    private final long maxBatchSize;

    public BatchingService(IngestQueueService queueService, ObjectMapper objectMapper, S3Client s3Client,
                           @Value("${batch.max-bytes:5242880}") long maxBatchSize,
                           @Value("${s3.bucket}") String bucketName) {
        this.queueService = queueService;
        this.objectMapper = objectMapper;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.maxBatchSize = maxBatchSize;
    }

    @Scheduled(fixedDelayString = "${batch.flush-ms:3000}")
    public void flushBatch() throws IOException {
        List<IngestRequest> batch = new ArrayList<>();
        long totalSize = 0;

        while (!queueService.getQueue().isEmpty() && totalSize < maxBatchSize) {
            IngestRequest payload = queueService.getQueue().poll();
            if (payload == null) break;
            batch.add(payload);
            String json = objectMapper.writeValueAsString(payload);
            long size = json.getBytes(StandardCharsets.UTF_8).length;
            totalSize+=size;
        }

        if (!batch.isEmpty()) writeToS3(batch);
    }

    private void writeToS3(List<IngestRequest> batch) throws IOException {
        if (batch.isEmpty()) return;

        StringBuilder sb = new StringBuilder();
        for (IngestRequest req : batch) {
            sb.append(objectMapper.writeValueAsString(req)).append("\n");
        }

        String key = "batch-" + System.currentTimeMillis() + ".json";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                RequestBody.fromString(sb.toString())
        );
    }
}
