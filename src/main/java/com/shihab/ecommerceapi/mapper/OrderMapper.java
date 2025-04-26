package com.shihab.ecommerceapi.mapper;

import com.shihab.ecommerceapi.dto.OrderDto;
import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.model.User;

public class OrderMapper {

    public static OrderDto toDto(Order order) {
        OrderDto dto = new OrderDto();
        dto.setId(order.getId());
        dto.setUserId(order.getUser().getId());
        dto.setTotal(order.getTotal());
        dto.setStatus(order.getStatus().name());
        return dto;
    }

    public static Order toEntity(OrderDto dto, User user) {
        Order order = new Order();
        order.setId(dto.getId());
        order.setUser(user);
        order.setTotal(dto.getTotal());
        order.setStatus(Order.Status.valueOf(dto.getStatus()));
        return order;
    }
}