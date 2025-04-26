package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Payment;
import com.shihab.ecommerceapi.service.PaymentService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/payments")
public class PaymentController {
    
    @Autowired
    private PaymentService paymentService;

    @GetMapping
    public List<Payment> getAll() {
        return paymentService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Payment> getById(@PathVariable Integer id) {
        return paymentService.findById(id);
    }

    @PostMapping
    public Payment create(@RequestBody Payment payment) {
        return paymentService.save(payment);
    }

    @PutMapping("/{id}")
    public Payment update(@PathVariable Integer id, @RequestBody Payment updatedPayment) {
        updatedPayment.setId(id);
        return paymentService.save(updatedPayment);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        paymentService.deleteById(id);
    }
}