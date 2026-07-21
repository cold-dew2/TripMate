package com.example.backend.global.config;

// Spring Bean 등록을 위해 사용

import org.springframework.context.annotation.Bean;

// Spring 설정 클래스임을 나타냄
import org.springframework.context.annotation.Configuration;

// CORS 설정 정보를 등록하기 위한 객체
import org.springframework.web.servlet.config.annotation.CorsRegistry;

// Spring MVC 설정을 커스터마이징하기 위한 인터페이스
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


// Spring 설정 클래스
@Configuration
public class CorsConfig {

    // WebMvcConfigurer 객체를 Spring Bean으로 등록
    @Bean
    public WebMvcConfigurer corsConfigurer() {

        // WebMvcConfigurer 구현체 반환
        return new WebMvcConfigurer() {

            // CORS 설정 추가
            @Override
            public void addCorsMappings(
                    CorsRegistry registry
            ) {

                // 모든 URL에 대해 CORS 허용
                registry.addMapping("/**")

                        // localhost:3000 에서 오는 요청 허용
                        // Next.js 개발 서버
                        .allowedOrigins("http://localhost:3000")

                        // 모든 HTTP Method 허용
                        // GET, POST, PUT, PATCH, DELETE 등
                        .allowedMethods("*");
            }
        };
    }
}