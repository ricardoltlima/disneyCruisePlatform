package com.disney.app.reservationservice.client;

import com.disney.app.reservationservice.config.RequestLoggingFilter;
import com.disney.app.reservationservice.error.CruiseSearchServiceException;
import com.disney.app.reservationservice.error.TransientCruiseSearchServiceException;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.reactor.circuitbreaker.operator.CircuitBreakerOperator;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientRequestException;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

@Component
public class CruiseSearchClient {

    private static final Logger log = LoggerFactory.getLogger(CruiseSearchClient.class);

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

            log.info("cruise_search_client_lookup_requested cruiseId={}", cruiseId);

            return webClient.get()
                    .uri("/api/v1/cruises/{id}", cruiseId)
                    .headers(headers -> {
                        addAuthorizationHeader(headers, bearerToken);
                        addCorrelationHeader(headers, correlationId);
                    })
                    .exchangeToMono(response -> {
                        if (response.statusCode().equals(HttpStatus.NOT_FOUND)) {
                            log.info("cruise_search_client_lookup_not_found cruiseId={}", cruiseId);
                            return Mono.empty();
                        }

                    if (isRetryableStatus(response.statusCode())) {
                        return response.createException()
                                .flatMap(ex -> Mono.error(new TransientCruiseSearchServiceException(
                                        "Cruise Search Service request failed with status: " + response.statusCode()
                                )));
                    }

                    if (response.statusCode().isError()) {
                        return response.createException()
                                .flatMap(ex -> Mono.error(new CruiseSearchServiceException(
                                        "Cruise Search Service rejected Reservation Service request with status: " + response.statusCode()
                                )));
                    }

                        return response.bodyToMono(CruiseSearchResponse.class);
                    })
                    .doOnNext(response -> log.info("cruise_search_client_lookup_succeeded cruiseId={} status={} availableCabins={}",
                            response.id(),
                            response.status(),
                            response.availableCabins()))
                    .onErrorMap(WebClientRequestException.class,
                            ex -> new TransientCruiseSearchServiceException("Cruise Search Service is unavailable"))
                    .timeout(timeout)
                    .onErrorMap(TimeoutException.class,
                            ex -> new TransientCruiseSearchServiceException("Cruise Search Service timed out"))
                    .onErrorMap(WebClientResponseException.class,
                            ex -> new CruiseSearchServiceException("Cruise Search Service rejected Reservation Service request"))
                    .transformDeferred(CircuitBreakerOperator.of(cruiseSearchCircuitBreaker))
                    .transformDeferred(RetryOperator.of(cruiseSearchRetry))
                    .doOnError(error -> log.warn("cruise_search_client_lookup_failed cruiseId={} error={}",
                            cruiseId,
                            error.getClass().getSimpleName()));
        });
    }

    private boolean isRetryableStatus(HttpStatusCode statusCode) {
        return statusCode.is5xxServerError()
                || statusCode.equals(HttpStatus.REQUEST_TIMEOUT)
                || statusCode.equals(HttpStatus.TOO_MANY_REQUESTS);
    }

    private void addAuthorizationHeader(HttpHeaders headers, String token) {
        if (StringUtils.hasText(token)) {
            headers.setBearerAuth(token);
        }
    }

    private void addCorrelationHeader(HttpHeaders headers, String correlationId) {
        if (StringUtils.hasText(correlationId)) {
            headers.set(RequestLoggingFilter.CORRELATION_ID_HEADER, correlationId);
        }
    }
}
