package com.goormi.routine.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {
    
    @Override
    public void configureMessageBroker(MessageBrokerRegistry config) {
        config.enableSimpleBroker("/topic", "/queue");
        config.setApplicationDestinationPrefixes("/app");
    }
    
    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint("/ws")
                .setAllowedOrigins(
                    "http://localhost:3000",
                    "http://localhost:8080",
                    "http://localhost:5173",
                    "http://54.180.93.1:3000",
                    "http://54.180.93.1:8080",
                    "https://54.180.93.1:8080",
                    "http://routine-it-frontend-1757331119.s3-website.ap-northeast-2.amazonaws.com",
                    "https://d17wq6hjjpeoqd.cloudfront.net",
                    "http://15.164.98.221:8080"
                )
                .withSockJS();
    }
}