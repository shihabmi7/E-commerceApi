package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.ProductImage;
import com.shihab.ecommerceapi.service.ProductImageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/productimages")
public class ProductImageController {
    
    @Autowired
    private ProductImageService productimageService;

    @GetMapping
    public List<ProductImage> getAll() {
        return productimageService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ProductImage> getById(@PathVariable Integer id) {
        return productimageService.findById(id);
    }

    @PostMapping
    public ProductImage create(@RequestBody ProductImage productimage) {
        return productimageService.save(productimage);
    }

    @PutMapping("/{id}")
    public ProductImage update(@PathVariable Integer id, @RequestBody ProductImage updatedProductImage) {
        updatedProductImage.setId(id);
        return productimageService.save(updatedProductImage);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        productimageService.deleteById(id);
    }
}