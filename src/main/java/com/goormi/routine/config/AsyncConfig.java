package com.goormi.routine.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

import lombok.extern.slf4j.Slf4j;

@Configuration
@EnableAsync
@Slf4j
public class AsyncConfig {

	@Bean(name = "aiReviewExecutor")
	public ExecutorService aiReviewExecutor() {
		ExecutorService executor = Executors.newFixedThreadPool(10);
		return executor;
	}
}
