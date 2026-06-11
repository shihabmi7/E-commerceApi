package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Discount;
import com.shihab.ecommerceapi.service.DiscountService;
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

@WebMvcTest(controllers = DiscountController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class DiscountControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean DiscountService discountService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(discountService.findAll()).thenReturn(List.of(new Discount())); mockMvc.perform(get("/api/discounts")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(discountService.findById(1)).thenReturn(Optional.of(new Discount())); mockMvc.perform(get("/api/discounts/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { Discount d = new Discount(); when(discountService.save(any())).thenReturn(d); mockMvc.perform(post("/api/discounts").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(d))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(discountService).deleteById(1); mockMvc.perform(delete("/api/discounts/1")).andExpect(status().isOk()); }
}
