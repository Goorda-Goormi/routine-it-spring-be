package com.goormi.routine.domain.calendar.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * WebClient 설정
 */
@Slf4j
@Configuration
public class WebClientConfig {
    @Value("${kakao.calendar.base-url}")
    private String kakaoCalendarBaseUrl;

    /**
     * 카카오 API 호출용 WebClient Bean 등록
     */
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .baseUrl(kakaoCalendarBaseUrl)
                .codecs(configurer -> configurer
                        .defaultCodecs()
                        .maxInMemorySize(1024 * 1024)) // 1MB
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private ExchangeFilterFunction logRequest() {
        return ExchangeFilterFunction.ofRequestProcessor(clientRequest -> {
            log.info("Request: {} {}", clientRequest.method(), clientRequest.url());
            return Mono.just(clientRequest);
        });
    }

    private ExchangeFilterFunction logResponse() {
        return ExchangeFilterFunction.ofResponseProcessor(clientResponse -> {
            log.info("Response Status: {}", clientResponse.statusCode());
            return Mono.just(clientResponse);
        });
    }
}
