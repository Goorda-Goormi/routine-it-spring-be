package com.goormi.routine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.http.HttpMethod;

@Configuration
public class StorageSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain storageChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/storage/**")
                .csrf(AbstractHttpConfigurer::disable)
                // ★ 전역 CorsConfigurationSource 빈을 사용해서 CORS 켜기
                .cors(c -> {})
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(a -> a
                        // ★ 프리플라이트 허용
                        .requestMatchers(HttpMethod.OPTIONS, "/api/storage/**").permitAll()
                        .anyRequest().permitAll()
                );

        return http.build();
    }
}

