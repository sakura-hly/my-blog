package com.moon.helloworld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class DelayProducer {
    private static final String IP_ADDRESS = "127.0.0.1";
    private static final int PORT = 5672;

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost(IP_ADDRESS);
        factory.setPort(PORT);
        factory.setUsername("guest");
        factory.setPassword("guest");

        Connection connection = factory.newConnection(); // 创建连接
        Channel channel = connection.createChannel(); // 创建通道

        channel.exchangeDeclare("exchange.normal", "fanout", true);
        channel.exchangeDeclare("exchange.dlx", "direct", true);
        Map<String, Object> arg = new HashMap<>();
        arg.put("x-message-ttl", 10000);
        arg.put("x-dead-letter-exchange", "exchange.dlx");
        arg.put("x-dead-letter-routing-key", "routingkey");

        channel.queueDeclare("queue.normal", true, false, false, arg);
        channel.queueBind("queue.normal", "exchange.normal", "");

        channel.queueDeclare("queue.dlx", true, false, false, null);
        channel.queueBind("queue.dlx", "exchange.dlx", "routingkey");

        channel.basicPublish("exchange.normal", "rk", MessageProperties.PERSISTENT_TEXT_PLAIN, "dlx".getBytes());

        // 关闭资源
        channel.close();
        connection.close();
    }
}
