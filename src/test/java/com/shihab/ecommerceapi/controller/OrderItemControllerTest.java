package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.OrderItemService;
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

@WebMvcTest(controllers = OrderItemController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class OrderItemControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean OrderItemService orderItemService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(orderItemService.findAll()).thenReturn(List.of(new OrderItem())); mockMvc.perform(get("/api/orderitems")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(orderItemService.findById(1)).thenReturn(Optional.of(new OrderItem())); mockMvc.perform(get("/api/orderitems/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { OrderItem oi = new OrderItem(); when(orderItemService.save(any())).thenReturn(oi); mockMvc.perform(post("/api/orderitems").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(oi))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(orderItemService).deleteById(1); mockMvc.perform(delete("/api/orderitems/1")).andExpect(status().isOk()); }
}
