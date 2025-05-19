package com.shihab.ecommerceapi.rabbitmq;

import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Service;

@Service
public class OrderMessageListener {

    @RabbitListener(queues = "${app.rabbitmq.queue}")
    public void generalMessageReceive(String message) {

        System.out.println("Received message: " + message);

    }

    @RabbitListener(queues = "${app.rabbitmq.queue_json}")
    public void orderMessageReceive(OrderMessage orderMessage) {

        System.out.println("Received message: " + orderMessage.toString());

    }

}
