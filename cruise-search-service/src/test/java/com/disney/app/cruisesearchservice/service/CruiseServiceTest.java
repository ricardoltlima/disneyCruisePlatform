package com.disney.app.cruisesearchservice.service;

import com.disney.app.cruisesearchservice.config.CacheConfig;
import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.entity.CruiseEntity;
import com.disney.app.cruisesearchservice.error.CruiseNotFoundException;
import com.disney.app.cruisesearchservice.error.CruisePersistenceException;
import com.disney.app.cruisesearchservice.error.IdempotencyKeyConflictException;
import com.disney.app.cruisesearchservice.error.InvalidIdempotencyKeyException;
import com.disney.app.cruisesearchservice.mapper.CruiseMapper;
import com.disney.app.cruisesearchservice.repository.CruiseRepository;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.cache.CacheManager;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CruiseServiceTest {

    private static final LocalDateTime DEPARTURE_DATE = LocalDateTime.of(2026, 10, 1, 9, 0);
    private static final LocalDateTime RETURN_DATE = LocalDateTime.of(2026, 10, 5, 9, 0);

    private CruiseRepository repository;
    private CruiseService service;

    @BeforeEach
    void setUp() {
        repository = mock(CruiseRepository.class);
        CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConfig.IDEMPOTENCY_CACHE);
        service = new CruiseService(repository, new TestCruiseMapper(), cacheManager, retry(1));
    }

    @Test
    void addCruiseSavesCruiseAndReturnsResponse() {
        CruiseRequest request = cruiseRequest("Disney Wish");
        CruiseEntity savedEntity = cruiseEntity("cruise-1", request);

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.addCruise(request, "create-cruise-1"))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        verify(repository).save(new CruiseEntity(
                null,
                request.shipName(),
                request.departurePort(),
                request.destination(),
                request.departureDate(),
                request.returnDate()
        ));
    }

    @Test
    void addCruiseReturnsCachedResponseForSameIdempotencyKeyAndSameRequest() {
        CruiseRequest request = cruiseRequest("Disney Wish");
        CruiseEntity savedEntity = cruiseEntity("cruise-1", request);

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.just(savedEntity));

        StepVerifier.create(service.addCruise(request, "create-cruise-1"))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        StepVerifier.create(service.addCruise(request, "create-cruise-1"))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();

        verify(repository).save(any(CruiseEntity.class));
    }

    @Test
    void addCruiseRejectsSameIdempotencyKeyWithDifferentRequest() {
        CruiseRequest originalRequest = cruiseRequest("Disney Wish");
        CruiseRequest differentRequest = cruiseRequest("Disney Treasure");

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.just(cruiseEntity("cruise-1", originalRequest)));

        StepVerifier.create(service.addCruise(originalRequest, "create-cruise-1"))
                .expectNextCount(1)
                .verifyComplete();

        StepVerifier.create(service.addCruise(differentRequest, "create-cruise-1"))
                .expectError(IdempotencyKeyConflictException.class)
                .verify();
    }

    @Test
    void addCruiseRejectsBlankIdempotencyKey() {
        StepVerifier.create(service.addCruise(cruiseRequest("Disney Wish"), " "))
                .expectError(InvalidIdempotencyKeyException.class)
                .verify();

        verify(repository, never()).save(any(CruiseEntity.class));
    }

    @Test
    void addCruiseMapsDatabaseFailureToPersistenceException() {
        when(repository.save(any(CruiseEntity.class)))
                .thenReturn(Mono.error(new DataAccessResourceFailureException("Mongo unavailable")));

        StepVerifier.create(service.addCruise(cruiseRequest("Disney Wish"), "create-cruise-1"))
                .expectError(CruisePersistenceException.class)
                .verify();
    }

    @Test
    void getCruiseReturnsCruiseWhenFound() {
        CruiseEntity entity = cruiseEntity("cruise-1", cruiseRequest("Disney Wish"));

        when(repository.findById("cruise-1")).thenReturn(Mono.just(entity));

        StepVerifier.create(service.getCruise("cruise-1"))
                .expectNext(cruiseResponse(entity))
                .verifyComplete();
    }

    @Test
    void getCruiseReturnsNotFoundWhenRepositoryIsEmpty() {
        when(repository.findById("missing-cruise")).thenReturn(Mono.empty());

        StepVerifier.create(service.getCruise("missing-cruise"))
                .expectError(CruiseNotFoundException.class)
                .verify();
    }

    @Test
    void getCruisesReturnsPagedCruises() {
        Pageable pageable = PageRequest.of(0, 2);
        CruiseEntity first = cruiseEntity("cruise-1", cruiseRequest("Disney Wish"));
        CruiseEntity second = cruiseEntity("cruise-2", cruiseRequest("Disney Treasure"));

        when(repository.findAllBy(pageable)).thenReturn(Flux.just(first, second));

        StepVerifier.create(service.getCruises(pageable))
                .expectNext(cruiseResponse(first))
                .expectNext(cruiseResponse(second))
                .verifyComplete();
    }

    @Test
    void addCruiseRetriesTransientDatabaseFailure() {
        Retry retry = retry(3);
        CacheManager cacheManager = new ConcurrentMapCacheManager(CacheConfig.IDEMPOTENCY_CACHE);
        service = new CruiseService(repository, new TestCruiseMapper(), cacheManager, retry);

        CruiseRequest request = cruiseRequest("Disney Wish");
        CruiseEntity savedEntity = cruiseEntity("cruise-1", request);
        AtomicInteger attempts = new AtomicInteger();

        when(repository.save(any(CruiseEntity.class))).thenReturn(Mono.defer(() -> {
            if (attempts.incrementAndGet() < 3) {
                return Mono.error(new DataAccessResourceFailureException("Temporary Mongo failure"));
            }

            return Mono.just(savedEntity);
        }));

        StepVerifier.create(service.addCruise(request, "create-cruise-1"))
                .expectNext(cruiseResponse(savedEntity))
                .verifyComplete();
    }

    private static Retry retry(int maxAttempts) {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ZERO)
                .retryExceptions(DataAccessResourceFailureException.class)
                .build();

        return Retry.of("testMongoRetry", config);
    }

    private static CruiseRequest cruiseRequest(String shipName) {
        return new CruiseRequest(
                shipName,
                "Port Canaveral",
                "Bahamas",
                DEPARTURE_DATE,
                RETURN_DATE
        );
    }

    private static CruiseEntity cruiseEntity(String id, CruiseRequest request) {
        return new CruiseEntity(
                id,
                request.shipName(),
                request.departurePort(),
                request.destination(),
                request.departureDate(),
                request.returnDate()
        );
    }

    private static CruiseResponse cruiseResponse(CruiseEntity entity) {
        return new CruiseResponse(
                entity.id(),
                entity.shipName(),
                entity.departurePort(),
                entity.destination(),
                entity.departureDate(),
                entity.returnDate()
        );
    }

    private static class TestCruiseMapper implements CruiseMapper {

        @Override
        public CruiseResponse toCruiseResponse(CruiseEntity cruiseEntity) {
            return cruiseResponse(cruiseEntity);
        }

        @Override
        public CruiseEntity toCruiseEntity(CruiseRequest cruiseRequest) {
            return new CruiseEntity(
                    null,
                    cruiseRequest.shipName(),
                    cruiseRequest.departurePort(),
                    cruiseRequest.destination(),
                    cruiseRequest.departureDate(),
                    cruiseRequest.returnDate()
            );
        }
    }
}
