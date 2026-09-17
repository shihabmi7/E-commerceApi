package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Payment;
import com.shihab.ecommerceapi.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Payment>>> getAll() {
        List<Payment> payments = paymentService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Payments fetched successfully", payments));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Payment>> getById(@PathVariable Integer id) {
        Payment payment = paymentService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No payment found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Payment fetched successfully", payment));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Payment>> create(@Valid @RequestBody Payment payment) {
        Payment saved = paymentService.save(payment);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Payment created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Payment>> update(@PathVariable Integer id, @Valid @RequestBody Payment updatedPayment) {
        updatedPayment.setId(id);
        Payment saved = paymentService.save(updatedPayment);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Payment updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        paymentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
