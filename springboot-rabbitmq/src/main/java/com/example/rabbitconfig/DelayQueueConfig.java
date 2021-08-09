package com.example.rabbitconfig;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.CustomExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;

import java.util.HashMap;
import java.util.Map;

import static com.example.util.Constant.*;

/**
 * @author machao
 * @date 2021/7/30
 */
public class DelayQueueConfig {

    @Bean
    public CustomExchange delayExchange() {
        Map<String, Object> args = new HashMap<String, Object>(1) {{
            put("x-delayed-type", "direct");
        }};
        return new CustomExchange(DELAY_EXCHANGE, DELAY_EXCHANGE_TYPE, true, false, args);
    }

    @Bean
    public Queue delayQueue() {
        return new Queue(DELAY_QUEUE);
    }

    @Bean
    @Qualifier
    public Binding delayBinding(CustomExchange delayExchange, Queue delayQueue) {
        return BindingBuilder.bind(delayQueue).to(delayExchange).with(DELAY_QUEUE_KEY).noargs();
    }
}
