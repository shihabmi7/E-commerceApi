package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Shipping;
import com.shihab.ecommerceapi.service.ShippingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/shippings")
public class ShippingController {

    private final ShippingService shippingService;

    public ShippingController(ShippingService shippingService) {
        this.shippingService = shippingService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Shipping>>> getAll() {
        List<Shipping> shippings = shippingService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Shippings fetched successfully", shippings));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Shipping>> getById(@PathVariable Integer id) {
        Shipping shipping = shippingService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No shipping found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Shipping fetched successfully", shipping));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Shipping>> create(@Valid @RequestBody Shipping shipping) {
        Shipping saved = shippingService.save(shipping);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Shipping created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Shipping>> update(@PathVariable Integer id, @Valid @RequestBody Shipping updatedShipping) {
        updatedShipping.setId(id);
        Shipping saved = shippingService.save(updatedShipping);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Shipping updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        shippingService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
