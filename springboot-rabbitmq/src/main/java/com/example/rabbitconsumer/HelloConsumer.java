package com.example.rabbitconsumer;

import org.springframework.amqp.rabbit.annotation.Queue;
import org.springframework.amqp.rabbit.annotation.RabbitHandler;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

/**
 * @author machao
 * @date 2021/7/28
 */
@Component
@RabbitListener(queuesToDeclare = @Queue("hello"))
public class HelloConsumer {

    @RabbitHandler
    public void consumer(String msg) {
        System.out.println(msg);
    }
}
