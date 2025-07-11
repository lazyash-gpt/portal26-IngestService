//package com.portal26.ingest.configs;
//
//import com.portal26.ingest.models.IngestRequest;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
//
//import java.util.concurrent.BlockingQueue;
//import java.util.concurrent.Executor;
//import java.util.concurrent.LinkedBlockingQueue;
//
//@Configuration
//public class QueueConfig {
////    @Bean
////    public BlockingQueue<IngestRequest> ingestQueue() {
////        return new LinkedBlockingQueue<>();
////    }
//    @Bean
//    public Executor taskExecutor() {
//        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
//        executor.setCorePoolSize(2);
//        executor.setMaxPoolSize(4);
//        executor.setQueueCapacity(100);
//        executor.setThreadNamePrefix("ingest-");
//        executor.initialize();
//        return executor;
//    }
//}
//
