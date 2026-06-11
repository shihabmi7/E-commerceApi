package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.Category;
import com.shihab.ecommerceapi.repository.CategoryRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    @Test
    void findAll_returnsAllCategories() {
        Category c = new Category(1, "Electronics", "All electronics");
        when(categoryRepository.findAll()).thenReturn(List.of(c));

        List<Category> result = categoryService.findAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("Electronics");
    }

    @Test
    void findById_returnsCategory_whenPresent() {
        Category c = new Category(1, "Electronics", "All electronics");
        when(categoryRepository.findById(1)).thenReturn(Optional.of(c));

        assertThat(categoryService.findById(1)).isPresent();
    }

    @Test
    void findById_returnsEmpty_whenAbsent() {
        when(categoryRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(categoryService.findById(99)).isEmpty();
    }

    @Test
    void save_delegatesToRepository() {
        Category c = new Category(null, "Books", "All books");
        Category saved = new Category(3, "Books", "All books");
        when(categoryRepository.save(c)).thenReturn(saved);

        assertThat(categoryService.save(c).getId()).isEqualTo(3);
    }

    @Test
    void deleteById_delegates() {
        doNothing().when(categoryRepository).deleteById(1);
        categoryService.deleteById(1);
        verify(categoryRepository).deleteById(1);
    }
}
