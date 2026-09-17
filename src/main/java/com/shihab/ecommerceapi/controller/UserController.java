package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<User>>> getAll() {
        List<User> users = userService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Users fetched successfully", users));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<User>> getById(@PathVariable Integer id) {
        User user = userService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No user found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "User fetched successfully", user));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<User>> create(@Valid @RequestBody User user) {
        User saved = userService.save(user);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "User created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<User>> update(@PathVariable Integer id, @Valid @RequestBody User updatedUser) {
        updatedUser.setId(id);
        User saved = userService.save(updatedUser);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "User updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
