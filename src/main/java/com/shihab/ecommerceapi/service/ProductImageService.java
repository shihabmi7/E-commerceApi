package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.ProductImage;
import com.shihab.ecommerceapi.repository.ProductImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductImageService {
    
    @Autowired
    private ProductImageRepository productimageRepository;

    public List<ProductImage> findAll() {
        return productimageRepository.findAll();
    }

    public Optional<ProductImage> findById(Integer id) {
        return productimageRepository.findById(id);
    }

    public ProductImage save(ProductImage productimage) {
        return productimageRepository.save(productimage);
    }

    public void deleteById(Integer id) {
        productimageRepository.deleteById(id);
    }
}