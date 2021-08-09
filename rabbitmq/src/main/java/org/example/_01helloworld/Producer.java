package org.example._01helloworld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
            //声明一个队列，如果当前队列不存在则自动创建。
            //String queue：队列的名字
            //boolean durable：队列是否持久化。服务器重启，true时，队列还在，消息没了；false时，队列没了。
            //boolean exclusive：队列是否独占。true时，只能当前channel使用，
            //boolean autoDelete：队列是否自动删除。true时，队列发布过消息且消息全部被消费且消费者断开连接后会自动删除队列。
            //Map<String, Object> arguments：队列的其他参数。
            channel.queueDeclare("msg", true, false, true, null);
            //发布消息
            //String exchange：指定交换机，“”使用默认的交换机
            //String routingKey：routingKey，将根据routingKey确定发布到哪个队列，默认交换机时等同于队列名
            //BasicProperties props：指定消息的一些属性。MessageProperties中包含了一些常用的属性组合。
            //byte[] body：消息内容
            channel.basicPublish(null, "msg", null, "Hello RabbitMQ!".getBytes(StandardCharsets.UTF_8));
        } catch (IOException | TimeoutException e) {
            e.printStackTrace();
        }
    }
}
