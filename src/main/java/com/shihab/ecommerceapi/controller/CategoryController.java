package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Category;
import com.shihab.ecommerceapi.service.CategoryService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController extends BaseController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping
    public List<Category> getAll() {
        return categoryService.findAll();
    }

    @GetMapping("/{id}")
    public Category getById(@PathVariable Integer id) {
        log.info("Category getById called!");
        return categoryService.findById(id).orElseThrow(() -> new EntityNotFoundException("" +
                "No category found with ID: " + id));
    }

    @PostMapping
    public Category create(@RequestBody Category category) {
        return categoryService.save(category);
    }

    @PutMapping("/{id}")
    public Category update(@PathVariable Integer id, @RequestBody Category updatedCategory) {
        updatedCategory.setId(id);
        return categoryService.save(updatedCategory);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        categoryService.deleteById(id);
    }
}