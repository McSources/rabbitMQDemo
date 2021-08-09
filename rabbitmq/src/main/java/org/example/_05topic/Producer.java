package org.example._05topic;

import com.rabbitmq.client.BuiltinExchangeType;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author machao
 * @date 2021/7/27
 */
public class Producer {
    private static final String EXCHANGE = "user_topic";
    private static final String KEY1 = "user.update";
    private static final String KEY2 = "user.update.name";
    private static final String KEY3 = "user.create.name";
    private static final String KEY4 = "user.create.child.name";
    private static final String KEY5 = "user.update.child.name";

    public static void main(String[] args) throws IOException {
        //创建ConnectionFactory
        ConnectionFactory factory = new ConnectionFactory();
        //从配置文件中读取连接配置，包括 username、password等
        factory.load("classpath:/client.properties");
        //创建连接，用连接创建通道
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            //声明一个直连类型的交换机
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.TOPIC);
            //发布消息到指定的交换机
            //String exchange：指定交换机
            //String routingKey：routingKey，指定发布到哪个队列
            //BasicProperties props：指定消息的一些属性。MessageProperties中包含了一些常用的属性组合。
            //byte[] body：消息内容
            channel.basicPublish(EXCHANGE, KEY1, null, ("Hello topic key " + KEY1).getBytes());
            channel.basicPublish(EXCHANGE, KEY2, null, ("Hello topic key " + KEY2).getBytes());
            channel.basicPublish(EXCHANGE, KEY3, null, ("Hello topic key " + KEY3).getBytes());
            channel.basicPublish(EXCHANGE, KEY4, null, ("Hello topic key " + KEY4).getBytes());
            channel.basicPublish(EXCHANGE, KEY5, null, ("Hello topic key " + KEY5).getBytes());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
