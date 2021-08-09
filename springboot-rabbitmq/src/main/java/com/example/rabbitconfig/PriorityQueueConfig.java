package com.example.rabbitconfig;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.util.Constant.PRIORITY_QUEUE;

/**
 * 优先级队列
 *
 * @author machao
 * @date 2021/7/30
 */
@Configuration
public class PriorityQueueConfig {

    /**
     * 声明一个持久化的具名队列，具名队列会自动绑定到默认交换机上。
     */
    @Bean
    public Queue priorityQueue() {
        return QueueBuilder.durable(PRIORITY_QUEUE).maxPriority(10).build();
    }
}
