package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.Address;
import com.shihab.ecommerceapi.service.AddressService;
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

@WebMvcTest(controllers = AddressController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class AddressControllerTest {
    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean AddressService addressService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test void getAll() throws Exception { when(addressService.findAll()).thenReturn(List.of(new Address())); mockMvc.perform(get("/api/addresses")).andExpect(status().isOk()); }
    @Test void getById() throws Exception { when(addressService.findById(1)).thenReturn(Optional.of(new Address())); mockMvc.perform(get("/api/addresses/1")).andExpect(status().isOk()); }
    @Test void create() throws Exception { Address a = new Address(); when(addressService.save(any())).thenReturn(a); mockMvc.perform(post("/api/addresses").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(a))).andExpect(status().isOk()); }
    @Test void deleteById() throws Exception { doNothing().when(addressService).deleteById(1); mockMvc.perform(delete("/api/addresses/1")).andExpect(status().isOk()); }
}
