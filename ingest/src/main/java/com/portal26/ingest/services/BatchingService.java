package com.portal26.ingest.services;

import com.portal26.ingest.models.IngestPayload;
import org.apache.tomcat.util.http.fileupload.ByteArrayOutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class BatchingService {

    private final IngestQueueService queueService;
    private final S3Client s3Client;
    private final String bucketName;
    private final long maxBatchSize;

    public BatchingService(IngestQueueService queueService, S3Client s3Client,
                           @Value("${batch.max-bytes:5242880}") long maxBatchSize,
                           @Value("${s3.bucket}") String bucketName) {
        this.queueService = queueService;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.maxBatchSize = maxBatchSize;
    }

    @Scheduled(fixedDelayString = "${batch.flush-ms:3000}")
    public void flushBatch() throws IOException {
        List<IngestPayload> batch = new ArrayList<>();
        long totalSize = 0;

        while (!queueService.getQueue().isEmpty() && totalSize < maxBatchSize) {
            IngestPayload payload = queueService.getQueue().poll();
            if (payload == null) break;
            batch.add(payload);
            totalSize += payload.data().length;
        }

        if (!batch.isEmpty()) writeToS3(batch);
    }

    private void writeToS3(List<IngestPayload> batch) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (IngestPayload p : batch) {
            baos.write(p.data());
            baos.write("\n".getBytes());
        }
        Path tmpFile = Files.createTempFile("batch-", ".log");
        Files.write(tmpFile, baos.toByteArray());

        String key = "ingest/" + Instant.now().toString() + "/batch-" + UUID.randomUUID() + ".log";

        s3Client.putObject(PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .build(),
                tmpFile);

        Files.deleteIfExists(tmpFile);
    }
}
