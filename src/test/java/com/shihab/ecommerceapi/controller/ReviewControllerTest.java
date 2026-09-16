package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Product;
import com.shihab.ecommerceapi.model.Review;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.ReviewService;
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

@WebMvcTest(controllers = ReviewController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ReviewControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ReviewService reviewService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(reviewService.findAll()).thenReturn(List.of(new Review())); mockMvc.perform(get("/api/reviews")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(reviewService.findById(1)).thenReturn(Optional.of(new Review())); mockMvc.perform(get("/api/reviews/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception {
        User user = new User(); user.setId(1);
        Product product = new Product(); product.setId(1);
        Review r = new Review(1, user, product, 5, "Great product!");
        when(reviewService.save(any())).thenReturn(r);
        mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(r))).andExpect(status().isCreated());
    }
    @Test void deleteById() throws Exception { doNothing().when(reviewService).deleteById(1); mockMvc.perform(delete("/api/reviews/1")).andExpect(status().isNoContent()); }

    @Test
    void create_returns400_whenRatingOutOfRange() throws Exception {
        User user = new User(); user.setId(1);
        Product product = new Product(); product.setId(1);
        Review invalid = new Review(null, user, product, 999, "way out of range");

        mockMvc.perform(post("/api/reviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());

        verify(reviewService, never()).save(any());
    }
}
