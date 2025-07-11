package com.portal26.ingest.controllers;

import com.portal26.ingest.models.IngestPayload;
import com.portal26.ingest.services.IngestQueueService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.actuate.endpoint.web.annotation.RestControllerEndpoint;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Set;

@RestController
@RequestMapping("/ingest")
public class IngestController {
    private final IngestQueueService queueService;
    private Set<String> allowedTiers;

    public IngestController(IngestQueueService queueService,
                            @Value("${allowed-tiers}") Set<String> allowedTiers) {
        this.queueService = queueService;
        this.allowedTiers = allowedTiers;
    }

    @PostMapping
    public ResponseEntity<String> ingest(@RequestHeader("X-Customer-Tier") String tier,
                                         @RequestBody byte[] body) {
        if (!allowedTiers.contains(tier)) return ResponseEntity.status(403).body("Forbidden tier");
        if (body.length < 1024 || body.length > 10 * 1024 * 1024)
            return ResponseEntity.badRequest().body("Invalid body size");

        queueService.enqueue(new IngestPayload(tier, body));
        return ResponseEntity.accepted().body("Accepted");
    }
}
