package com.moon.helloworld;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class RabbitConsumer {

    private static final String QUEUE_NAME = "queue_demo";
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
                System.out.println(" recv message: " + new String(body));
                try {
                    TimeUnit.SECONDS.sleep(1);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                channel.basicAck(envelope.getDeliveryTag(), false);
            }
        };

//        channel.basicConsume(QUEUE_NAME, consumer);

        GetResponse response = channel.basicGet(QUEUE_NAME, false);
        System.out.println(new String(response.getBody()));
        channel.basicAck(response.getEnvelope().getDeliveryTag(), false);
        // 等待回调函数执行完毕之后,关闭资源
        try {
            TimeUnit.SECONDS.sleep(5);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
        connection.close();
    }
}
