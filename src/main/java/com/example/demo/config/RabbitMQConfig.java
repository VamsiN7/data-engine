package com.example.demo.config;

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

    @Value("${rabbitmq.queue.plan}")
    private String planQueue;

    @Value("${rabbitmq.exchange.plan}")
    private String planExchange;

    @Value("${rabbitmq.routing.plan}")
    private String planRoutingKey;

    @Bean
    public Queue planQueue() {
        return QueueBuilder.durable(planQueue)
                .withArgument("x-dead-letter-exchange", planExchange + ".dlx")
                .withArgument("x-dead-letter-routing-key", planQueue + ".dlq")
                .build();
    }

    @Bean
    public DirectExchange planExchange() {
        return new DirectExchange(planExchange);
    }

    @Bean
    public Binding planBinding(Queue planQueue, DirectExchange planExchange) {
        return BindingBuilder.bind(planQueue)
                .to(planExchange)
                .with(planRoutingKey);
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(planQueue + ".dlq").build();
    }

    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange(planExchange + ".dlx");
    }

    @Bean
    public Binding deadLetterBinding(Queue deadLetterQueue, DirectExchange deadLetterExchange) {
        return BindingBuilder.bind(deadLetterQueue)
                .to(deadLetterExchange)
                .with(planQueue + ".dlq");
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }
} 