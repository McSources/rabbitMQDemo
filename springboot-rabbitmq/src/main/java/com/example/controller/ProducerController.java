package com.example.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.text.MessageFormat;
import java.util.Date;

import static com.example.util.Constant.*;

/**
 * @author machao
 * @date 2021/7/29
 */
@Slf4j
@RestController
public class ProducerController {


    private RabbitTemplate rabbitTemplate;

    @Autowired
    public void setRabbitTemplate(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    @PostMapping("publish")
    public String publish(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg) {
        rabbitTemplate.convertAndSend(ex, routingKey, msg);
        return MessageFormat.format("当前时间：{0}, 发送一条信息给交换机 {1}，路由为 {2}", FORMAT.format(new Date()), ex, routingKey);
    }

    @PostMapping("publishWithDelay")
    public String publishWithDelay(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg, @RequestParam Integer delay) {
        rabbitTemplate.convertAndSend(ex, routingKey, msg, message -> {
            message.getMessageProperties().setDelay(delay);
            return message;
        });
        return MessageFormat.format("当前时间：{0}, 发送一条延迟 {1} 毫秒的信息给交换机 {2}，路由为 {3}", FORMAT.format(new Date()), delay, ex, routingKey);
    }

    /**
     * 在自动注入完成后执行的初始化方法。
     */
//    @PostConstruct
//    public void postConstruct(){
//        rabbitTemplate.setConfirmCallback(CONFIRM_CALLBACK);
//        rabbitTemplate.setReturnsCallback(RETURNS_CALLBACK);
//    }
    @PostMapping("publishConfirm")
    public String publishAndConfirm(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg) {
        CorrelationData correlationData = new CorrelationData();
        correlationData.getFuture().addCallback(SUCCESS_CALLBACK, FAILURE_CALLBACK);
        rabbitTemplate.convertAndSend(ex, routingKey, msg, correlationData);
        return MessageFormat.format("当前时间：{0}, 发送一条信息给交换机 {1}，路由为 {2}", FORMAT.format(new Date()), ex, routingKey);
    }

    @PostMapping("publishPriority")
    public String publishPriority(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg, @RequestParam Integer priority) {
        rabbitTemplate.convertAndSend(ex, routingKey, msg, message -> {
            message.getMessageProperties().setPriority(priority);
            return message;
        });
        return MessageFormat.format("当前时间：{0}, 发送一条优先级为{3}的信息给交换机 {1}，路由为 {2}", FORMAT.format(new Date()), ex, routingKey, priority);
    }

    @PostMapping("publishBatchPriority")
    public String publishBatchPriority(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg, @RequestParam Integer count) {
        for (int i = 0; i < count; i++) {
            int finalI = i;
            rabbitTemplate.convertAndSend(ex, routingKey, "批量消息：" + msg + " " + i, (m) -> {
                m.getMessageProperties().setPriority(finalI + 10);
                return m;
            });
        }
        return MessageFormat.format("当前时间：{0}, 发送{3}条信息给交换机 {1}，路由为 {2}", FORMAT.format(new Date()), ex, routingKey, count);
    }

    @PostMapping("publishBatch")
    public String publishBatch(@RequestParam String ex, @RequestParam String routingKey, @RequestParam Object msg, @RequestParam Integer count) {
        for (int i = 0; i < count; i++) {
            rabbitTemplate.convertAndSend(ex, routingKey, "批量消息：" + msg + " " + i);
        }
        return MessageFormat.format("当前时间：{0}, 发送{3}条信息给交换机 {1}，路由为 {2}", FORMAT.format(new Date()), ex, routingKey, count);
    }
}
