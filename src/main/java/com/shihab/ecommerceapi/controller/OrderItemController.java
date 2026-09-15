package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.service.OrderItemService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/orderitems")
public class OrderItemController {

    private final OrderItemService orderitemService;

    public OrderItemController(OrderItemService orderitemService) {
        this.orderitemService = orderitemService;
    }

    @GetMapping
    public List<OrderItem> getAll() {
        return orderitemService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<OrderItem> getById(@PathVariable Integer id) {
        return orderitemService.findById(id);
    }

    @PostMapping
    public OrderItem create(@Valid @RequestBody OrderItem orderitem) {
        return orderitemService.save(orderitem);
    }

    @PutMapping("/{id}")
    public OrderItem update(@PathVariable Integer id, @Valid @RequestBody OrderItem updatedOrderItem) {
        updatedOrderItem.setId(id);
        return orderitemService.save(updatedOrderItem);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        orderitemService.deleteById(id);
    }
}