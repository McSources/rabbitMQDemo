package org.example._03fanout;

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
    public static void main(String[] args) throws IOException {
        //创建ConnectionFactory
        ConnectionFactory factory = new ConnectionFactory();
        //从配置文件中读取连接配置，包括 username、password等
        factory.load("classpath:/client.properties");
        //创建连接，用连接创建通道
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            //声明一个广播类型的交换机
            channel.exchangeDeclare("logs", BuiltinExchangeType.FANOUT);
            //发布消息到指定的交换机
            //String exchange：指定交换机
            //String routingKey：routingKey，fanout模式下 routingKey 没用，routingKey是告诉交换机发布到哪个队列，但fanout是发布到所有队列。
            //BasicProperties props：指定消息的一些属性。MessageProperties中包含了一些常用的属性组合。
            //byte[] body：消息内容
            channel.basicPublish("logs", "", null, "Hello Fanout!".getBytes());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
