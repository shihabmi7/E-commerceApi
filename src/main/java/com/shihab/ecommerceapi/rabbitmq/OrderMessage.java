package com.shihab.ecommerceapi.rabbitmq;

import lombok.Data;

@Data
public class OrderMessage {
    public String orderId;
    public String message;
    public double amount;
}
