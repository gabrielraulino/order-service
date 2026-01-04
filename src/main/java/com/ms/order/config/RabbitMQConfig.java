package com.ms.order.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${broker.queue.order.checkout.name}")
    private String checkoutQueue;

    @Value("${broker.queue.order.stock-update-failed.name}")
    private String stockUpdateFailedQueue;

    @Bean
    public Queue checkoutQueue() {
        return new Queue(checkoutQueue, true);
    }

    @Bean
    public Queue stockUpdateFailedQueue() {
        return new Queue(stockUpdateFailedQueue, true);
    }

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
        objectMapper.disable(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return new Jackson2JsonMessageConverter(objectMapper);
    }
}
