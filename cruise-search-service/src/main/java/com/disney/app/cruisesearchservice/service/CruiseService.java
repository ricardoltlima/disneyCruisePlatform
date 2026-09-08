package com.disney.app.cruisesearchservice.service;

import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.error.CruiseAlreadyExistsException;
import com.disney.app.cruisesearchservice.error.CruiseNotFoundException;
import com.disney.app.cruisesearchservice.error.CruisePersistenceException;
import com.disney.app.cruisesearchservice.mapper.CruiseMapper;
import com.disney.app.cruisesearchservice.repository.CruiseRepository;
import com.mongodb.DuplicateKeyException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;

@Slf4j
@Service
public class CruiseService {

    private final CruiseRepository repository;
    private final CruiseMapper mapper;

    public CruiseService(
            CruiseRepository repository,
            CruiseMapper mapper
    ) {
        this.repository = repository;
        this.mapper = mapper;
    }

    public Mono<CruiseResponse> addCruise(CruiseRequest request) {
        return saveCruise(request)
                .doOnError(error -> log.warn("Could not create cruise for ship '{}'. error={}", request.shipName(), error.getClass().getSimpleName()))
                .onErrorMap(DuplicateKeyException.class, ex -> new CruiseAlreadyExistsException("Cruise already exists"))
                .onErrorMap(DataAccessException.class, ex -> new CruisePersistenceException("Unable to save cruise"));
    }

    private Mono<CruiseResponse> saveCruise(CruiseRequest request) {
        log.info("Creating cruise for ship '{}' from '{}' to '{}' departing on {}.", request.shipName(), request.departurePort(), request.destination(), request.departureDate());

        return Mono.just(request)
                .map(mapper::toCruiseEntity)
                .flatMap(repository::save)
                .map(mapper::toCruiseResponse)
                .doOnNext(response -> log.info("Cruise '{}' was created successfully. cruiseId={} departureDate={}", response.shipName(), response.id(), response.departureDate()));
    }

    public Mono<CruiseResponse> getCruise(String id) {
        log.info("Looking up cruise with id '{}'.", id);
        return repository.findById(id)
                .switchIfEmpty(Mono.error(new CruiseNotFoundException(id)))
                .map(mapper::toCruiseResponse)
                .doOnNext(response -> log.info("Found cruise '{}'. cruiseId={}", response.shipName(), response.id()))
                .doOnError(error -> log.warn("Could not find cruise with id '{}'. error={}", id, error.getClass().getSimpleName()));
    }

    public Flux<CruiseResponse> getCruises(Pageable pageable) {
        log.info("Searching cruises. page={} size={} sort={}", pageable.getPageNumber(), pageable.getPageSize(), pageable.getSort());

        return repository.findAllBy(pageable)
                .map(mapper::toCruiseResponse)
                .doOnComplete(() -> log.info("Cruise search completed. page={} size={}", pageable.getPageNumber(), pageable.getPageSize()))
                .doOnError(error -> log.warn("Cruise search failed. page={} size={} error={}", pageable.getPageNumber(), pageable.getPageSize(), error.getClass().getSimpleName()));
    }
}
