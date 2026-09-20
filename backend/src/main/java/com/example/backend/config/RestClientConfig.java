package com.example.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.time.Duration;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        // 타임아웃이 없으면 Gemini/공공데이터 API가 응답을 안 줄 때 요청이 무한정
        // 걸려있어(재시도 로직도 503/429 응답에만 동작하고 행에는 동작 안 함) 목록/상세
        // 화면이 영영 안 끝나는 것처럼 보일 수 있다. 연결/응답 각각에 상한을 둔다.
        HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
        JdkClientHttpRequestFactory requestFactory = new JdkClientHttpRequestFactory(httpClient);
        requestFactory.setReadTimeout(Duration.ofSeconds(15));

        return RestClient.builder().requestFactory(requestFactory).build();
    }

}