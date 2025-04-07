package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.json.JSONObject;

import java.nio.charset.StandardCharsets;

@Service
public class RabbitMQConsumerService {

    private static final Logger logger = LoggerFactory.getLogger(RabbitMQConsumerService.class);

    @Autowired
    private ElasticsearchService elasticsearchService;

    @RabbitListener(queues = "${rabbitmq.queue.plan}")
    public void receiveMessage(Message message) {
        try {
            logger.info("Received message: {}", message);
            
            // Convert the message body to a String and log it for debugging
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.info("Message body: {}", messageBody);
            
            // Remove any surrounding quotes if present
            if (messageBody.startsWith("\"") && messageBody.endsWith("\"")) {
                messageBody = messageBody.substring(1, messageBody.length() - 1);
                // Also need to unescape any escaped quotes
                messageBody = messageBody.replace("\\\"", "\"");
                messageBody = messageBody.replace("\\\\", "\\");
            }
            
            // Parse the message as a JSONObject
            JSONObject jsonObject = new JSONObject(messageBody);
            
            // Check if it's a delete operation
            if (jsonObject.has("operation") && "delete".equals(jsonObject.getString("operation"))) {
                String objectId = jsonObject.getString("objectId");
                elasticsearchService.deletePlan(objectId);
                logger.info("Plan deleted from Elasticsearch: {}", objectId);
            } else {
                // Regular plan indexing
                elasticsearchService.indexPlan(jsonObject);
                logger.info("Plan indexed in Elasticsearch: {}", 
                        jsonObject.has("objectId") ? jsonObject.getString("objectId") : "unknown");
            }
            
        } catch (Exception e) {
            logger.error("Error processing message: {}", e.getMessage(), e);
            throw e; // This will trigger the retry mechanism
        }
    }

    @RabbitListener(queues = "${rabbitmq.queue.plan}.dlq")
    public void receiveDeadLetterMessage(Message message) {
        try {
            logger.error("Received dead letter message: {}", message);
            
            // Log the message content for debugging
            String messageBody = new String(message.getBody(), StandardCharsets.UTF_8);
            logger.error("Dead letter message content: {}", messageBody);
            
            // You could implement recovery logic here if needed
            
        } catch (Exception e) {
            logger.error("Error processing dead letter message: {}", e.getMessage(), e);
        }
    }
}