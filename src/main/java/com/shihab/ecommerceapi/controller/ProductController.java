package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Product getById(@PathVariable Integer id) {
        return productService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No product found with ID: " + id));
    }

    @PostMapping
    public Product create(@Valid @RequestBody Product product) {
        return productService.save(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Integer id, @Valid @RequestBody Product updatedProduct) {
        updatedProduct.setId(id);
        return productService.save(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        productService.deleteById(id);
    }
}