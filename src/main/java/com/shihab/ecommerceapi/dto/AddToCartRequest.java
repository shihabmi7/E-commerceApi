// src/main/java/com/shihab/ecommerceapi/dto/AddToCartRequest.java
package com.shihab.ecommerceapi.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class AddToCartRequest {
    private Integer userId;
    private Integer productId;
    private Integer quantity;
}
