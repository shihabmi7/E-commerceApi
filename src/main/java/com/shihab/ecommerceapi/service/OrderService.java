package com.shihab.ecommerceapi.service;

import com.shihab.ecommerceapi.dto.PlaceOrderRequest;
import com.shihab.ecommerceapi.event.OrderPlacedEvent;
import com.shihab.ecommerceapi.exception.BadRequestException;
import com.shihab.ecommerceapi.exception.EntityNotFoundException;
import com.shihab.ecommerceapi.model.Address;
import com.shihab.ecommerceapi.model.Cart;
import com.shihab.ecommerceapi.model.Order;
import com.shihab.ecommerceapi.model.OrderItem;
import com.shihab.ecommerceapi.model.User;
import com.shihab.ecommerceapi.repository.AddressRepository;
import com.shihab.ecommerceapi.repository.OrderItemRepository;
import com.shihab.ecommerceapi.repository.OrderRepository;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartService cartService;
    private final AddressRepository addressRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final Counter ordersPlaced;

    public OrderService(OrderRepository orderRepository,
                         OrderItemRepository orderItemRepository,
                         CartService cartService,
                         AddressRepository addressRepository,
                         ApplicationEventPublisher eventPublisher,
                         MeterRegistry meterRegistry) {
        this.orderRepository = orderRepository;
        this.orderItemRepository = orderItemRepository;
        this.cartService = cartService;
        this.addressRepository = addressRepository;
        this.eventPublisher = eventPublisher;
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
        // 0) the shipping address must exist and belong to the ordering user
        Address address = addressRepository.findById(req.getAddressId())
                .orElseThrow(() -> new EntityNotFoundException("Address not found: " + req.getAddressId()));
        if (address.getUser() == null || !req.getUserId().equals(address.getUser().getId())) {
            throw new BadRequestException("Address " + req.getAddressId() + " does not belong to user " + req.getUserId());
        }

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
        order.setAddress(address);
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

        // 7) tell the rest of the system. The RabbitMQ message is sent only after this
        // transaction commits (see OrderPlacedEventHandler), so a rollback sends nothing.
        eventPublisher.publishEvent(new OrderPlacedEvent(savedOrder.getId(), req.getUserId(), total));
        return savedOrder;
    }

}