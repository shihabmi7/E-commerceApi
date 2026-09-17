package com.shihab.ecommerceapi.controller;

import com.shihab.ecommerceapi.dto.CustomResponse;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.service.OrderItemService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/v1/orderitems")
public class OrderItemController {

    private final OrderItemService orderitemService;

    public OrderItemController(OrderItemService orderitemService) {
        this.orderitemService = orderitemService;
    }

    @GetMapping
    public ResponseEntity<CustomResponse<List<OrderItem>>> getAll() {
        List<OrderItem> orderItems = orderitemService.findAll();
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Order items fetched successfully", orderItems));
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomResponse<OrderItem>> getById(@PathVariable Integer id) {
        OrderItem orderItem = orderitemService.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("No order item found with ID: " + id));
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Order item fetched successfully", orderItem));
    }

    @PostMapping
    public ResponseEntity<CustomResponse<OrderItem>> create(@Valid @RequestBody OrderItem orderitem) {
        OrderItem saved = orderitemService.save(orderitem);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(saved.getId())
                .toUri();
        return ResponseEntity.created(location)
                .body(new CustomResponse<>(HttpStatus.CREATED.value(), "Order item created successfully", saved));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomResponse<OrderItem>> update(@PathVariable Integer id, @Valid @RequestBody OrderItem updatedOrderItem) {
        updatedOrderItem.setId(id);
        OrderItem saved = orderitemService.save(updatedOrderItem);
        return ResponseEntity.ok(new CustomResponse<>(HttpStatus.OK.value(), "Order item updated successfully", saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        orderitemService.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
