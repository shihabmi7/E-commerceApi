package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Review;
import com.shihab.ecommerceapi.service.ReviewService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/reviews")
public class ReviewController {
    
    @Autowired
    private ReviewService reviewService;

    @GetMapping
    public List<Review> getAll() {
        return reviewService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Review> getById(@PathVariable Integer id) {
        return reviewService.findById(id);
    }

    @PostMapping
    public Review create(@RequestBody Review review) {
        return reviewService.save(review);
    }

    @PutMapping("/{id}")
    public Review update(@PathVariable Integer id, @RequestBody Review updatedReview) {
        updatedReview.setId(id);
        return reviewService.save(updatedReview);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        reviewService.deleteById(id);
    }
}