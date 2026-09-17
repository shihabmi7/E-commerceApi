package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Wishlist;
import com.shihab.ecommerceapi.service.WishlistService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/wishlists")
public class WishlistController {

    private final WishlistService wishlistService;

    public WishlistController(WishlistService wishlistService) {
        this.wishlistService = wishlistService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Wishlist>>> getAll() {
        List<Wishlist> wishlists = wishlistService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Wishlists fetched successfully", wishlists));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Wishlist>> getById(@PathVariable Integer id) {
        Wishlist wishlist = wishlistService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No wishlist found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Wishlist fetched successfully", wishlist));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Wishlist>> create(@Valid @RequestBody Wishlist wishlist) {
        Wishlist saved = wishlistService.save(wishlist);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Wishlist created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Wishlist>> update(@PathVariable Integer id, @Valid @RequestBody Wishlist updatedWishlist) {
        updatedWishlist.setId(id);
        Wishlist saved = wishlistService.save(updatedWishlist);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Wishlist updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        wishlistService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
