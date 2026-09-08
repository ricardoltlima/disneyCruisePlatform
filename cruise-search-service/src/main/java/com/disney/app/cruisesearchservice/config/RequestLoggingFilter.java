package com.disney.app.cruisesearchservice.config;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Component
public class RequestLoggingFilter implements WebFilter {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";
    public static final String CORRELATION_ID_CONTEXT_KEY = "correlationId";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String correlationId = correlationId(exchange.getRequest());
        long startNanos = System.nanoTime();

        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, correlationId);

        return chain.filter(exchange)
                .doOnSubscribe(subscription -> withCorrelationId(correlationId, () ->
                        log.info("Incoming request started. method={} path={}",
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getPath().value())))
                .doOnSuccess(ignored -> withCorrelationId(correlationId, () ->
                        log.info("Request completed successfully. method={} path={} status={} durationMs={}",
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getPath().value(),
                                exchange.getResponse().getStatusCode(),
                                elapsedMillis(startNanos))))
                .doOnError(error -> withCorrelationId(correlationId, () ->
                        log.warn("Request failed. method={} path={} durationMs={} error={}",
                                exchange.getRequest().getMethod(),
                                exchange.getRequest().getPath().value(),
                                elapsedMillis(startNanos),
                                error.getClass().getSimpleName())))
                .contextWrite(context -> context.put(CORRELATION_ID_CONTEXT_KEY, correlationId));
    }

    private String correlationId(ServerHttpRequest request) {
        String correlationId = request.getHeaders().getFirst(CORRELATION_ID_HEADER);
        return correlationId == null || correlationId.isBlank()
                ? UUID.randomUUID().toString()
                : correlationId;
    }

    private long elapsedMillis(long startNanos) {
        return (System.nanoTime() - startNanos) / 1_000_000;
    }

    private void withCorrelationId(String correlationId, Runnable logStatement) {
        try (MDC.MDCCloseable ignored = MDC.putCloseable(CORRELATION_ID_CONTEXT_KEY, correlationId)) {
            logStatement.run();
        }
    }
}
