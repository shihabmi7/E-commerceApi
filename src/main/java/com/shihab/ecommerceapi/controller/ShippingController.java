package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Shipping;
import com.shihab.ecommerceapi.service.ShippingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/shippings")
public class ShippingController {
    
    @Autowired
    private ShippingService shippingService;

    @GetMapping
    public List<Shipping> getAll() {
        return shippingService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Shipping> getById(@PathVariable Integer id) {
        return shippingService.findById(id);
    }

    @PostMapping
    public Shipping create(@RequestBody Shipping shipping) {
        return shippingService.save(shipping);
    }

    @PutMapping("/{id}")
    public Shipping update(@PathVariable Integer id, @RequestBody Shipping updatedShipping) {
        updatedShipping.setId(id);
        return shippingService.save(updatedShipping);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        shippingService.deleteById(id);
    }
}