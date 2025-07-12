package com.portal26.ingest.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal26.ingest.models.IngestRequest;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class S3BatchWriteService {

    private final IngestQueueService queueService;
    private final MeterRegistry meterRegistry;
    private final ObjectMapper objectMapper;
    private final S3Client s3Client;
    private final String bucketName;
    private final long maxBatchSizeBytes;
    private final long maxDelayMs;

    private final Executor flushExecutor;
    private final Executor batchingExecutor;

    public S3BatchWriteService(
            IngestQueueService queueService,
            MeterRegistry meterRegistry,
            ObjectMapper objectMapper,
            S3Client s3Client,
            @Value("${s3.bucket}") String bucketName,
            @Value("${batch.max-bytes:5242880}") long maxBatchSizeBytes,
            @Value("${batch.max-delay-ms:5000}") long maxDelayMs,
            @Qualifier("flushExecutor") Executor flushExecutor,
            @Qualifier("batchingExecutor") Executor batchingExecutor
    ) {
        this.queueService = queueService;
        this.meterRegistry = meterRegistry;
        this.objectMapper = objectMapper;
        this.s3Client = s3Client;
        this.bucketName = bucketName;
        this.maxBatchSizeBytes = maxBatchSizeBytes;
        this.maxDelayMs = maxDelayMs;
        this.flushExecutor = flushExecutor;
        this.batchingExecutor = batchingExecutor;
    }

    @PostConstruct
    public void startBatchProcessor() {
        log.info("Starting batch processor thread...");
        batchingExecutor.execute(this::batchLoop);
    }

    @PreDestroy
    public void shutdown() {
        log.info("Shutting down batch processor (thread pool managed by Spring)");
    }

    private void batchLoop() {
        List<IngestRequest> currentBatch = new ArrayList<>();
        long currentBatchSize = 0;
        long batchStartTime = 0;

        while (!Thread.currentThread().isInterrupted()) {
            try {
                IngestRequest item = queueService.getQueue().poll(200, TimeUnit.MILLISECONDS);
                long now = System.currentTimeMillis();

                if (item != null) {
                    String json = objectMapper.writeValueAsString(item);
                    byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
                    long itemSize = bytes.length;

                    if (currentBatch.isEmpty()) {
                        batchStartTime = now;
                        log.debug("New batch started at {}", Instant.ofEpochMilli(batchStartTime));
                    }

                    if ((currentBatchSize + itemSize) > maxBatchSizeBytes) {
                        log.info("Size threshold reached ({} bytes), flushing batch of {} events", currentBatchSize, currentBatch.size());
                        submitBatch(new ArrayList<>(currentBatch));
                        currentBatch.clear();
                        currentBatchSize = 0;
                        batchStartTime = now;
                    }

                    currentBatch.add(item);
                    currentBatchSize += itemSize;
                    meterRegistry.counter("s3.ingest.events").increment();
                }

                if (!currentBatch.isEmpty() && (now - batchStartTime >= maxDelayMs)) {
                    log.info("Time threshold reached ({}ms), flushing batch of {} events", now - batchStartTime, currentBatch.size());
                    submitBatch(new ArrayList<>(currentBatch));
                    currentBatch.clear();
                    currentBatchSize = 0;
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception e) {
                log.error("Unexpected error in batch loop", e);
            }
        }

        if (!currentBatch.isEmpty()) {
            log.info("Flushing final batch of {} events on shutdown", currentBatch.size());
            submitBatch(currentBatch);
        }
    }

    private void submitBatch(List<IngestRequest> batch) {
        flushExecutor.execute(() -> {
            try {
                writeToS3(batch);
                meterRegistry.counter("s3.flush.success").increment();
            } catch (Exception e) {
                log.error("Failed to write batch to S3", e);
                meterRegistry.counter("s3.flush.failure").increment();
            }
        });
    }

    private void writeToS3(List<IngestRequest> batch) throws Exception {
        StringBuilder sb = new StringBuilder(batch.size() * 200);
        for (IngestRequest req : batch) {
            sb.append(objectMapper.writeValueAsString(req)).append("\n");
        }

        String key = "batches/" + Instant.now() + "-" + UUID.randomUUID() + ".json";

        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(key)
                        .contentType("application/json")
                        .build(),
                RequestBody.fromString(sb.toString(), StandardCharsets.UTF_8)
        );

        log.info("Wrote batch of {} events to S3: {}", batch.size(), key);
    }
}
