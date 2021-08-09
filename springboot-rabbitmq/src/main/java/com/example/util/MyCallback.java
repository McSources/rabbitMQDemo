package com.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import java.util.HashMap;
import java.util.Map;

import static com.example.util.Constant.BACKUP_EXCHANGE;

/**
 * @author machao
 * @date 2021/7/30
 */
@Slf4j
public class MyCallback implements RabbitTemplate.ConfirmCallback, RabbitTemplate.ReturnsCallback {

    private final RabbitTemplate template;

    public MyCallback(RabbitTemplate template) {
        this.template = template;
    }

    @Override
    public void confirm(CorrelationData correlationData, boolean ack, String cause) {
        String id = "";
        ReturnedMessage returnedMessage = null;
        if (correlationData != null) {
            id = correlationData.getId();
            returnedMessage = correlationData.getReturned();
        }

        log.info("CONFIRM_CALLBACK: {id: {}, ack: {}, msg: {}, cause: {}}", id, ack, returnedMessage, cause);
    }

    @Override
    public void returnedMessage(ReturnedMessage returned) {
        log.info("发送到交换机 {} 成功，路由到队列失败：{}，将转发到备份交换机", returned.getExchange(), returned.getReplyText());
        //记录消息的原交换机、原路由和消息内容，并发送到备份交换机做备份和发出警告。
        Map<String, Object> map = new HashMap<>(3);
        map.put("content", returned.getMessage());
        map.put("exchange", returned.getExchange());
        map.put("routingKey", returned.getRoutingKey());
        template.convertAndSend(BACKUP_EXCHANGE, "", map);
    }
}
