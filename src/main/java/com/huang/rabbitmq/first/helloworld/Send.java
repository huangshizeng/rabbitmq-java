package com.huang.rabbitmq.first.helloworld;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 消息发送者
 *
 * @author 黄世增
 */

public class Send {

    private final static String QUEUE_NAME = "hello world";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //true: 队列持久化
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        String message = "Hello World!";
        //MessageProperties.PERSISTENT_TEXT_PLAIN: 消息持久化
        channel.basicPublish("", QUEUE_NAME, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        System.out.println(" [x] Sent'" + message + "'");
        channel.close();
        connection.close();
    }
}
