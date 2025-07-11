package com.portal26.ingest.services;

import com.portal26.ingest.models.IngestRequest;
import lombok.Getter;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Getter
@Service
public class IngestQueueService {
    private final BlockingQueue<IngestRequest> queue = new LinkedBlockingQueue<>(10000);

    public void enqueue(IngestRequest payload) {
        queue.offer(payload); // apply backpressure logic if needed
    }

}