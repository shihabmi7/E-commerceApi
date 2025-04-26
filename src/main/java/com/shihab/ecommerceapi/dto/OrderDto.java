package com.shihab.ecommerceapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class OrderDto {
    private Integer id;
    private Integer userId;
    private Double total;
    private String status;

    // Getters and Setters
}