package com.disney.app.reservationservice.client;

import com.disney.app.reservationservice.config.RequestLoggingFilter;
import com.disney.app.reservationservice.error.CruiseSearchServiceException;
import com.disney.app.reservationservice.error.TransientCruiseSearchServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
public class CruiseSearchClient {

    private final WebClient webClient;
    private final String bearerToken;
    private final Retry cruiseSearchRetry;
    private final CircuitBreaker cruiseSearchCircuitBreaker;
    private final Duration timeout;

    public CruiseSearchClient(
            WebClient.Builder webClientBuilder,
            @Value("${app.clients.cruise-search.base-url}") String baseUrl,
            @Value("${app.clients.cruise-search.bearer-token}") String bearerToken,
            @Value("${app.clients.cruise-search.timeout}") Duration timeout,
            @Qualifier("cruiseSearchRetry") Retry cruiseSearchRetry,
            CircuitBreaker cruiseSearchCircuitBreaker
    ) {
        this.webClient = webClientBuilder.baseUrl(baseUrl).build();
        this.bearerToken = bearerToken;
        this.cruiseSearchRetry = cruiseSearchRetry;
        this.cruiseSearchCircuitBreaker = cruiseSearchCircuitBreaker;
        this.timeout = timeout;
    }

    public Mono<CruiseSearchResponse> getCruise(String cruiseId) {
        return Mono.deferContextual(context -> {
            String correlationId = context.getOrDefault(RequestLoggingFilter.CORRELATION_ID_CONTEXT_KEY, null);

            log.info("Looking up cruise {} in Cruise Search Service", cruiseId);

            return webClient.get()
                    .uri("/api/v1/cruises/{id}", cruiseId)
                    .headers(headers -> {
                        addAuthorizationHeader(headers, bearerToken);
                        addCorrelationHeader(headers, correlationId);
                    })
                    .exchangeToMono(response -> {
                        if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                            log.info("Cruise Search Service did not find cruise {}", cruiseId);
                            return Mono.empty();
                        }

                        if (isRetryableStatus(response.statusCode())) {
                            return response.createException()
                                    .flatMap(ex -> Mono.error(new TransientCruiseSearchServiceException(
                                            "Cruise Search Service is temporarily unavailable. Status: " + response.statusCode())));
                        }

                        if (response.statusCode().isError()) {
                            return response.createException()
                                    .flatMap(ex -> Mono.error(new CruiseSearchServiceException(
                                            "Cruise Search Service could not process the request. Status: " + response.statusCode())));
                        }

                        return response.bodyToMono(CruiseSearchResponse.class);
                    })
                    .doOnNext(response -> log.info("Cruise {} found with status {} and {} available cabins", response.id(), response.status(), response.availableCabins()))
                    .timeout(timeout)
                    .transformDeferred(CircuitBreakerOperator.of(cruiseSearchCircuitBreaker))
                    .transformDeferred(RetryOperator.of(cruiseSearchRetry))
                    .doOnError(error -> log.warn("Could not look up cruise {} in Cruise Search Service: {}", cruiseId, error.getClass().getSimpleName()));
        });
    }

    private boolean isRetryableStatus(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError()
                || statusCode.equals(HttpStatus.REQUEST_TIMEOUT)
                || statusCode.equals(HttpStatus.TOO_MANY_REQUESTS);
    }

    private void addAuthorizationHeader(HttpHeaders headers, String token) {
        if (StringUtils.isNotBlank(token)) {
            headers.setBearerAuth(token);
        }
    }

    private void addCorrelationHeader(HttpHeaders headers, String correlationId) {
        if (StringUtils.isNotBlank(correlationId)) {
            headers.set(RequestLoggingFilter.CORRELATION_ID_HEADER, correlationId);
        }
    }
}
