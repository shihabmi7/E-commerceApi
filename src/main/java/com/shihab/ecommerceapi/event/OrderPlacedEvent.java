package com.shihab.ecommerceapi.event;

/**
 * Published by OrderService when an order is placed.
 * It is turned into a RabbitMQ message only after the database transaction commits.
 */
public record OrderPlacedEvent(Integer orderId, Integer userId, double total) {
}
