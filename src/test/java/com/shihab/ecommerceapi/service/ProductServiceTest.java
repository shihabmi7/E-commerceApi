package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Test
    void findAll_returnsAllProducts() {
        Product p1 = new Product(1, "Laptop", "desc", 999.0, 10, null);
        Product p2 = new Product(2, "Phone", "desc", 499.0, 5, null);
        when(productRepository.findAll()).thenReturn(List.of(p1, p2));

        List<Product> result = productService.findAll();

        assertThat(result).hasSize(2);
        verify(productRepository).findAll();
    }

    @Test
    void findById_returnsProduct_whenExists() {
        Product product = new Product(1, "Laptop", "desc", 999.0, 10, null);
        when(productRepository.findById(1)).thenReturn(Optional.of(product));

        Optional<Product> result = productService.findById(1);

        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Laptop");
    }

    @Test
    void findById_returnsEmpty_whenNotExists() {
        when(productRepository.findById(99)).thenReturn(Optional.empty());

        Optional<Product> result = productService.findById(99);

        assertThat(result).isEmpty();
    }

    @Test
    void save_persistsAndReturnsProduct() {
        Product product = new Product(null, "Laptop", "desc", 999.0, 10, null);
        Product saved = new Product(1, "Laptop", "desc", 999.0, 10, null);
        when(productRepository.save(product)).thenReturn(saved);

        Product result = productService.save(product);

        assertThat(result.getId()).isEqualTo(1);
        verify(productRepository).save(product);
    }

    @Test
    void deleteById_callsRepository() {
        doNothing().when(productRepository).deleteById(1);

        productService.deleteById(1);

        verify(productRepository).deleteById(1);
    }
}
