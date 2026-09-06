package com.disney.app.cruisesearchservice.repository;

import com.disney.app.cruisesearchservice.entity.CruiseEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface CruiseRepository extends ReactiveCrudRepository<CruiseEntity, String> {

    Flux<CruiseEntity> findAllBy(Pageable pageable);
}
