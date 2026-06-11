package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Payment;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.PaymentService;
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

@WebMvcTest(controllers = PaymentController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class PaymentControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean PaymentService paymentService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(paymentService.findAll()).thenReturn(List.of(new Payment())); mockMvc.perform(get("/api/payments")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(paymentService.findById(1)).thenReturn(Optional.of(new Payment())); mockMvc.perform(get("/api/payments/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { Payment p = new Payment(); when(paymentService.save(any())).thenReturn(p); mockMvc.perform(post("/api/payments").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(p))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(paymentService).deleteById(1); mockMvc.perform(delete("/api/payments/1")).andExpect(status().isOk()); }
}
