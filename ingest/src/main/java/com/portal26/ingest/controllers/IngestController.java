package com.portal26.ingest.controllers;

import com.portal26.ingest.models.IngestRequest;
import com.portal26.ingest.services.BatchingService;
import com.portal26.ingest.services.IngestQueueService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/ingest")
public class IngestController {

    private final IngestQueueService queueService;
 //   private final MeterRegistry meterRegistry;
    private final Set<String> allowedTiers;

    public IngestController(IngestQueueService queueService,
                        //    MeterRegistry meterRegistry,
                            @Value("${allowed-tiers}") Set<String> allowedTiers) {
        this.queueService = queueService;
   //     this.meterRegistry = meterRegistry;
        this.allowedTiers = allowedTiers;
    }

    @PostMapping
    public ResponseEntity<?> ingest(@RequestBody IngestRequest request,
                                    @RequestHeader("X-Customer-Tier") String tier) {
       // meterRegistry.counter("requests.total").increment();

        if (!allowedTiers.contains(tier)) {
        //    meterRegistry.counter("requests.filtered").increment();
            return ResponseEntity.status(403).body("Forbidden tier");
        }

        queueService.enqueue(request);
        return ResponseEntity.ok().body("success");
    }
}
