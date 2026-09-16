package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Discount;
import com.shihab.ecommerceapi.service.DiscountService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {

    private final DiscountService discountService;

    public DiscountController(DiscountService discountService) {
        this.discountService = discountService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Discount>>> getAll() {
        List<Discount> discounts = discountService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Discounts fetched successfully", discounts));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Discount>> getById(@PathVariable Integer id) {
        Discount discount = discountService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No discount found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Discount fetched successfully", discount));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Discount>> create(@Valid @RequestBody Discount discount) {
        Discount saved = discountService.save(discount);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Discount created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Discount>> update(@PathVariable Integer id, @Valid @RequestBody Discount updatedDiscount) {
        updatedDiscount.setId(id);
        Discount saved = discountService.save(updatedDiscount);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Discount updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        discountService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
