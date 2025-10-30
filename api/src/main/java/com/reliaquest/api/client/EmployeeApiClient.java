package com.reliaquest.api.client;

import com.reliaquest.api.model.ApiResponse;
import com.reliaquest.api.model.CreateEmployeeRequest;
import com.reliaquest.api.model.Employee;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

/**
 * Thin wrapper around WebClient for calling the mock Employee API.
 * Configured CircuitBreaker (Resilience4j (CircuitBreakerOperator and RetryOperator)),
 * timeout and a fallback (onErrorResume) to return an empty list instead of propagating errors
 * wrap timing in timed(...) so Micrometer metrics are recorded.
 */
@Component
public class EmployeeApiClient {
    private static final Logger log = LoggerFactory.getLogger(EmployeeApiClient.class);
    private final CircuitBreaker cb;
    private final Retry retry;
    private final MeterRegistry meterRegistry;
    private final WebClient webClient;

    public EmployeeApiClient(WebClient employeeWebClient, MeterRegistry meterRegistry) {
        this.webClient = employeeWebClient;
        this.meterRegistry = meterRegistry;

        CircuitBreakerConfig cbConfig = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .minimumNumberOfCalls(10)
                .slidingWindowSize(10)
                .build();
        this.cb = CircuitBreaker.of("employeeApi", cbConfig);

        RetryConfig retryConfig = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofMillis(200))
                .retryOnException(throwable ->
                        // only retry on transient/network exceptions, not on WebClientResponseException for 4xx
                        throwable instanceof java.io.IOException
                                || throwable instanceof java.net.ConnectException
                                || throwable instanceof java.net.SocketTimeoutException)
                .build();
        this.retry = Retry.of("employeeApiRetry", retryConfig);
    }

    public List<Employee> getAllEmployees() {
        return timed("getAllEmployees", () -> {
            List<Employee> out = webClient
                    .get()
                    .retrieve()
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<List<Employee>>>() {
                    })
                    .map(ApiResponse::getData)
                    .transformDeferred(RetryOperator.of(retry)) // safe for GETs
                    .transformDeferred(CircuitBreakerOperator.of(cb))
                    .doOnSuccess(list -> log.info("Fetched {} employees", list == null ? 0 : list.size()))
                    .timeout(Duration.ofSeconds(5))
                    .onErrorResume(io.github.resilience4j.circuitbreaker.CallNotPermittedException.class, ex -> {
                        log.warn("Circuit breaker open for employeeApi - returning fallback empty list");
                        // We Could also increment a Micrometer counter here
                        return Mono.just(List.<Employee>of());
                    })
                    .onErrorResume(ex -> {
                        log.error("Failed to fetch employees: {}", ex.toString(), ex);
                        return Mono.just(List.of());
                    })
                    .block();
            return out == null ? List.of() : out;
        });
    }

    public Employee getById(String id) {
        return timed("getById", () -> webClient
                .get()
                .uri("/{id}", id)
                .retrieve()
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                })
                .map(ApiResponse::getData)
                .transformDeferred(RetryOperator.of(retry))
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .doOnSuccess(emp -> log.info("Fetched employee id={} found={}", id, emp != null))
                .timeout(Duration.ofSeconds(5))
                .doOnError(ex -> log.warn("Failed to fetch employee id={}: {}", id, ex.toString()))
                .onErrorResume(ex -> Mono.empty())
                .block());
    }

    public Employee create(CreateEmployeeRequest req) {
        return timed("createEmployee", () -> {
            // perform call and throw on non-2xx instead
            ApiResponse<Employee> response = webClient.post()
                    .bodyValue(Map.of(
                            "name", req.getName(),
                            "salary", req.getSalary(),
                            "age", req.getAge(),
                            "title", req.getTitle()))
                    .retrieve()
                    // surface 4xx/5xx as WebClientResponseException so we can handle/log
                    .onStatus(HttpStatusCode::isError, resp ->
                            resp.bodyToMono(String.class)
                                    .defaultIfEmpty(resp.statusCode().toString())
                                    .flatMap(body -> {
                                        String msg = "Employee API returned status " + resp.statusCode() + ": " + body;
                                        log.warn(msg);
                                        return Mono.error(new RuntimeException(msg));
                                    }))
                    .bodyToMono(new ParameterizedTypeReference<ApiResponse<Employee>>() {
                    })
                    .transformDeferred(RetryOperator.of(retry))
                    .transformDeferred(CircuitBreakerOperator.of(cb))     // protect POST (no retry)
                    .timeout(Duration.ofSeconds(5))
                    .doOnSuccess(empResp -> log.debug("Raw create response: {}", empResp))
                    .doOnError(ex -> log.error("Create employee failed name={}: {}", req.getName(), ex.toString()))
                    .block(); // may be null if remote returned empty body

            if (response == null) {
                String msg = "Employee API returned empty response for create";
                log.error(msg);
                throw new RuntimeException(msg);
            }

            Employee created = response.getData();
            if (created == null) {
                // If API returned wrapper but no data, treat as error
                String msg = "Employee API returned no employee object in response.data";
                log.error(msg + " — full response: {}", response);
                throw new RuntimeException(msg);
            }

            return created;
        });
    }

    /**
     * NOTE: The mock server expects DELETE /employee/{name} with BODY { "name": "..." } and returns { "data": true }.
     */
    public boolean deleteByName(String name) {
        Boolean ok = webClient.method(HttpMethod.DELETE)
                .uri("/{name}", name)
                .bodyValue(Map.of("name", name))
                .retrieve()
                .onStatus(s -> s.value() == 404, resp -> {
                    log.info("Delete name={} -> 404 (treat as not deleted)", name);
                    return Mono.empty();
                })
                .onStatus(HttpStatusCode::isError, resp -> resp.createException().flatMap(Mono::error))
                .bodyToMono(new ParameterizedTypeReference<ApiResponse<Boolean>>() {
                })
                .map(r -> Boolean.TRUE.equals(r.getData()))
                .transformDeferred(CircuitBreakerOperator.of(cb))
                .defaultIfEmpty(false)
                .timeout(Duration.ofSeconds(5))
                .doOnSuccess(result -> log.info("Delete name={} result={}", name, result))
                .doOnError(ex -> log.warn("Delete name={} failed: {}", name, ex.toString()))
                .onErrorReturn(false)
                .block();
        return Boolean.TRUE.equals(ok);
    }


    private <T> T timed(String operation, Supplier<T> supplier) {
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            T result = supplier.get();
            sample.stop(Timer.builder("employee.api.latency")
                    .tag("operation", operation)
                    .tag("status", "success")
                    .register(meterRegistry));
            return result;
        } catch (Exception ex) {
            sample.stop(Timer.builder("employee.api.latency")
                    .tag("operation", operation)
                    .tag("status", "failure")
                    .register(meterRegistry));
            throw ex;
        }
    }
}
