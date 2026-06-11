package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Wishlist;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.UserDetailsServiceImpl;
import com.shihab.ecommerceapi.service.WishlistService;
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

@WebMvcTest(controllers = WishlistController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class WishlistControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean WishlistService wishlistService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(wishlistService.findAll()).thenReturn(List.of(new Wishlist())); mockMvc.perform(get("/api/wishlists")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(wishlistService.findById(1)).thenReturn(Optional.of(new Wishlist())); mockMvc.perform(get("/api/wishlists/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { Wishlist w = new Wishlist(); when(wishlistService.save(any())).thenReturn(w); mockMvc.perform(post("/api/wishlists").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(w))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(wishlistService).deleteById(1); mockMvc.perform(delete("/api/wishlists/1")).andExpect(status().isOk()); }
}
