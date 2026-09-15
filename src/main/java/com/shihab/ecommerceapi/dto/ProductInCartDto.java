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
    private Double price;      // current live price of the product
    private Integer stock;
    private Category category;
    private Integer quantity;
    private Double lockedPrice;    // price snapshotted when the item was added to the cart — what will actually be charged
    private Boolean priceChanged;  // true if the product's live price has moved since it was added
    private Double priceDelta;     // price - lockedPrice (positive = price went up, negative = price went down)
}
