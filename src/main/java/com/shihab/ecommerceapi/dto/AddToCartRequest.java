// src/main/java/com/shihab/ecommerceapi/dto/AddToCartRequest.java
package com.shihab.ecommerceapi.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    @NotNull
    private Integer userId;

    @NotNull
    private Integer productId;

    @NotNull
    @Positive
    private Integer quantity;
}
