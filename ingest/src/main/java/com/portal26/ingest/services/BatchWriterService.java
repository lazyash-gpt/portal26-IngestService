//package com.portal26.ingest.services;
//
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.portal26.ingest.models.IngestRequest;
//import io.micrometer.core.instrument.MeterRegistry;
//import jakarta.annotation.PostConstruct;
//import lombok.RequiredArgsConstructor;
//import org.springframework.scheduling.annotation.Async;
//import org.springframework.stereotype.Component;
//import software.amazon.awssdk.core.sync.RequestBody;
//import software.amazon.awssdk.services.s3.S3Client;
//import software.amazon.awssdk.services.s3.model.PutObjectRequest;
//
//import java.io.IOException;
//import java.nio.charset.StandardCharsets;
//import java.util.ArrayList;
//import java.util.List;
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.TimeUnit;
//
//@Component
//@RequiredArgsConstructor
//public class BatchWriterService {
//
//        private final BlockingQueue<IngestRequest> queue;
//        private final ObjectMapper objectMapper;
//        private final S3Client s3Client;
//     //   private final MeterRegistry meterRegistry;
//
//        private static final long MAX_BATCH_SIZE_BYTES = 5 * 1024 * 1024; // 5MB
//        private static final long MAX_BATCH_WAIT_MS = 3000;
//
//        public void enqueue(IngestRequest request) {
//            queue.offer(request);
//        }
//
//        @PostConstruct
//        public void startBatchWriter() {
//            System.out.println("In postcontruct");
//            List<IngestRequest> batch = new ArrayList<>();
//            long batchSizeBytes = 0;
//            long lastFlushTime = System.currentTimeMillis();
//
//            while (true) {
//                try {
//                    IngestRequest req = queue.poll(100, TimeUnit.MILLISECONDS);
//                    long now = System.currentTimeMillis();
//
//                    if (req != null) {
//                        String json = objectMapper.writeValueAsString(req);
//                        long size = json.getBytes(StandardCharsets.UTF_8).length;
//
//                        if (batchSizeBytes + size > MAX_BATCH_SIZE_BYTES ||
//                                now - lastFlushTime > MAX_BATCH_WAIT_MS) {
//                            flush(batch);
//                            batch = new ArrayList<>();
//                            batchSizeBytes = 0;
//                            lastFlushTime = now;
//                        }
//
//                        batch.add(req);
//                        batchSizeBytes += size;
//                    } else if (!batch.isEmpty() && now - lastFlushTime > MAX_BATCH_WAIT_MS) {
//                        flush(batch);
//                        batch = new ArrayList<>();
//                        batchSizeBytes = 0;
//                        lastFlushTime = now;
//                    }
//                } catch (Exception e) {
//                    e.printStackTrace(); // Replace with proper logging in prod
//                }
//            }
//        }
//
  //       private void flush(List<IngestRequest> batch) throws IOException {
//            if (batch.isEmpty()) return;
//
//            StringBuilder sb = new StringBuilder();
//            for (IngestRequest req : batch) {
//                sb.append(objectMapper.writeValueAsString(req)).append("\n");
//            }
//
//            String key = "batch-" + System.currentTimeMillis() + ".json";
//
//            s3Client.putObject(
//                    PutObjectRequest.builder()
//                            .bucket(System.getenv("S3_BUCKET"))
//                            .key(key)
//                            .build(),
//                    RequestBody.fromString(sb.toString())
//            );

         //   meterRegistry.counter("s3.write.count").increment();
        //    meterRegistry.gauge("s3.write.batch.size", batch.size());
//        }
//}
//
