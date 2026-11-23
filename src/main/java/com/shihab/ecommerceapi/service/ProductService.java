package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.repository.ProductRepository;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository,
                          MeterRegistry meterRegistry,
                          @Value("${app.inventory.low-stock-threshold:5}") int lowStockThreshold) {
        this.productRepository = productRepository;

        // business metric: how many products are almost out of stock.
        // Prometheus reads it on every scrape (runs one small count query).
        Gauge.builder("products_low_stock", productRepository,
                        repo -> repo.countByStockLessThan(lowStockThreshold))
                .description("Number of products with stock below the threshold")
                .register(meterRegistry);
    }

    public List<Product> findAll() {
        return productRepository.findAll();
    }

    public Optional<Product> findById(Integer id) {
        return productRepository.findById(id);
    }

    public Product save(Product product) {
        return productRepository.save(product);
    }

    public void deleteById(Integer id) {
        productRepository.deleteById(id);
    }
}