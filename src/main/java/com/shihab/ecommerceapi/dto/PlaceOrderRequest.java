// src/main/java/com/shihab/ecommerceapi/dto/PlaceOrderRequest.java
package com.shihab.ecommerceapi.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
public class PlaceOrderRequest {
    @NotNull
    private Integer userId;
}
