package com.huang.rabbitmq.fifth.topic;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @author 黄世增
 */

public class Send {

    private final static String EXCHANGE_NAME = "exchange2";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //交换机，分发到所有与它绑定的队列中
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        String message = "Hello World!";
        channel.basicPublish(EXCHANGE_NAME, "cn.huangshizeng", null, message.getBytes(StandardCharsets.UTF_8));
        System.out.println(" [x] Sent'" + message + "'");
        channel.close();
        connection.close();
    }
}
