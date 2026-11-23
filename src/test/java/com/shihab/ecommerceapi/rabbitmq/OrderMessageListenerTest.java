package com.shihab.ecommerceapi.rabbitmq;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMessageListenerTest {

    @Test
    void orderMessageReceive_countsTheMessage() {
        SimpleMeterRegistry registry = new SimpleMeterRegistry();
        OrderMessageListener listener = new OrderMessageListener(registry);

        OrderMessage message = new OrderMessage();
        message.setOrderId("12");
        message.setAmount(99.5);
        message.setMessage("Order 12 placed by user 3");
        listener.orderMessageReceive(message);
        listener.orderMessageReceive(message);

        assertThat(registry.get("order_messages_received_total").counter().count()).isEqualTo(2.0);
    }
}
