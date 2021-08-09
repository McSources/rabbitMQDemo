package org.example._07deadmsg;

import com.rabbitmq.client.*;

import java.util.HashMap;
import java.util.Map;

import static org.example._07deadmsg.Constant.*;

/**
 * @author machao
 * @date 2021/7/29
 */
public class NormalConsumer {

    public static void main(String[] argv) throws Exception {
        ConnectionFactory factory = new ConnectionFactory();
        factory.load("classpath:/client.properties");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //声明死信和普通交换机 类型为 direct
        channel.exchangeDeclare(NORMAL_EXCHANGE, BuiltinExchangeType.DIRECT);
        channel.exchangeDeclare(DEAD_EXCHANGE, BuiltinExchangeType.DIRECT);
        //声明死信队列
        channel.queueDeclare(DEAD_QUEUE, false, false, false, null);
        //绑定到死信交换机
        channel.queueBind(DEAD_QUEUE, DEAD_EXCHANGE, DEAD_KEY);
        //正常队列绑定死信队列信息
        Map<String, Object> params = new HashMap<>();
        //正常队列设置死信交换机 参数 key 是固定值
        params.put("x-dead-letter-exchange", DEAD_EXCHANGE);
        //正常队列设置死信 routing-key 参数 key 是固定值
        params.put("x-dead-letter-routing-key", DEAD_KEY);
        //正常队列设置队列长度
        params.put("x-max-length", 5);
        //声明普通队列
        channel.queueDeclare(NORMAL_QUEUE, false, false, false, params);
        //绑定到普通交换机
        channel.queueBind(NORMAL_QUEUE, NORMAL_EXCHANGE, NORMAL_KEY);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            //正常队列10秒钟拒绝一条消息。
            try {
                Thread.sleep(10 * 1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            channel.basicReject(delivery.getEnvelope().getDeliveryTag(), true);
            System.out.println("NormalConsumer 拒绝了消息" + new String(delivery.getBody()));
        };
        //限制当前消费者一次处理一条消息。
        channel.basicQos(1);
        channel.basicConsume(NORMAL_QUEUE, false, deliverCallback, consumerTag -> {
        });
        System.out.println("等待接收正常队列消息.....");
        //此消费者队列长度为2，超时时间为5s，此消费者10s拒绝一条消息。
        //生产者一次发10条消息，正常队列存放2条，被正常消费者拿到1条，剩下的7条全部进入死信队列。
        //正常队列中的两条，5s内没有被正常消费者消费，进入死信队列。
        //10s后消费者拒绝一条消息，该消息进入死信队列。
    }
}
