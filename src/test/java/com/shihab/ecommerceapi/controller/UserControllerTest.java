package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.service.JwtService;
import com.shihab.ecommerceapi.service.UserDetailsServiceImpl;
import com.shihab.ecommerceapi.service.UserService;
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

@WebMvcTest(controllers = UserController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class UserControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean UserService userService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_returns200() throws Exception {
        User u = new User(1, "Alice", "alice@example.com", "pass", "123", User.Role.customer, null);
        when(userService.findAll()).thenReturn(List.of(u));
        mockMvc.perform(get("/api/users"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].email").value("alice@example.com"));
    }

    @Test
    void getById_returns200_whenFound() throws Exception {
        User u = new User(1, "Alice", "alice@example.com", "pass", "123", User.Role.customer, null);
        when(userService.findById(1)).thenReturn(Optional.of(u));
        mockMvc.perform(get("/api/users/1")).andExpect(status().isOk());
    }

    @Test
    void create_returns200() throws Exception {
        User u = new User(null, "Bob", "bob@example.com", "pass", "456", User.Role.customer, null);
        when(userService.save(any())).thenReturn(new User(2, "Bob", "bob@example.com", "pass", "456", User.Role.customer, null));
        mockMvc.perform(post("/api/users")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(u)))
                .andExpect(status().isOk());
    }

    @Test
    void delete_returns200() throws Exception {
        doNothing().when(userService).deleteById(1);
        mockMvc.perform(delete("/api/users/1")).andExpect(status().isOk());
    }
}
