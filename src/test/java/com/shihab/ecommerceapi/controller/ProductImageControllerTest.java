package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.ProductImage;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.ProductImageService;
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

@WebMvcTest(controllers = ProductImageController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ProductImageControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ProductImageService productImageService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(productImageService.findAll()).thenReturn(List.of(new ProductImage())); mockMvc.perform(get("/api/productimages")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(productImageService.findById(1)).thenReturn(Optional.of(new ProductImage())); mockMvc.perform(get("/api/productimages/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { ProductImage pi = new ProductImage(); when(productImageService.save(any())).thenReturn(pi); mockMvc.perform(post("/api/productimages").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(pi))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(productImageService).deleteById(1); mockMvc.perform(delete("/api/productimages/1")).andExpect(status().isOk()); }
}
