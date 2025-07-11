package com.portal26.ingest.services;

import com.portal26.ingest.models.IngestPayload;
import org.springframework.stereotype.Service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

@Service
public class IngestQueueService {
    private final BlockingQueue<IngestPayload> queue = new LinkedBlockingQueue<>(10000);

    public void enqueue(IngestPayload payload) {
        queue.offer(payload); // apply backpressure logic if needed
    }

    public BlockingQueue<IngestPayload> getQueue() {
        return queue;
    }
}