package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.ProductService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/products")
public class ProductController {
    
    @Autowired
    private ProductService productService;

    @GetMapping
    public List<Product> getAll() {
        return productService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Product> getById(@PathVariable Integer id) {
        return productService.findById(id);
    }

    @PostMapping
    public Product create(@RequestBody Product product) {
        return productService.save(product);
    }

    @PutMapping("/{id}")
    public Product update(@PathVariable Integer id, @RequestBody Product updatedProduct) {
        updatedProduct.setId(id);
        return productService.save(updatedProduct);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        productService.deleteById(id);
    }
}