package com.disney.app.cruisesearchservice.controller;

import com.disney.app.cruisesearchservice.dto.CruiseRequest;
import com.disney.app.cruisesearchservice.dto.CruiseResponse;
import com.disney.app.cruisesearchservice.service.CruiseService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/cruises")
public class CruiseController {

    private final CruiseService service;

    public CruiseController(CruiseService service) {
        this.service = service;
    }

    @PostMapping
    public Mono<ResponseEntity<CruiseResponse>> addCruise(
            @RequestHeader("Idempotency-Key") String idempotencyKey,
            @Valid @RequestBody CruiseRequest request
    ) {
        return service.addCruise(request, idempotencyKey)
                .map(cruiseCreated -> ResponseEntity.created(URI.create("/api/v1/cruises/" + cruiseCreated.id())).body(cruiseCreated));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<CruiseResponse>> getCruise(@PathVariable String id) {
        return service.getCruise(id)
                .map(ResponseEntity::ok);
    }

    @GetMapping
    public Flux<CruiseResponse> getCruises(Pageable pageable) {
        return service.getCruises(pageable);
    }
}
