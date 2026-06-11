package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Shipping;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.ShippingService;
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

@WebMvcTest(controllers = ShippingController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class ShippingControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean ShippingService shippingService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(shippingService.findAll()).thenReturn(List.of(new Shipping())); mockMvc.perform(get("/api/shippings")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(shippingService.findById(1)).thenReturn(Optional.of(new Shipping())); mockMvc.perform(get("/api/shippings/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { Shipping s = new Shipping(); when(shippingService.save(any())).thenReturn(s); mockMvc.perform(post("/api/shippings").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(s))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(shippingService).deleteById(1); mockMvc.perform(delete("/api/shippings/1")).andExpect(status().isOk()); }
}
