package com.shihab.ecommerceapi.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${app.rabbitmq.queue_json}")
    private String queueJson;

    @Value("${app.rabbitmq.exchange_json}")
    private String exchangeJson;

    @Value("${app.rabbitmq.routingkey_json}")
    private String routingKeyJson;

    @Bean
    public Queue jsonQueue() {
        return new Queue(queueJson, true);
    }

    @Bean
    public TopicExchange jsonExchange() {
        return new TopicExchange(exchangeJson);
    }


    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }

    @Bean
    public Binding jsonBinding() {
        return BindingBuilder.bind(jsonQueue()).
                to(jsonExchange()).with(routingKeyJson);
    }
}


// @Value("${app.rabbitmq.queue}")
//    private String queue;
//
//    @Value("${app.rabbitmq.exchange}")
//    private String exchange;
//
//    @Value("${app.rabbitmq.routingkey}")
//    private String routingKey;
//  @Bean
//    public Queue normalQueue() {
//        return new Queue(queue, true);
//    }
//
//    @Bean
//    public TopicExchange normalExchange() {
//        return new TopicExchange(exchange);
//    }
//
//    @Bean
//    public Binding binding() {
//        return BindingBuilder.
//                bind(normalQueue()).to(normalExchange()).
//                with(routingKey);
//    }