package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Category;
import com.shihab.ecommerceapi.service.CategoryService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
public class CategoryController extends BaseController {

    private final CategoryService categoryService;

    public CategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Category>>> getAll() {
        List<Category> categories = categoryService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Categories fetched successfully", categories));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Category>> getById(@PathVariable Integer id) {
        log.info("Category getById called!");
        Category category = categoryService.findById(id).orElseThrow(() -> new EntityNotFoundException("" +
                "No category found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Category fetched successfully", category));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Category>> create(@Valid @RequestBody Category category) {
        Category saved = categoryService.save(category);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Category created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Category>> update(@PathVariable Integer id, @Valid @RequestBody Category updatedCategory) {
        updatedCategory.setId(id);
        Category saved = categoryService.save(updatedCategory);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Category updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        categoryService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
