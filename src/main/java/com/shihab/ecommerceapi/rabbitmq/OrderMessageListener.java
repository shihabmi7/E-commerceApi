package com.shihab.ecommerceapi.rabbitmq;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

/**
 * Receives "order placed" messages from RabbitMQ.
 * Today it logs the message and counts it. This is the place to add real follow-up work
 * (confirmation email, push notification, stock update...).
 */
@Service
public class OrderMessageListener {

    private static final Logger log = LoggerFactory.getLogger(OrderMessageListener.class);

    private final Counter received;

    public OrderMessageListener(MeterRegistry meterRegistry) {
        this.received = Counter.builder("order_messages_received_total")
                .description("Order messages received from RabbitMQ")
                .register(meterRegistry);
    }

    @RabbitListener(queues = "${app.rabbitmq.queue_json}")
    public void orderMessageReceive(OrderMessage orderMessage) {
        log.info("Order message received: orderId={}, amount={}, message={}",
                orderMessage.getOrderId(), orderMessage.getAmount(), orderMessage.getMessage());
        received.increment();
    }
}
