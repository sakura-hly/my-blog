package com.moon.helloworld;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

public class RPCServer {
    private static final String RPC_QUEUE_NAME = "rpc_queue";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("127.0.0.1");
        factory.setPort(5672);
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection(); // 创建连接
        Channel channel = connection.createChannel(); // 创建通道

        channel.queueDeclare(RPC_QUEUE_NAME, false, false, false, null);
        channel.basicQos(1);
        System.out.println("[x] Awaiting RPC requests");

        Consumer consumer = new DefaultConsumer(channel) {
            @Override
            public void handleDelivery(String consumerTag,
                                       Envelope envelope,
                                       AMQP.BasicProperties properties,
                                       byte[] body) throws IOException {
                AMQP.BasicProperties replyProps =
                        new AMQP.BasicProperties()
                                .builder()
                                .correlationId(properties.getCorrelationId())
                                .build();
                String response = "";
                try {
                    String message = new String(body, "UTF-8");
                    int i = Integer.valueOf(message);
                    System.out.println("[.] fib(" + message + ")");
                    response += fib(i);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                } finally {
                    channel.basicPublish("", properties.getReplyTo(), replyProps, response.getBytes());
                    channel.basicAck(envelope.getDeliveryTag(), false);
                }

            }
        };

        channel.basicConsume(RPC_QUEUE_NAME, false, consumer);
        try {
            // 需要一点延迟，不然还没有消费消息，程序就退出了
            Thread.sleep(3 * 1000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        channel.close();
        connection.close();
    }

    private static int fib(int n) {
        if (n == 0) return 0;
        if (n == 1) return 1;
        return fib(n - 1) + fib(n - 2);
    }
}
