package com.moon.helloworld;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class DelayConsumer {
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final int PORT = 5672;

    public static void main(String[] args) throws IOException, TimeoutException {
        Address[] addresses = new Address[]{new Address(IP_ADDRESS, PORT)};

        ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection(addresses); // 创建连接
        final Channel channel = connection.createChannel(); // 创建通道
        channel.basicQos(64); // 设置客户端最多接收未被 ack 的消息的个数

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                System.out.println("recv dlx message: " + new String(body));


                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

        channel.basicConsume("queue.dlx", false, consumer);


        try {
            // 需要一点延迟，不然还没有消费消息，程序就退出了
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
        connection.close();
    }
}
