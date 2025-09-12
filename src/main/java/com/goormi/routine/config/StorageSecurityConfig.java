// src/main/java/com/goormi/routine/config/StorageSecurityConfig.java
package com.goormi.routine.config;

import org.springframework.context.annotation.Bean;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;

@Configuration
public class StorageSecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain storageChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/storage/**")
                .authorizeHttpRequests(a -> a.anyRequest().permitAll())
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .formLogin(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .oauth2Login(AbstractHttpConfigurer::disable);
        // JwtAuthenticationFilter 등은 이 체인에는 추가하지 않음
        return http.build();
    }

    // 나머지 엔드포인트는 기존 메인 체인에서 처리(기존 SecurityConfig 유지)
}
