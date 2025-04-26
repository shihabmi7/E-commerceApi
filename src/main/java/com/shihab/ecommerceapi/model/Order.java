package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private Double total;

    @Enumerated(EnumType.STRING)
    private Status status = Status.pending;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        pending, shipped, delivered, cancelled
    }

    // Getters and setters...
}