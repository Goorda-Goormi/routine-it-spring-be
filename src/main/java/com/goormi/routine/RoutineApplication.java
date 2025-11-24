package com.goormi.routine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@ConditionalOnProperty(name = "scheduler.enabled", havingValue = "true", matchIfMissing = true)
@EnableScheduling
@EnableAsync
@EnableCaching
public class RoutineApplication {
	
	public static void main(String[] args) {
		SpringApplication.run(RoutineApplication.class, args);
	}

}
