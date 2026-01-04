package com.ms.order.producer;

import jakarta.validation.Valid;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderProducer {
    private final RabbitTemplate rabbitTemplate;

    OrderProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @Value(value = "${broker.queue.order.cancelled.name}")
    private String cancelledOrderRoutingKey;

    @Value(value = "${broker.queue.product.update-stock.name}")
    private String updateStockRoutingKey;

    public void publishOrderCancelledEvent(@Valid Object orderCancelledEvent) {
        rabbitTemplate.convertAndSend(cancelledOrderRoutingKey, orderCancelledEvent);
    }

    public void publishUpdateStockEvent(@Valid Object updateStockEvent) {
        rabbitTemplate.convertAndSend(updateStockRoutingKey, updateStockEvent);
    }
}

