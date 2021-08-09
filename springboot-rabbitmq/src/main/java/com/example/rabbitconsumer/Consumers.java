package com.example.rabbitconsumer;

import com.rabbitmq.client.Channel;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.Map;

import static com.example.util.Constant.*;

/**
 * 消费者们
 *
 * @author machao
 * @date 2021/7/31
 */
@Component
@Slf4j
public class Consumers {
    /**
     * work队列的消费者1
     */
    @RabbitListener(queuesToDeclare = @Queue("work"))
    public void consumer1(String msg) {
        log.info("consumer1 " + msg);
    }

    /**
     * work队列的消费者2
     */
    @RabbitListener(queuesToDeclare = @Queue("work"))
    public void consumer2(String msg) {
        log.info("consumer2 " + msg);
    }

    /**
     * 死信队列消费者
     */
    @RabbitListener(queues = DEAD_QUEUE, ackMode = "AUTO")
    public void receiveDeadMsg(Message message) {
        String msg = new String(message.getBody());
        log.info("当前时间：{},收到死信队列信息{}", FORMAT.format(new Date()), msg);
    }

    /**
     * 延迟队列的消费者
     */
    @RabbitListener(queues = DELAY_QUEUE, ackMode = "AUTO")
    public void receiveDelayedQueue(Message message) {
        String msg = new String(message.getBody());
        log.info("当前时间：{},收到延时队列的消息：{}", FORMAT.format(new Date()), msg);
    }

    /**
     * 发布确认模式中，备用交换机的消费者
     * 备用交换机负责接收发送到其他交换机但是无法路由的消息，因此一般是 fanout 的
     *
     * @QueueBinding 将一个新声明的匿名队列绑定到了一个已存在的交换机上，交换机不需要声明。
     */
    @RabbitListener(bindings = @QueueBinding(value = @Queue(), exchange = @Exchange(value = BACKUP_EXCHANGE, type = "fanout", declare = "false")))
    public void backupConsumer(Map<String, Object> msg, Channel channel) {
        log.info("备份消息内容：{}，消息原交换机：{}，消息原路由：{}", msg.get("content"), msg.get("exchange"), msg.get("routingKey"));
    }

    /**
     * 优先级队列的消费者
     */
    @RabbitListener(queuesToDeclare = @Queue(value = PRIORITY_QUEUE, arguments = @Argument(name = "x-max-priority", value = "10", type = "java.lang.Integer")))
    public void priorityConsumer(Message message) {
        Integer priority = message.getMessageProperties().getPriority();
        log.info("priorityConsumer 当前时间：{},收到优先级为 {} 的消息：{}", FORMAT.format(new Date()), priority, new String(message.getBody()));
    }

    /**
     * 优先级队列的消费者2
     * 测试优先级队列是怎么给自己的消费者分配消息的
     */
    @RabbitListener(queues = PRIORITY_QUEUE)
    public void priorityConsumer2(Message message) {
        Integer priority = message.getMessageProperties().getPriority();
        log.info("priorityConsumer2 当前时间：{},收到优先级为 {} 的消息：{}", FORMAT.format(new Date()), priority, new String(message.getBody()));
    }
}
