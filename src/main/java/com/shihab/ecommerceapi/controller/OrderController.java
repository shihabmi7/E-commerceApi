package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.service.OrderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    
    @Autowired
    private OrderService orderService;

    @GetMapping
    public List<Order> getAll() {
        return orderService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<Order> getById(@PathVariable Integer id) {
        return orderService.findById(id);
    }

    @PostMapping
    public Order create(@RequestBody Order order) {
        return orderService.save(order);
    }

    @PutMapping("/{id}")
    public Order update(@PathVariable Integer id, @RequestBody Order updatedOrder) {
        updatedOrder.setId(id);
        return orderService.save(updatedOrder);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        orderService.deleteById(id);
    }
}