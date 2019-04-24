package com.moon.helloworld;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class RabbitProducer {

    private static final String EXCHANGE_NAME = "exchange_demo";
    private static final String ROUTING_KEY = "routingkey_demo";
    private static final String QUEUE_NAME = "queue_demo";
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

        Map<String, Object> arg = new HashMap<>();
        arg.put("alternate-exchange", "myAe");

        channel.exchangeDeclare("norma1Exchange", "direct", true, false, arg);
        channel.exchangeDeclare("myAe", "fanout", true, false, null);
        channel.queueDeclare("norma1Queue",true,false,false,null);
        channel.queueBind("norma1Queue","norma1Exchange","norma1Key");
        channel.queueDeclare("unroutedQueue",true,false,false,null);
        channel.queueBind("unroutedQueue","myAe","");

        // 创建一个type为direct、持久化、非自动删除的交换器
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", true, false, null);

        // 创建一个持久化、非排他、非自动删除的的队列
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);

        // 将交换器和队列通过路由键绑定
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, ROUTING_KEY);

        // 发送一条持久化的消息: hello world!
        String message = "hello world!";
        channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, true, true, MessageProperties.PERSISTENT_TEXT_PLAIN,
                message.getBytes());
        channel.addReturnListener(new ReturnListener() {
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
                String message = new String(body);
                System.out.println("Basic.Return 返回的结果是: " + message);
            }
        });

        // 关闭资源
        channel.close();
        connection.close();
    }
}
