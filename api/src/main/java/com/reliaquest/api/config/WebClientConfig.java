package com.reliaquest.api.config;

import io.netty.channel.ChannelOption;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

/**
 * EmployeeApiClient depends on this bean named employeeWebClient.
 * Spring will automatically inject it because WebClientConfig defines it as a @Bean
 */

@Configuration
public class WebClientConfig {

    private static final Logger log = LoggerFactory.getLogger(WebClientConfig.class);

    @Bean
    WebClient employeeWebClient(
            @Value("${employee.mock.base-url:http://localhost:8112/api/v1/employee}") String baseUrl) {
        HttpClient http = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 2_000)
                .responseTimeout(Duration.ofSeconds(3));

        return WebClient.builder()
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(http))
                .filter(logRequest())
                .filter(logResponse())
                .build();
    }

    private static ExchangeFilterFunction logRequest() {
        return (req, next) -> {
            log.debug("→ {} {}", req.method(), req.url());
            return next.exchange(req);
        };
    }

    private static ExchangeFilterFunction logResponse() {
        return (req, next) -> next.exchange(req)
                .doOnSuccess(resp -> log.debug("← {} {} -> {}", req.method(), req.url(), resp.statusCode()))
                .doOnError(ex -> log.warn("✖ {} {} failed: {}", req.method(), req.url(), ex.toString()));
    }
}
