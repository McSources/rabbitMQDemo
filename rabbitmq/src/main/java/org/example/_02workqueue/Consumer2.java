package org.example._02workqueue;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author machao
 * @date 2021/7/27
 */
public class Consumer2 {

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.load("classpath:/client.properties");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        try {
            //声明一个队列，如果当前队列不存在则自动创建。该队列的属性必须跟已经声明的队列一致，否则报错。
            channel.queueDeclare("msg", true, false, true, null);
            //定义消费者成功接收消息时的回调。
            DeliverCallback deliver = (consumerTag, message) -> {
                String body = new String(message.getBody());
                System.out.println(consumerTag + " 收到消息 " + body);
                int num = Integer.parseInt(body.substring(body.length() - 1));
                try {
                    Thread.sleep(num % 2 * 5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //手动应答。
                //long deliveryTag：当前这次传送的编号，类似快递单号。
                //boolean multiple：是否批量。true时，会自动将当前消息前面没有应答的消息也应答一下。
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            };
            //定义消费者被动取消接收消息时的回调，例如：队列被删除。
            CancelCallback cancel = consumerTag -> System.out.println(consumerTag + "canceled");
            channel.basicQos(1);
            //注册消费者
            //String queue：消费者监听的队列
            //boolean autoAck：是否自动应答。服务器接收到回应之后会自动从队列中移除消息。自动应答就是，在消息接收成功时自动给服务器反馈。
            //DeliverCallback deliverCallback：功接收消息时的回调。
            //CancelCallback cancelCallback：动取消接收消息时的回调
            String consume = channel.basicConsume("msg", false, deliver, cancel);
            //主动放弃接收消息，将消费者移除。
//            channel.basicCancel(consume);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
