package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public List<User> getAll() {
        return userService.findAll();
    }

    @GetMapping("/{id}")
    public User getById(@PathVariable Integer id) {
        return userService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No user found with ID: " + id));
    }

    @PostMapping
    public User create(@Valid @RequestBody User user) {
        return userService.save(user);
    }

    @PutMapping("/{id}")
    public User update(@PathVariable Integer id, @Valid @RequestBody User updatedUser) {
        updatedUser.setId(id);
        return userService.save(updatedUser);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        userService.deleteById(id);
    }
}