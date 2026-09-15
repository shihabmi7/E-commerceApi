package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
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

    @NotNull
    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // NEW: link to Address
    @NotNull
    @ManyToOne
    @JoinColumn(name = "address_id", nullable = false)
    private Address address;

    @NotNull
    @PositiveOrZero
    private Double total;

    @Enumerated(EnumType.STRING)
    private Status status = Status.pending;

    private LocalDateTime createdAt = LocalDateTime.now();

    public enum Status {
        pending, shipped, delivered, cancelled
    }

    // Getters and setters...
}