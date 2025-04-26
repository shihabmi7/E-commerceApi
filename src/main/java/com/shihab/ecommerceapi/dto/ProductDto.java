package com.shihab.ecommerceapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ProductDto {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;

    // Getters and Setters
}