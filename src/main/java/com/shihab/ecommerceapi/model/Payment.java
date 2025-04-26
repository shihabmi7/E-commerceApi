package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "payments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Payment {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    private String paymentMethod;

    @Enumerated(EnumType.STRING)
    private PaymentStatus paymentStatus = PaymentStatus.pending;

    private LocalDateTime paymentDate = LocalDateTime.now();

    public enum PaymentStatus {
        pending, completed, failed
    }

    // Getters and setters...
}