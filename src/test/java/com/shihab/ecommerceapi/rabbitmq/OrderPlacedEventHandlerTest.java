package com.shihab.ecommerceapi.rabbitmq;

import com.shihab.ecommerceapi.event.OrderPlacedEvent;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpConnectException;

import java.net.ConnectException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class OrderPlacedEventHandlerTest {

    @Mock private OrderMessageSender sender;

    private SimpleMeterRegistry meterRegistry;
    private OrderPlacedEventHandler handler;

    @BeforeEach
    void setUp() {
        meterRegistry = new SimpleMeterRegistry();
        handler = new OrderPlacedEventHandler(sender, meterRegistry);
    }

    @Test
    void onOrderPlaced_sendsMessageWithOrderDetails() {
        handler.onOrderPlaced(new OrderPlacedEvent(12, 3, 99.5));

        ArgumentCaptor<OrderMessage> captor = ArgumentCaptor.forClass(OrderMessage.class);
        verify(sender).sendOrderMessage(captor.capture());
        assertThat(captor.getValue().getOrderId()).isEqualTo("12");
        assertThat(captor.getValue().getAmount()).isEqualTo(99.5);
        assertThat(captor.getValue().getMessage()).contains("12").contains("3");
    }

    @Test
    void onOrderPlaced_whenBrokerIsDown_doesNotThrow_andCountsFailure() {
        doThrow(new AmqpConnectException(new ConnectException("refused")))
                .when(sender).sendOrderMessage(any(OrderMessage.class));

        assertThatCode(() -> handler.onOrderPlaced(new OrderPlacedEvent(12, 3, 99.5)))
                .doesNotThrowAnyException();
        assertThat(meterRegistry.get("order_messages_publish_failed_total").counter().count()).isEqualTo(1.0);
    }
}
