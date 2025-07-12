package com.portal26.ingest.controllers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.portal26.ingest.models.IngestRequest;
import com.portal26.ingest.services.IngestQueueService;
import io.micrometer.core.instrument.MeterRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/ingest")
@Slf4j
public class IngestController {

    private final IngestQueueService queueService;
    private final MeterRegistry meterRegistry;
    private final Set<String> allowedTiers;
    private final ObjectMapper objectMapper;

    public IngestController(IngestQueueService queueService,
                            MeterRegistry meterRegistry,
                            @Value("${allowed-tiers}") Set<String> allowedTiers, ObjectMapper objectMapper) {
        this.queueService = queueService;
        this.meterRegistry = meterRegistry;
        this.allowedTiers = allowedTiers;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public ResponseEntity<?> ingest(@RequestBody IngestRequest request,
                                    @RequestHeader("X-Customer-Tier") String tier) {
        if (!StringUtils.hasText(tier) || !allowedTiers.contains(tier.toLowerCase())) {
            meterRegistry.counter("events.filtered", "tier", tier != null ? tier : "missing").increment();
            log.warn("Filtered event due to invalid/missing tier: {}", tier);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "invalid tier"));
        }
        String body="";
        try{
            body= objectMapper.writeValueAsString(request);
            if (body.length() > 10 * 1024 * 1024) {
                meterRegistry.counter("events.filtered", "reason", "size_limit").increment();
                log.warn("Filtered event due to size constraints");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "invalid size"));
            }
        }catch(Exception e){
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("status", "wrong input"));
        }

        queueService.enqueue(request);
        meterRegistry.counter("events.received").increment();
        log.info("Accepted event from tier: {} | Size: {} bytes", tier, body.length());
        return ResponseEntity.ok(Map.of("status", "success"));
    }
}
