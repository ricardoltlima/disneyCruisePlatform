package com.disney.app.cruisesearchservice.service;

import com.disney.app.cruisesearchservice.config.CacheConfig;
import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.error.CruiseAlreadyExistsException;
import com.disney.app.cruisesearchservice.error.CruiseNotFoundException;
import com.disney.app.cruisesearchservice.error.CruisePersistenceException;
import com.disney.app.cruisesearchservice.error.IdempotencyKeyConflictException;
import com.disney.app.cruisesearchservice.error.InvalidIdempotencyKeyException;
import com.disney.app.cruisesearchservice.mapper.CruiseMapper;
import com.disney.app.cruisesearchservice.repository.CruiseRepository;
import com.mongodb.DuplicateKeyException;
import io.github.resilience4j.reactor.retry.RetryOperator;
import io.github.resilience4j.retry.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
public class CruiseService {

    private static final Logger log = LoggerFactory.getLogger(CruiseService.class);

    private final CruiseRepository repository;
    private final CruiseMapper mapper;
    private final CacheManager cacheManager;
    private final Retry mongoRetry;

    public CruiseService(CruiseRepository repository, CruiseMapper mapper, CacheManager cacheManager, Retry mongoRetry) {
        this.repository = repository;
        this.mapper = mapper;
        this.cacheManager = cacheManager;
        this.mongoRetry = mongoRetry;
    }

    public Mono<CruiseResponse> addCruise(CruiseRequest request, String idempotencyKey) {
        if (!StringUtils.hasText(idempotencyKey)) {
            log.warn("cruise_create_rejected reason=missing_idempotency_key");
            return Mono.error(new InvalidIdempotencyKeyException("Idempotency-Key header must not be blank."));
        }

        log.info("cruise_create_requested shipName={} departurePort={} destination={} departureDate={} idempotencyKey={}",
                request.shipName(),
                request.departurePort(),
                request.destination(),
                request.departureDate(),
                idempotencyKey);

        Cache cache = cacheManager.getCache(CacheConfig.IDEMPOTENCY_CACHE);
        IdempotencyEntry cachedEntry = cache.get(idempotencyKey, IdempotencyEntry.class);

        if (cachedEntry != null) {
            if (!cachedEntry.request().equals(request)) {
                log.warn("cruise_create_idempotency_conflict idempotencyKey={}", idempotencyKey);
                return Mono.error(new IdempotencyKeyConflictException(
                        "This Idempotency-Key was already used with a different cruise request."
                ));
            }

            log.info("cruise_create_idempotency_replay idempotencyKey={} cruiseId={}",
                    idempotencyKey,
                    cachedEntry.response().id());
            return Mono.just(cachedEntry.response());
        }

        return Mono.just(request)
                .map(mapper::toCruiseEntity)
                .flatMap(entity -> repository.save(entity)
                        .transformDeferred(RetryOperator.of(mongoRetry)))
                .map(mapper::toCruiseResponse)
                .doOnNext(response -> log.info("cruise_created cruiseId={} shipName={} departureDate={}",
                        response.id(),
                        response.shipName(),
                        response.departureDate()))
                .doOnNext(response -> cache.put(idempotencyKey, new IdempotencyEntry(request, response)))
                .doOnError(error -> log.warn("cruise_create_failed shipName={} error={}",
                        request.shipName(),
                        error.getClass().getSimpleName()))
                .onErrorMap(DuplicateKeyException.class,
                        ex -> new CruiseAlreadyExistsException("Cruise already exists"))
                .onErrorMap(DataAccessException.class,
                        ex -> new CruisePersistenceException("Unable to save cruise"));
    }

    public Mono<CruiseResponse> getCruise(String id) {
        log.info("cruise_lookup_requested cruiseId={}", id);
        return repository.findById(id)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .switchIfEmpty(Mono.error(new CruiseNotFoundException(id)))
                .map(mapper::toCruiseResponse)
                .doOnNext(response -> log.info("cruise_lookup_succeeded cruiseId={}", response.id()))
                .doOnError(error -> log.warn("cruise_lookup_failed cruiseId={} error={}",
                        id,
                        error.getClass().getSimpleName()));
    }

    public Flux<CruiseResponse> getCruises(Pageable pageable) {
        log.info("cruise_search_requested page={} size={} sort={}",
                pageable.getPageNumber(),
                pageable.getPageSize(),
                pageable.getSort());
        return repository.findAllBy(pageable)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .map(mapper::toCruiseResponse)
                .doOnNext(cruiseResponse -> log.info("Response: {}", cruiseResponse))
                .doOnComplete(() -> log.info("cruise_search_completed page={} size={}",
                        pageable.getPageNumber(),
                        pageable.getPageSize()))
                .doOnError(error -> log.warn("cruise_search_failed page={} size={} error={}",
                        pageable.getPageNumber(),
                        pageable.getPageSize(),
                        error.getClass().getSimpleName()));
    }

    private record IdempotencyEntry(CruiseRequest request, CruiseResponse response) {
    }
}
