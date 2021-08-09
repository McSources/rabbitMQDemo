package com.example.rabbitconfig;

import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.ExchangeBuilder;
import org.springframework.amqp.core.FanoutExchange;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.example.util.Constant.BACKUP_EXCHANGE;
import static com.example.util.Constant.LONELY_EXCHANGE;

/**
 * @author machao
 * @date 2021/7/30
 */
@Configuration
public class PublishConfirmConfig {
    /**
     * 没有绑定队列的交换机
     * 但是绑定了备用交换机，往当前交换机发送失败的消息都会转发到备用交换机。
     * 这样不需要自己手写转发。
     */
    @Bean
    public DirectExchange lonelyExchange() {
        return ExchangeBuilder.directExchange(LONELY_EXCHANGE).alternate(BACKUP_EXCHANGE).build();
    }

    /**
     * 备份交换机，发送到lonely交换机的消息无法路由到队列，会被转发到备份交换机
     * 这个交换机的队列在消费者那里声明。
     */
    @Bean
    public FanoutExchange backupExchange() {
        return new FanoutExchange(BACKUP_EXCHANGE);
    }
}
