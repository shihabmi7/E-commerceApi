package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.AddToCartRequest;
import com.shihab.ecommerceapi.dto.ProductInCartDto;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    
    @Autowired
    private CartService cartService;

    @GetMapping("/{id}")
    public Optional<Cart> getById(@PathVariable Integer id) {
        return cartService.findById(id);
    }

    @PostMapping
    public Cart create(@RequestBody Cart cart) {
        return cartService.save(cart);
    }

    @PutMapping("/{id}")
    public Cart update(@PathVariable Integer id, @RequestBody Cart updatedCart) {
        updatedCart.setId(id);
        return cartService.save(updatedCart);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        cartService.deleteById(id);
    }

    /**
     * Returns a list of products in the user's cart, each with its aggregated quantity.
     */
    @GetMapping
    public List<ProductInCartDto> getCart(@RequestParam Integer userId) {
        // 1) fetch all Cart entries for the user
        List<Cart> carts = cartService.findByUserId(userId);

        // 2) group by product and sum quantity
        Map<Product, Integer> aggregated = carts.stream()
                .collect(Collectors.toMap(
                        Cart::getProduct,
                        Cart::getQuantity,
                        Integer::sum,
                        LinkedHashMap::new
                ));

        // 3) map to DTOs
        return aggregated.entrySet().stream()
                .map(entry -> {
                    Product p = entry.getKey();
                    return new ProductInCartDto(
                            p.getId(),
                            p.getName(),
                            p.getDescription(),
                            p.getPrice(),
                            p.getStock(),
                            p.getCategory(),
                            entry.getValue()           // total quantity
                    );
                })
                .collect(Collectors.toList());
    }

    @PostMapping("/add")
    public Cart addToCart(@RequestBody AddToCartRequest req) {
        return cartService.addToCart(
                req.getUserId(),
                req.getProductId(),
                req.getQuantity()
        );
    }



}