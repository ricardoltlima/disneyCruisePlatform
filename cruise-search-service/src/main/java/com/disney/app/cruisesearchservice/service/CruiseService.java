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
            return Mono.error(new InvalidIdempotencyKeyException("Idempotency-Key header must not be blank."));
        }

        Cache cache = cacheManager.getCache(CacheConfig.IDEMPOTENCY_CACHE);
        IdempotencyEntry cachedEntry = cache.get(idempotencyKey, IdempotencyEntry.class);

        if (cachedEntry != null) {
            if (!cachedEntry.request().equals(request)) {
                return Mono.error(new IdempotencyKeyConflictException(
                        "This Idempotency-Key was already used with a different cruise request."
                ));
            }

            return Mono.just(cachedEntry.response());
        }

        return Mono.just(request)
                .map(mapper::toCruiseEntity)
                .flatMap(entity -> repository.save(entity)
                        .transformDeferred(RetryOperator.of(mongoRetry)))
                .map(mapper::toCruiseResponse)
                .doOnNext(response -> cache.put(idempotencyKey, new IdempotencyEntry(request, response)))
                .onErrorMap(DuplicateKeyException.class,
                        ex -> new CruiseAlreadyExistsException("Cruise already exists"))
                .onErrorMap(DataAccessException.class,
                        ex -> new CruisePersistenceException("Unable to save cruise"));
    }

    public Mono<CruiseResponse> getCruise(String id) {
        return repository.findById(id)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .switchIfEmpty(Mono.error(new CruiseNotFoundException(id)))
                .map(mapper::toCruiseResponse);
    }

    public Flux<CruiseResponse> getCruises(Pageable pageable) {
        return repository.findAllBy(pageable)
                .transformDeferred(RetryOperator.of(mongoRetry))
                .map(mapper::toCruiseResponse);
    }

    private record IdempotencyEntry(CruiseRequest request, CruiseResponse response) {
    }
}
