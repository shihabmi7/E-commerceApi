package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Payment;
import com.shihab.ecommerceapi.service.PaymentService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @GetMapping
    public List<Payment> getAll() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Payment> getById(@PathVariable Integer id) {
        return paymentService.findById(id);
    }

    @PostMapping
    public Payment create(@Valid @RequestBody Payment payment) {
        return paymentService.save(payment);
    }

    @PutMapping("/{id}")
    public Payment update(@PathVariable Integer id, @Valid @RequestBody Payment updatedPayment) {
        updatedPayment.setId(id);
        return paymentService.save(updatedPayment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        paymentService.deleteById(id);
    }
}