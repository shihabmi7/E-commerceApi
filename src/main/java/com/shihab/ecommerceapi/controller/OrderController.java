package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.dto.PlaceOrderRequest;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<Order>>> getAll() {
        List<Order> orders = orderService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Orders fetched successfully", orders));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<Order>> getById(@PathVariable Integer id) {
        Order order = orderService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No order found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Order fetched successfully", order));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<Order>> create(@Valid @RequestBody Order order) {
        Order saved = orderService.save(order);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Order created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<Order>> update(@PathVariable Integer id, @Valid @RequestBody Order updatedOrder) {
        updatedOrder.setId(id);
        Order saved = orderService.save(updatedOrder);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Order updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        orderService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    // existing CRUD endpoints omitted…

    @PostMapping("/place")
    public ResponseEntity<CustomResponse<Order>> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        Order placed = orderService.placeOrder(req);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .replacePath("/api/orders/{id}")
                .buildAndExpand(placed.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Order placed successfully", placed));
    }

}
