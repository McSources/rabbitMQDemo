package org.example._07deadmsg;

import com.rabbitmq.client.*;

import static org.example._07deadmsg.Constant.*;

/**
 * @author machao
 * @date 2021/7/29
 */
public class DeadConsumer {
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.load("classpath:/client.properties");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.queueDeclare(DEAD_QUEUE, false, false, false, null);
        channel.queueBind(DEAD_QUEUE, DEAD_EXCHANGE, DEAD_KEY);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            System.out.println("DeadConsumer 接收死信队列的消息" + new String(delivery.getBody()));
        };
        channel.basicConsume(DEAD_QUEUE, true, deliverCallback, consumerTag -> {
        });
        System.out.println("等待接收死信队列消息.....");
    }
}
