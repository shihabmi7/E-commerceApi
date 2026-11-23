package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Payment;
import com.shihab.ecommerceapi.repository.PaymentRepository;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final MeterRegistry meterRegistry;

    public PaymentService(PaymentRepository paymentRepository, MeterRegistry meterRegistry) {
        this.paymentRepository = paymentRepository;
        this.meterRegistry = meterRegistry;
    }

    public List<Payment> findAll() {
        return paymentRepository.findAll();
    }

    public Optional<Payment> findById(Integer id) {
        return paymentRepository.findById(id);
    }

    public Payment save(Payment payment) {
        Payment saved = paymentRepository.save(payment);

        // business metric: payments_total{status="completed|failed|pending", method="..."}
        meterRegistry.counter("payments_total",
                "status", String.valueOf(saved.getPaymentStatus()),
                "method", String.valueOf(saved.getPaymentMethod()))
                .increment();
        return saved;
    }

    public void deleteById(Integer id) {
        paymentRepository.deleteById(id);
    }
}