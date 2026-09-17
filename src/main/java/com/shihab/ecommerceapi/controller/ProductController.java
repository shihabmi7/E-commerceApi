package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Product>>> getAll() {
        List<Product> products = productService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Products fetched successfully", products));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Product>> getById(@PathVariable Integer id) {
        Product product = productService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No product found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Product fetched successfully", product));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Product>> create(@Valid @RequestBody Product product) {
        Product saved = productService.save(product);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Product created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Product>> update(@PathVariable Integer id, @Valid @RequestBody Product updatedProduct) {
        updatedProduct.setId(id);
        Product saved = productService.save(updatedProduct);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Product updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        productService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
