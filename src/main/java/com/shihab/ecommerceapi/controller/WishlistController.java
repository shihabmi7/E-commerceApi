package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Wishlist;
import com.shihab.ecommerceapi.service.WishlistService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/wishlists")
public class WishlistController {
    
    @Autowired
    private WishlistService wishlistService;

    @GetMapping
    public List<Wishlist> getAll() {
        return wishlistService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Wishlist> getById(@PathVariable Integer id) {
        return wishlistService.findById(id);
    }

    @PostMapping
    public Wishlist create(@RequestBody Wishlist wishlist) {
        return wishlistService.save(wishlist);
    }

    @PutMapping("/{id}")
    public Wishlist update(@PathVariable Integer id, @RequestBody Wishlist updatedWishlist) {
        updatedWishlist.setId(id);
        return wishlistService.save(updatedWishlist);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        wishlistService.deleteById(id);
    }
}