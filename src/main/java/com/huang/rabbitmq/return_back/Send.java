package com.huang.rabbitmq.return_back;

import com.rabbitmq.client.*;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.TimeoutException;

/**
 * 消息发送者
 *
 * @author 黄世增
 */

public class Send {

    private final static String EXCHANGE_NAME = "exchange";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        //交换机，分发到所有与它绑定的队列中
        channel.exchangeDeclare(EXCHANGE_NAME, "topic");
        channel.addReturnListener(new ReturnListener() {
            //当前的 exchange 不存在或者指定的 routingkey 路由不到
            @Override
            public void handleReturn(int replyCode, String replyText, String exchange, String routingKey, AMQP.BasicProperties properties, byte[] body) throws IOException {
                System.out.println("-------return----------");
                System.out.println("replyCode:" + replyCode);
                System.out.println("replyText:" + replyText);
                System.out.println("exchange:" + exchange);
                System.out.println("routingKey:" + routingKey);
                System.out.println("properties:" + properties);
                System.out.println("body:" + new String(body));
            }
        });

        String message = "Hello World!";
        //MessageProperties.PERSISTENT_TEXT_PLAIN: 消息持久化
        channel.basicPublish(EXCHANGE_NAME, "cn.huang", true, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        System.out.println(" [x] Sent'" + message + "'");
//        channel.close();
//        connection.close();
    }
}
