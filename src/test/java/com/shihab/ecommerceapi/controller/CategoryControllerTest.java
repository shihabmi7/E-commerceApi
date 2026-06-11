package com.shihab.ecommerceapi.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Category;
import com.shihab.ecommerceapi.service.CategoryService;
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

@WebMvcTest(controllers = CategoryController.class, excludeAutoConfiguration = SecurityAutoConfiguration.class)
class CategoryControllerTest {

    @Autowired MockMvc mockMvc;
    @Autowired ObjectMapper objectMapper;
    @MockBean CategoryService categoryService;
    @MockBean JwtService jwtService;
    @MockBean UserDetailsServiceImpl userDetailsService;

    @Test
    void getAll_returns200() throws Exception {
        Category c = new Category(1, "Electronics", "All electronics");
        when(categoryService.findAll()).thenReturn(List.of(c));
        mockMvc.perform(get("/api/categories"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Electronics"));
    }

    @Test
    void getById_returnsCategory_whenFound() throws Exception {
        Category c = new Category(1, "Books", "All books");
        when(categoryService.findById(1)).thenReturn(Optional.of(c));
        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Books"));
    }

    @Test
    void getById_returns404_whenNotFound() throws Exception {
        when(categoryService.findById(99)).thenReturn(Optional.empty());
        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returns200() throws Exception {
        Category c = new Category(null, "Toys", "All toys");
        when(categoryService.save(any())).thenReturn(new Category(3, "Toys", "All toys"));
        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(c)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(3));
    }

    @Test
    void delete_returns200() throws Exception {
        doNothing().when(categoryService).deleteById(1);
        mockMvc.perform(delete("/api/categories/1")).andExpect(status().isOk());
    }
}
