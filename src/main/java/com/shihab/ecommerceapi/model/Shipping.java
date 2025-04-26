package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "shipping")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Shipping {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "order_id")
    private Order order;

    @ManyToOne
    @JoinColumn(name = "address_id")
    private Address address;

    private String trackingNumber;
    private LocalDateTime shippedDate;
    private LocalDateTime deliveryDate;

    // Getters and setters...
}