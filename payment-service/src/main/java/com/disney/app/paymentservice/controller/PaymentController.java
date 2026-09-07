package com.disney.app.paymentservice.controller;

import com.disney.app.paymentservice.dto.PaymentRequest;
import com.disney.app.paymentservice.dto.PaymentResponse;
import com.disney.app.paymentservice.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    public Mono<ResponseEntity<PaymentResponse>> createPayment(@Valid @RequestBody PaymentRequest request) {
        return paymentService.createPayment(request)
                .map(response -> ResponseEntity
                        .created(URI.create("/api/v1/payments/" + response.id()))
                        .body(response));
    }

    @GetMapping("/{id}")
    public Mono<ResponseEntity<PaymentResponse>> getPayment(@PathVariable String id) {
        return paymentService.getPayment(id)
                .map(ResponseEntity::ok);
    }
}
