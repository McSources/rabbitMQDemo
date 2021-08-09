package org.example._07deadmsg;

import com.rabbitmq.client.*;

import static org.example._07deadmsg.Constant.NORMAL_EXCHANGE;
import static org.example._07deadmsg.Constant.NORMAL_KEY;

/**
 * @author machao
 * @date 2021/7/29
 */
public class Producer {
    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.load("classpath:/client.properties");
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
            //设置消息的 TTL 时间
            AMQP.BasicProperties properties = new AMQP.BasicProperties.Builder().expiration("5000").build();
            //该信息是用作演示队列个数限制
            for (int i = 0; i < 10; i++) {
                String message = "info" + i;
                channel.basicPublish(NORMAL_EXCHANGE, NORMAL_KEY, properties, message.getBytes());
                System.out.println("生产者发送消息:" + message);
            }
        }
    }
}

