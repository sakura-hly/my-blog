package com.moon.helloworld;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.TimeoutException;

public class RPCClient {
    private static String requestQueueName = "rpc_queue";
    private static String replyQueueName;

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection(); // 创建连接
        Channel channel = connection.createChannel(); // 创建通道

        replyQueueName = channel.queueDeclare().getQueue();
//        String response = null;
        String corrld = UUID.randomUUID().toString();

        AMQP.BasicProperties props = new AMQP.BasicProperties
                .Builder()
                .replyTo(replyQueueName)
                .correlationId(corrld)
                .build();

        System.out.println("[x] Requesting fib(30)");
        channel.basicPublish("", requestQueueName, props, "30".getBytes());

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties, byte[] body) throws IOException {
                if (corrld.equals(properties.getCorrelationId())) {
                    System.out.println("[.] Got '" + new String(body) + "'");
                }
            }
        };
        channel.basicConsume(replyQueueName, false, consumer);

        try {
            Thread.sleep(10 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
        connection.close();
    }
}
