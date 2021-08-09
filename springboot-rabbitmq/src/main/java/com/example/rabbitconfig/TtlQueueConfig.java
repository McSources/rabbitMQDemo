package com.example.rabbitconfig;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

import static com.example.util.Constant.*;

/**
 * 延迟队列配置
 * 一个普通交换机，绑定两个延迟队列，
 * 延迟队列A，10s过期
 * 延迟队列B，40s过期
 * 两个延迟队列绑定一个死信交换机和死信队列。
 *
 * @author machao
 * @date 2021/7/29
 */
@Configuration
public class TtlQueueConfig {

    @Bean
    public DirectExchange normalExchange() {
        return new DirectExchange(NORMAL_EXCHANGE);
    }

    @Bean
    public DirectExchange deadExchange() {
        return new DirectExchange(DEAD_EXCHANGE);
    }

    @Bean
    public Queue normalQueueA() {
        Map<String, Object> args = new HashMap<String, Object>(3) {{
            //声明当前队列绑定的死信交换机
            put("x-dead-letter-exchange", DEAD_EXCHANGE);
            //声明当前队列的死信路由 key
            put("x-dead-letter-routing-key", DEAD_QUEUE_KEY);
            //声明队列的 TTL
            put("x-message-ttl", 10 * 1000);
        }};
        return QueueBuilder.durable(NORMAL_QUEUE_A).withArguments(args).build();
    }

    @Bean
    public Binding bindingAtoX(@Qualifier("normalQueueA") Queue normalQueueA, @Qualifier("normalExchange") DirectExchange normalExchange) {
        return BindingBuilder.bind(normalQueueA).to(normalExchange).with(NORMAL_QUEUE_A_KEY);
    }

    @Bean
    public Queue normalQueueB() {
        Map<String, Object> args = new HashMap<String, Object>(3) {{
            //声明当前队列绑定的死信交换机
            put("x-dead-letter-exchange", DEAD_EXCHANGE);
            //声明当前队列的死信路由 key
            put("x-dead-letter-routing-key", DEAD_QUEUE_KEY);
            //声明队列的 TTL
            put("x-message-ttl", 40 * 1000);
        }};
        return QueueBuilder.durable(NORMAL_QUEUE_B).withArguments(args).build();
    }

    @Bean
    public Binding bindingBtoX(@Qualifier("normalQueueB") Queue normalQueueB, @Qualifier("normalExchange") DirectExchange normalExchange) {
        return BindingBuilder.bind(normalQueueB).to(normalExchange).with(NORMAL_QUEUE_B_KEY);
    }

    @Bean
    public Queue deadQueue() {
        return new Queue(DEAD_QUEUE);
    }

    @Bean
    public Binding bindingDtoY(@Qualifier("deadQueue") Queue queueD, @Qualifier("deadExchange") DirectExchange yExchange) {
        return BindingBuilder.bind(queueD).to(yExchange).with(DEAD_QUEUE_KEY);
    }

}
