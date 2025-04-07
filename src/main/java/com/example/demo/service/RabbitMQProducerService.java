package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

@Service
public class RabbitMQProducerService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQProducerService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange.plan}")
    private String exchange;

    @Value("${rabbitmq.routing.plan}")
    private String routingKey;

    public void sendMessage(JSONObject message) {
        try {
            logger.info("Sending message to RabbitMQ: {}", message);
            
            // Convert JSONObject to byte array
            String messageString = message.toString();
            byte[] messageBytes = messageString.getBytes(StandardCharsets.UTF_8);
            
            // Create message properties
            MessageProperties properties = new MessageProperties();
            properties.setContentType(MessageProperties.CONTENT_TYPE_JSON);
            properties.setContentEncoding(StandardCharsets.UTF_8.name());
            
            // Create message
            Message rabbitMessage = new Message(messageBytes, properties);
            
            // Send the message directly without using conversion
            rabbitTemplate.send(exchange, routingKey, rabbitMessage);
            
            logger.info("Message sent successfully");
        } catch (Exception e) {
            logger.error("Failed to send message to RabbitMQ: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to send message to RabbitMQ: " + e.getMessage(), e);
        }
    }
}