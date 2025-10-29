package com.goormi.routine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class GeminiSdkConfig {

	@Bean
	public Client geminiClient(){
		return new Client();
	}
}
