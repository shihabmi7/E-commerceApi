package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.service.OrderItemService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orderitems")
public class OrderItemController {
    
    @Autowired
    private OrderItemService orderitemService;

    @GetMapping
    public List<OrderItem> getAll() {
        return orderitemService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<OrderItem> getById(@PathVariable Integer id) {
        return orderitemService.findById(id);
    }

    @PostMapping
    public OrderItem create(@RequestBody OrderItem orderitem) {
        return orderitemService.save(orderitem);
    }

    @PutMapping("/{id}")
    public OrderItem update(@PathVariable Integer id, @RequestBody OrderItem updatedOrderItem) {
        updatedOrderItem.setId(id);
        return orderitemService.save(updatedOrderItem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        orderitemService.deleteById(id);
    }
}