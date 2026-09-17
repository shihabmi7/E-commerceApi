package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Review;
import com.shihab.ecommerceapi.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/reviews")
public class ReviewController {

    private final ReviewService reviewService;

    public ReviewController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Review>>> getAll() {
        List<Review> reviews = reviewService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Reviews fetched successfully", reviews));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Review>> getById(@PathVariable Integer id) {
        Review review = reviewService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No review found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Review fetched successfully", review));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Review>> create(@Valid @RequestBody Review review) {
        Review saved = reviewService.save(review);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Review created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Review>> update(@PathVariable Integer id, @Valid @RequestBody Review updatedReview) {
        updatedReview.setId(id);
        Review saved = reviewService.save(updatedReview);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Review updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        reviewService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
