package com.dxd.oj.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JudgeMqConfig {

    @Value("${oj.judge.mq.exchange:oj.judge.exchange}")
    private String exchangeName;

    @Value("${oj.judge.mq.queue:oj.judge.queue}")
    private String queueName;

    @Value("${oj.judge.mq.routing-key:oj.judge.submit}")
    private String routingKey;

    @Bean
    public DirectExchange judgeExchange() {
        return new DirectExchange(exchangeName, true, false);
    }

    @Bean
    public Queue judgeQueue() {
        return new Queue(queueName, true, false, false);
    }

    @Bean
    public Binding judgeBinding(Queue judgeQueue, DirectExchange judgeExchange) {
        return BindingBuilder.bind(judgeQueue).to(judgeExchange).with(routingKey);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
