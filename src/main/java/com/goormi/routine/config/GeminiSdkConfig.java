package com.goormi.routine.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.google.genai.Client;

@Configuration
public class GeminiSdkConfig {

	@Value("${gemini.api.key}")
	private String apiKey;

	@Bean
	public Client geminiClient(){
		return Client.builder()
			.apiKey(apiKey)
			.build();
	}
}
