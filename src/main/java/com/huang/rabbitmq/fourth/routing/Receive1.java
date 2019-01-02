package com.huang.rabbitmq.fourth.routing;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DeliverCallback;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * @author 黄世增
 */

public class Receive1 {

    private final static String EXCHANGE_NAME = "exchange1";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct");
        //声明一个非持久化、独立、自动删除的队列
        String queueName = channel.queueDeclare().getQueue();
        channel.queueBind(queueName, EXCHANGE_NAME, "key1");
        channel.queueBind(queueName, EXCHANGE_NAME, "key2");

        System.out.println(" [*] Waiting for messages. To exit press CTRL+C");
        //每次从队列中获取数量，告诉服务器，在我当前消息没有确认完成之前不要给我发新的消息
        channel.basicQos(1);
        DeliverCallback deliverCallback = (consumerTag, delivery) -> {
            String message = new String(delivery.getBody(), StandardCharsets.UTF_8);
            System.out.println("Worker1  [x] Received '" + message + "'");
            //模拟处理
            try {
                Thread.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                System.out.println("Worker1  [x] Done");
                //消息处理完后手动消息确认
                channel.basicAck(delivery.getEnvelope().getDeliveryTag(), false);
            }
        };
        channel.basicConsume(queueName, false, deliverCallback, consumerTag -> {
        });
    }
}
