package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.ProductImage;
import com.shihab.ecommerceapi.service.ProductImageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/productimages")
public class ProductImageController {

    private final ProductImageService productimageService;

    public ProductImageController(ProductImageService productimageService) {
        this.productimageService = productimageService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<ProductImage>>> getAll() {
        List<ProductImage> productImages = productimageService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Product images fetched successfully", productImages));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<ProductImage>> getById(@PathVariable Integer id) {
        ProductImage productImage = productimageService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No product image found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Product image fetched successfully", productImage));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<ProductImage>> create(@Valid @RequestBody ProductImage productimage) {
        ProductImage saved = productimageService.save(productimage);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Product image created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<ProductImage>> update(@PathVariable Integer id, @Valid @RequestBody ProductImage updatedProductImage) {
        updatedProductImage.setId(id);
        ProductImage saved = productimageService.save(updatedProductImage);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Product image updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productimageService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
