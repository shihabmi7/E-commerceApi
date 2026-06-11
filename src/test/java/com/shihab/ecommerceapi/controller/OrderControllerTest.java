package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.dto.PlaceOrderRequest;
import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.OrderService;
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

@WebMvcTest(controllers = OrderController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class OrderControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderService orderService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_returns200() throws Exception {
        when(orderService.findAll()).thenReturn(List.of(new Order()));
        mockMvc.perform(get("/api/orders")).andExpect(status().isOk());
    }

    @Test
    void getById_returns200() throws Exception {
        when(orderService.findById(1)).thenReturn(Optional.of(new Order()));
        mockMvc.perform(get("/api/orders/1")).andExpect(status().isOk());
    }

    @Test
    void create_returns200() throws Exception {
        Order order = new Order();
        when(orderService.save(any())).thenReturn(order);
        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(order)))
                .andExpect(status().isOk());
    }

    @Test
    void placeOrder_returns200() throws Exception {
        PlaceOrderRequest req = new PlaceOrderRequest(7);
        Order placed = new Order();
        placed.setId(1);
        when(orderService.placeOrder(any())).thenReturn(placed);
        mockMvc.perform(post("/api/orders/place")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns200() throws Exception {
        doNothing().when(orderService).deleteById(1);
        mockMvc.perform(delete("/api/orders/1")).andExpect(status().isOk());
    }
}
