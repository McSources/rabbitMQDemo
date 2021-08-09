package org.example._08federation;

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
    /**
     * 使用自带的直连交换机
     */
    private static final String EXCHANGE = "amq.direct";

    public static void main(String[] args) throws IOException {
        //创建ConnectionFactory
        ConnectionFactory factory = new ConnectionFactory();
        //从配置文件中读取连接配置，包括 username、password等
        factory.load("classpath:/client.properties");
//        factory.setHost("172.22.58.105");
        factory.setVirtualHost("/");
        //创建连接，用连接创建通道
        try (Connection connection = factory.newConnection(); Channel channel = connection.createChannel()) {
            //发布消息到指定的交换机
            //String exchange：指定交换机
            //String routingKey：routingKey，指定发布到哪个队列
            //BasicProperties props：指定消息的一些属性。MessageProperties中包含了一些常用的属性组合。
            //byte[] body：消息内容
            channel.basicPublish(EXCHANGE, "rk", null, "Federation fuck!".getBytes());
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
