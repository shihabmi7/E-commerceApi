package com.shihab.ecommerceapi.rabbitmq;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1")
public class MessageController {

    private final OrderMessageSender orderMessageSender;

    public MessageController(OrderMessageSender sender) {
        this.orderMessageSender = sender;
    }

    @GetMapping("/publish")
    public ResponseEntity<String> sendMessageToRabbitMq(@RequestParam("message") String message) {
        orderMessageSender.sendGeneralMessage(message);
        return ResponseEntity.ok("Message has been sent!" + message);
    }

    @PostMapping("/publish")
    public ResponseEntity<String> sendMessageToQueue(@RequestBody OrderMessage orderMessage) {
        orderMessageSender.sendOrderMessage(orderMessage);
        return ResponseEntity.ok("Message has been sent!" + orderMessage.toString());
    }
}
