package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.ProductService;
import com.shihab.ecommerceapi.service.UserDetailsServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Optional;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(
        controllers = ProductController.class,
        excludeAutoConfiguration = SecurityAutoConfiguration.class
)
class ProductControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @MockBean
    private JwtService jwtService;

    @MockBean
    private UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_returns200WithProductList() throws Exception {
        Product p = new Product(1, "Laptop", "desc", 999.0, 10, null);
        when(productService.findAll()).thenReturn(List.of(p));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Laptop"))
                .andExpect(jsonPath("$[0].price").value(999.0));
    }

    @Test
    void getById_returns200_whenProductExists() throws Exception {
        Product p = new Product(1, "Laptop", "desc", 999.0, 10, null);
        when(productService.findById(1)).thenReturn(Optional.of(p));

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop"));
    }

    @Test
    void getById_returnsEmpty_whenProductNotFound() throws Exception {
        when(productService.findById(99)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isOk());
    }

    @Test
    void create_returns200WithSavedProduct() throws Exception {
        Product input = new Product(null, "Phone", "desc", 499.0, 5, null);
        Product saved = new Product(2, "Phone", "desc", 499.0, 5, null);
        when(productService.save(any(Product.class))).thenReturn(saved);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(input)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(2));
    }

    @Test
    void update_returns200WithUpdatedProduct() throws Exception {
        Product updated = new Product(1, "Laptop Pro", "new desc", 1299.0, 3, null);
        when(productService.save(any(Product.class))).thenReturn(updated);

        mockMvc.perform(put("/api/products/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(updated)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Laptop Pro"));
    }

    @Test
    void delete_returns200() throws Exception {
        doNothing().when(productService).deleteById(1);

        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isOk());
    }
}
