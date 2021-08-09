package org.example._04routing;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

/**
 * @author machao
 * @date 2021/7/27
 */
public class ConsumerError {
    private static final String EXCHANGE = "logs_direct";
    private static final String ERROR = "error";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.load("classpath:/client.properties");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        try {
            //声明交换机，交换机不存在的时候会自动创建
            channel.exchangeDeclare(EXCHANGE, BuiltinExchangeType.DIRECT);
            //创建消费者用的临时队列：不持久化，自动删除
            String queue = channel.queueDeclare().getQueue();
            //将队列绑定到交换机
            channel.queueBind(queue, EXCHANGE, ERROR);
            //定义消费者成功接收消息时的回调。
            DeliverCallback deliver = (consumerTag, message) -> {
                System.out.println("ErrorMessage:" + new String(message.getBody()));
                //手动应答。
                //long deliveryTag：当前这次传送的编号，类似快递单号。
                //boolean multiple：是否批量。true时，会自动将当前消息前面没有应答的消息也应答一下。
                channel.basicAck(message.getEnvelope().getDeliveryTag(), false);
            };
            //定义消费者被动取消接收消息时的回调，例如：队列被删除。
            CancelCallback cancel = consumerTag -> System.out.println(consumerTag + " canceled");
            //注册消费者
            //String queue：消费者监听的队列
            //boolean autoAck：是否自动应答。服务器接收到回应之后会自动从队列中移除消息。自动应答就是，在消息接收成功时自动给服务器反馈。
            //DeliverCallback deliverCallback：功接收消息时的回调。
            //CancelCallback cancelCallback：动取消接收消息时的回调
            String consume = channel.basicConsume(queue, false, deliver, cancel);
            //主动放弃接收消息，将消费者移除。
//            channel.basicCancel(consume);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
