package com.shihab.ecommerceapi.rabbitmq;

import com.shihab.ecommerceapi.event.OrderPlacedEvent;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.AmqpException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Sends an "order placed" message to RabbitMQ once the order is safely committed.
 * A RabbitMQ outage must never fail or roll back the order, so errors are logged and counted only.
 */
@Component
public class OrderPlacedEventHandler {

    private static final Logger log = LoggerFactory.getLogger(OrderPlacedEventHandler.class);

    private final OrderMessageSender sender;
    private final Counter publishFailures;

    public OrderPlacedEventHandler(OrderMessageSender sender, MeterRegistry meterRegistry) {
        this.sender = sender;
        this.publishFailures = Counter.builder("order_messages_publish_failed_total")
                .description("Order messages that could not be sent to RabbitMQ")
                .register(meterRegistry);
    }

    // fallbackExecution: still publish if an order is ever placed outside a transaction
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onOrderPlaced(OrderPlacedEvent event) {
        OrderMessage message = new OrderMessage();
        message.setOrderId(String.valueOf(event.orderId()));
        message.setMessage("Order " + event.orderId() + " placed by user " + event.userId());
        message.setAmount(event.total());
        try {
            sender.sendOrderMessage(message);
        } catch (AmqpException e) {
            publishFailures.increment();
            log.warn("Could not publish message for order {}: {}", event.orderId(), e.getMessage());
        }
    }
}
