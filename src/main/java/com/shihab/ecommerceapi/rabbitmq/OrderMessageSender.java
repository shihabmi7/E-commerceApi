package com.shihab.ecommerceapi.rabbitmq;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class OrderMessageSender {

    @Value("${app.rabbitmq.exchange_json}")
    private String exchangeJson;
    @Value("${app.rabbitmq.routingkey_json}")
    private String routingKeyjson;

    private final RabbitTemplate rabbitTemplate;

    final Logger logger = LoggerFactory.getLogger(OrderMessageSender.class);

    public OrderMessageSender(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    public void sendOrderMessage(OrderMessage orderMessage) {
        logger.debug("--JSON Message Sent--", orderMessage.toString());
        rabbitTemplate.convertAndSend(exchangeJson, routingKeyjson, orderMessage );
    }

}

//    @Value("${app.rabbitmq.exchange}")
//    private String exchange;
//    @Value("${app.rabbitmq.routingkey}")
//    private String routingKey;


//public void sendGeneralMessage(String message) {
//        logger.info("--Message Sent--", message);
//        rabbitTemplate.convertAndSend(exchange, routingKey, message );
//    }