package com.huang.rabbitmq.confirm;

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

    private final static String QUEUE_NAME = "queue";
    private final static String EXCHANGE_NAME = "exchange";

    public static void main(String[] args) throws IOException, TimeoutException {
        ConnectionFactory factory = new ConnectionFactory();
        factory.setHost("localhost");
        Connection connection = factory.newConnection();
        Channel channel = connection.createChannel();
        channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);
        channel.queueDeclare(QUEUE_NAME, true, false, false, null);
        channel.queueBind(QUEUE_NAME, EXCHANGE_NAME, "huang");

        // 开启发送方确认模式
        channel.confirmSelect();
        String message = "Hello World!";
        //MessageProperties.PERSISTENT_TEXT_PLAIN: 消息持久化
        channel.basicPublish(EXCHANGE_NAME, "huang", MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));
        System.out.println(" [x] Sent'" + message + "'");
        //异步监听确认和未确认的消息
        channel.addConfirmListener(new ConfirmListener() {
            //消息正确到达broker
            @Override
            public void handleAck(long deliveryTag, boolean multiple) throws IOException {
                System.out.println("已收到消息");
                System.out.println(String.format("已确认消息，标识：%d，多个消息：%b", deliveryTag, multiple));
            }

            //RabbitMQ因为自身内部错误导致消息丢失，就会发送一条nack消息
            @Override
            public void handleNack(long deliveryTag, boolean multiple) throws IOException {
                System.out.println("未确认消息，标识：" + deliveryTag);
            }
        });
//        channel.close();
//        connection.close();
    }
}
