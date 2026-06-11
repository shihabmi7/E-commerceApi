package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Review;
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
    @Test void create() throws Exception { Review r = new Review(); when(reviewService.save(any())).thenReturn(r); mockMvc.perform(post("/api/reviews").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(r))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(reviewService).deleteById(1); mockMvc.perform(delete("/api/reviews/1")).andExpect(status().isOk()); }
}
