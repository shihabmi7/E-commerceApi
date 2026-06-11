package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.dto.PlaceOrderRequest;
import com.shihab.ecommerceapi.model.*;
import com.shihab.ecommerceapi.repository.OrderItemRepository;
import com.shihab.ecommerceapi.repository.OrderRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock private OrderRepository orderRepository;
    @Mock private OrderItemRepository orderItemRepository;
    @Mock private CartService cartService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void findAll_returnsAll() {
        when(orderRepository.findAll()).thenReturn(List.of(new Order()));
        assertThat(orderService.findAll()).hasSize(1);
    }

    @Test
    void findById_returnsOrder_whenPresent() {
        Order order = new Order();
        order.setId(1);
        when(orderRepository.findById(1)).thenReturn(Optional.of(order));
        assertThat(orderService.findById(1)).isPresent();
    }

    @Test
    void findById_returnsEmpty_whenAbsent() {
        when(orderRepository.findById(99)).thenReturn(Optional.empty());
        assertThat(orderService.findById(99)).isEmpty();
    }

    @Test
    void save_persistsOrder() {
        Order order = new Order();
        when(orderRepository.save(order)).thenReturn(order);
        assertThat(orderService.save(order)).isEqualTo(order);
    }

    @Test
    void deleteById_delegates() {
        doNothing().when(orderRepository).deleteById(1);
        orderService.deleteById(1);
        verify(orderRepository).deleteById(1);
    }

    @Test
    void placeOrder_computesTotalAndCreatesOrderItems() {
        Product p1 = new Product(1, "Laptop", "desc", 1000.0, 5, null);
        Product p2 = new Product(2, "Mouse", "desc", 50.0, 10, null);

        Cart c1 = new Cart(1, new User(), p1, 2);
        Cart c2 = new Cart(2, new User(), p2, 1);

        when(cartService.findByUserId(7)).thenReturn(List.of(c1, c2));

        Order savedOrder = new Order();
        savedOrder.setId(100);
        savedOrder.setTotal(2050.0);
        savedOrder.setStatus(Order.Status.pending);

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
        when(orderItemRepository.save(any(OrderItem.class))).thenAnswer(inv -> inv.getArgument(0));

        PlaceOrderRequest req = new PlaceOrderRequest(7);
        Order result = orderService.placeOrder(req);

        assertThat(result.getId()).isEqualTo(100);
        assertThat(result.getTotal()).isEqualTo(2050.0); // 2*1000 + 1*50

        // Two order items saved
        verify(orderItemRepository, times(2)).save(any(OrderItem.class));

        // Cart items cleared
        verify(cartService).deleteById(1);
        verify(cartService).deleteById(2);
    }

    @Test
    void placeOrder_withEmptyCart_createsOrderWithZeroTotal() {
        when(cartService.findByUserId(7)).thenReturn(List.of());

        Order savedOrder = new Order();
        savedOrder.setId(101);
        savedOrder.setTotal(0.0);
        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        PlaceOrderRequest req = new PlaceOrderRequest(7);
        Order result = orderService.placeOrder(req);

        assertThat(result.getTotal()).isEqualTo(0.0);
        verify(orderItemRepository, never()).save(any());
    }
}
