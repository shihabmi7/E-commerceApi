package com.shihab.ecommerceapi.model;

import lombok.*;

import jakarta.persistence.*;
import java.time.LocalDate;


@Entity
@Table(name = "discounts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Discount {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private Double discountPercentage;
    private LocalDate startDate;
    private LocalDate endDate;

    // Getters and setters...
}