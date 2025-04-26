package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.repository.OrderItemRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class OrderItemService {
    
    @Autowired
    private OrderItemRepository orderitemRepository;

    public List<OrderItem> findAll() {
        return orderitemRepository.findAll();
    }

    public Optional<OrderItem> findById(Integer id) {
        return orderitemRepository.findById(id);
    }

    public OrderItem save(OrderItem orderitem) {
        return orderitemRepository.save(orderitem);
    }

    public void deleteById(Integer id) {
        orderitemRepository.deleteById(id);
    }
}