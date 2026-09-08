package com.disney.app.cruisesearchservice.metrics;

import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Aspect
@Component
public class CruiseServiceMetricsAspect {

    private final MeterRegistry meterRegistry;

    public CruiseServiceMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* com.disney.app.cruisesearchservice.service.CruiseService.addCruise(..))")
    public Mono<CruiseResponse> recordAddCruiseMetrics(ProceedingJoinPoint joinPoint) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                return proceedMono(joinPoint)
                        .doOnNext(response -> {
                            meterRegistry.counter("cruise.created", "status", response.status().name()).increment();
                            sample.stop(meterRegistry.timer("cruise.create.duration", "outcome", "success"));
                        })
                        .doOnError(error -> recordCreateFailure(error, sample));
            } catch (Throwable error) {
                recordCreateFailure(error, sample);
                return Mono.error(error);
            }
        });
    }

    @Around("execution(* com.disney.app.cruisesearchservice.service.CruiseService.getCruise(..))")
    public Mono<CruiseResponse> recordGetCruiseMetrics(ProceedingJoinPoint joinPoint) {
        return Mono.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                return proceedMono(joinPoint)
                        .doOnNext(response -> {
                            meterRegistry.counter("cruise.lookup.completed", "outcome", "found").increment();
                            sample.stop(meterRegistry.timer("cruise.lookup.duration", "outcome", "found"));
                        })
                        .doOnError(error -> {
                            meterRegistry.counter("cruise.lookup.completed", "outcome", error.getClass().getSimpleName()).increment();
                            sample.stop(meterRegistry.timer("cruise.lookup.duration", "outcome", "error"));
                        });
            } catch (Throwable error) {
                meterRegistry.counter("cruise.lookup.completed", "outcome", error.getClass().getSimpleName()).increment();
                sample.stop(meterRegistry.timer("cruise.lookup.duration", "outcome", "error"));
                return Mono.error(error);
            }
        });
    }

    @Around("execution(* com.disney.app.cruisesearchservice.service.CruiseService.getCruises(..))")
    public Flux<CruiseResponse> recordGetCruisesMetrics(ProceedingJoinPoint joinPoint) {
        return Flux.defer(() -> {
            Timer.Sample sample = Timer.start(meterRegistry);

            try {
                return proceedFlux(joinPoint)
                        .doOnNext(cruiseResponse -> meterRegistry.counter("cruise.search.item.returned").increment())
                        .doOnComplete(() -> {
                            meterRegistry.counter("cruise.search.completed", "outcome", "success").increment();
                            sample.stop(meterRegistry.timer("cruise.search.duration", "outcome", "success"));
                        })
                        .doOnError(error -> {
                            meterRegistry.counter("cruise.search.completed", "outcome", error.getClass().getSimpleName()).increment();
                            sample.stop(meterRegistry.timer("cruise.search.duration", "outcome", "error"));
                        });
            } catch (Throwable error) {
                meterRegistry.counter("cruise.search.completed", "outcome", error.getClass().getSimpleName()).increment();
                sample.stop(meterRegistry.timer("cruise.search.duration", "outcome", "error"));
                return Flux.error(error);
            }
        });
    }

    private void recordCreateFailure(Throwable error, Timer.Sample sample) {
        meterRegistry.counter("cruise.create.failed", "error", error.getClass().getSimpleName()).increment();
        sample.stop(meterRegistry.timer("cruise.create.duration", "outcome", "error"));
    }

    @SuppressWarnings("unchecked")
    private Mono<CruiseResponse> proceedMono(ProceedingJoinPoint joinPoint) throws Throwable {
        return (Mono<CruiseResponse>) joinPoint.proceed();
    }

    @SuppressWarnings("unchecked")
    private Flux<CruiseResponse> proceedFlux(ProceedingJoinPoint joinPoint) throws Throwable {
        return (Flux<CruiseResponse>) joinPoint.proceed();
    }
}
