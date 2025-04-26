package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Discount;
import com.shihab.ecommerceapi.service.DiscountService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/discounts")
public class DiscountController {
    
    @Autowired
    private DiscountService discountService;

    @GetMapping
    public List<Discount> getAll() {
        return discountService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Discount> getById(@PathVariable Integer id) {
        return discountService.findById(id);
    }

    @PostMapping
    public Discount create(@RequestBody Discount discount) {
        return discountService.save(discount);
    }

    @PutMapping("/{id}")
    public Discount update(@PathVariable Integer id, @RequestBody Discount updatedDiscount) {
        updatedDiscount.setId(id);
        return discountService.save(updatedDiscount);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        discountService.deleteById(id);
    }
}