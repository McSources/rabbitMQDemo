package com.example.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.ReturnedMessage;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.util.concurrent.FailureCallback;
import org.springframework.util.concurrent.SuccessCallback;

import java.text.SimpleDateFormat;
import java.util.Locale;

/**
 * @author machao
 * @date 2021/7/30
 */
@Slf4j
public class Constant {
    public static final String DELAY_QUEUE = "delay-queue";
    public static final String DELAY_QUEUE_KEY = "delay";
    public static final String DELAY_EXCHANGE = "delay_exchange";
    public static final String DELAY_EXCHANGE_TYPE = "x-delayed-message";
    public static final String NORMAL_EXCHANGE = "normal_exchange";
    public static final String DEAD_EXCHANGE = "dead_exchange";
    public static final String NORMAL_QUEUE_A = "normal-queue-a";
    public static final String NORMAL_QUEUE_B = "normal-queue-b";
    public static final String DEAD_QUEUE = "dead-queue";
    public static final String DEAD_QUEUE_KEY = "dd";
    public static final String NORMAL_QUEUE_A_KEY = "na";
    public static final String NORMAL_QUEUE_B_KEY = "nb";
    public static final String LONELY_EXCHANGE = "lonely_exchange";
    public static final String BACKUP_EXCHANGE = "backup_exchange";
    public static final String PRIORITY_QUEUE = "priority-queue";

    public static final SimpleDateFormat FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault());

    public static final RabbitTemplate.ConfirmCallback CONFIRM_CALLBACK = (correlationData, ack, cause) -> {
        String id = "";
        ReturnedMessage returnedMessage = null;
        if (correlationData != null) {
            id = correlationData.getId();
            returnedMessage = correlationData.getReturned();
        }

        log.info("CONFIRM_CALLBACK: {id: {}, ack: {}, msg: {}, cause: {}}", id, ack, returnedMessage, cause);
    };
    public static final RabbitTemplate.ReturnsCallback RETURNS_CALLBACK = returned -> log.info("RETURNS_CALLBACK: {{}}", returned);
    public static final SuccessCallback<CorrelationData.Confirm> SUCCESS_CALLBACK = result -> log.info("SUCCESS_CALLBACK: {{}}", result);
    public static final FailureCallback FAILURE_CALLBACK = ex -> log.info("FAILURE_CALLBACK: ", ex);
}
