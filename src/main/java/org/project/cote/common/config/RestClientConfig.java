package org.project.cote.common.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

@Configuration
public class RestClientConfig {

    private static final Duration LEETCODE_CONNECT_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration LEETCODE_READ_TIMEOUT = Duration.ofSeconds(10);

    @Bean
    public RestClient leetCodeRestClient() {
        return RestClient.builder()
                .baseUrl("https://leetcode.com")
                .requestFactory(buildRequestFactory(LEETCODE_CONNECT_TIMEOUT, LEETCODE_READ_TIMEOUT))
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader(HttpHeaders.USER_AGENT,
                        "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36")
                .defaultHeader("Referer", "https://leetcode.com/problemset/")
                .build();
    }

    private SimpleClientHttpRequestFactory buildRequestFactory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectTimeout);
        factory.setReadTimeout(readTimeout);
        return factory;
    }
}
