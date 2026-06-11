package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.dto.AddToCartRequest;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.service.CartService;
import com.shihab.ecommerceapi.service.JwtService;
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

@WebMvcTest(controllers = CartController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CartControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean CartService cartService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getCart_returnsEmptyList_whenNoItems() throws Exception {
        when(cartService.findByUserId(1)).thenReturn(List.of());
        mockMvc.perform(get("/api/cart").param("userId", "1"))
                .andExpect(status().isOk())
                .andExpect(content().json("[]"));
    }

    @Test
    void getById_returnsCart() throws Exception {
        Cart cart = new Cart(1, null, new Product(1, "Laptop", "desc", 999.0, 5, null), 2);
        when(cartService.findById(1)).thenReturn(Optional.of(cart));
        mockMvc.perform(get("/api/cart/1"))
                .andExpect(status().isOk());
    }

    @Test
    void create_savesCart() throws Exception {
        Cart cart = new Cart(null, null, new Product(), 1);
        when(cartService.save(any())).thenReturn(new Cart(1, null, new Product(), 1));
        mockMvc.perform(post("/api/cart")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cart)))
                .andExpect(status().isOk());
    }

    @Test
    void addToCart_delegatesToService() throws Exception {
        AddToCartRequest req = new AddToCartRequest(1, 10, 3);
        Cart result = new Cart(5, null, new Product(), 3);
        when(cartService.addToCart(1, 10, 3)).thenReturn(result);
        mockMvc.perform(post("/api/cart/add")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns200() throws Exception {
        doNothing().when(cartService).deleteById(1);
        mockMvc.perform(delete("/api/cart/1"))
                .andExpect(status().isOk());
    }
}
