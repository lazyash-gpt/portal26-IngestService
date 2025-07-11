package com.portal26.ingest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableAsync
@EnableScheduling
public class IngestApplication {

	public static void main(String[] args) {
		System.out.println("Hello World");
		SpringApplication.run(IngestApplication.class, args);
	}

}
