package com.example.f_f.global.config;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;

@Component // 스프링 컴포넌트로 등록
@RequiredArgsConstructor // final 필드를 자동으로 생성자 주입
public class WebClientFactory {

    @Bean
    public WebClient fastApiClient(
            @Value("${fastapi.base-url}") String baseUrl
    ) {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .exchangeStrategies(
                        ExchangeStrategies.builder()
                                .codecs(c -> c.defaultCodecs().maxInMemorySize(2 * 1024 * 1024))
                                .build()
                )
                .build();
    }
}
