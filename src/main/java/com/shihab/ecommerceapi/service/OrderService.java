package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.dto.PlaceOrderRequest;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.OrderItemRepository;
import com.shihab.ecommerceapi.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final Counter ordersPlaced;

    public OrderService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         CartService cartService,
                         MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.ordersPlaced = Counter.builder("orders_placed_total")
                .description("Total number of orders placed")
                .register(meterRegistry);
    }

    public List<Order> findAll() {
        return orderRepository.findAll();
    }

    public Optional<Order> findById(Integer id) {
        return orderRepository.findById(id);
    }

    public Order save(Order order) {
        return orderRepository.save(order);
    }

    public void deleteById(Integer id) {
        orderRepository.deleteById(id);
    }

    // existing CRUD methods omitted…

    @Transactional
    public Order placeOrder(PlaceOrderRequest req) {
        // 1) fetch cart items
        List<Cart> carts = cartService.findByUserId(req.getUserId());

        // 2) compute total using the price snapshotted at add-to-cart time,
        // not the product's current (possibly changed) price
        double total = carts.stream()
                .mapToDouble(c -> c.getPrice() * c.getQuantity())
                .sum();

        // 3) create Order
        Order order = new Order();
        User aUser = new User();
        aUser.setId(req.getUserId());
        order.setUser(aUser);
        order.setTotal(total);
        order.setStatus(Order.Status.pending);
        Order savedOrder = orderRepository.save(order);

        // 4) create OrderItems
        for (Cart cart : carts) {
            OrderItem item = new OrderItem();
            item.setOrder(savedOrder);
            item.setProduct(cart.getProduct());
            item.setQuantity(cart.getQuantity());
            item.setPrice(cart.getPrice());
            orderItemRepository.save(item);
        }

        // 5) clear the cart
        carts.forEach(c -> cartService.deleteById(c.getId()));

        // 6) business metric: shows up in Grafana as orders_placed_total
        ordersPlaced.increment();
        return savedOrder;
    }

}