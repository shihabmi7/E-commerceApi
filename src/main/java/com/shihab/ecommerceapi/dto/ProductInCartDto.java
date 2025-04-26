// src/main/java/com/shihab/ecommerceapi/dto/ProductInCartDto.java
package com.shihab.ecommerceapi.dto;

import com.shihab.ecommerceapi.model.Category;
import lombok.*;

// A flat view: product fields + the quantity in the cart for one user
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ProductInCartDto {
    private Integer id;
    private String name;
    private String description;
    private Double price;
    private Integer stock;
    private Category category;
    private Integer quantity;
}
