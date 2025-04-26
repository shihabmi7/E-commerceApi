package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.service.CartService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/carts")
public class CartController {
    
    @Autowired
    private CartService cartService;

    @GetMapping
    public List<Cart> getAll() {
        return cartService.findAll();
    }

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
}