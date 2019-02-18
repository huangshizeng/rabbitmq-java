# 如何保证RabbitMQ全链路数据100%不丢失
正在学rabbitmq，特此记录一下，这里就不讲rabbitmq基础了，直接进入主题。

我们都知道，消息从生产者到消费者消费要经过3个步骤：

1、生产者发送消息到rabbitmq

2、rabbitmq发送消息到消费者

3、消费者消费这条消息

这3个步骤中的每一步都有可能导致消息丢失，消息丢失不可怕，可怕的是丢失了我们还不知道，
所以要有一些措施来保证系统的可靠性。这里的可靠并不是一定就100%不丢失了，磁盘损坏，机房
爆炸等等都能导致数据丢失，当然这种都是极小概率发生，能做到99.999999%消息不丢失，就是可靠的了。
下面来具体分析一下问题以及解决方案。

## 生产者可靠性投递

生产者可靠性投递，即生产者要确保将消息正确投递到rabbitmq中。生产者投递的消息丢失的原因有很多，
比如消息在网络传输的过程中发生网络故障消息丢失，或者消息投递到rabbitmq时rabbitmq挂了，
那消息也可能丢失，而我们根本不知道发生了什么。针对以上情况，rabbitmq本身提供了一些机制：

事务消息机制

confirm消息确认机制

消息持久化

**事务消息机制**由于会严重降低性能，所以一般不采用这种方法，我就不介绍了，而采用另一种轻量级的解决方案——confirm消息确认机制。

什么是**confirm消息确认机制**？顾名思义，就是生产者投递的消息一旦投递到rabbitmq后，
rabbitmq就会发送一个确认消息给生产者，让生产者知道我已经收到消息了，否则这条消息就可能已经
丢失了，需要生产者重新发送消息了。

通过下面这句代码来开启确认模式：

    channel.confirmSelect();// 开启发送方确认模式

然后异步监听确认和未确认的消息：

    channel.addConfirmListener(new ConfirmListener() {
        //消息正确到达broker
        @Override
        public void handleAck(long deliveryTag, boolean multiple) throws IOException {
            System.out.println("已收到消息");
            //做一些其他处理
        }
    
        //RabbitMQ因为自身内部错误导致消息丢失，就会发送一条nack消息
        @Override
        public void handleNack(long deliveryTag, boolean multiple) throws IOException {
            System.out.println("未确认消息，标识：" + deliveryTag);
            //做一些其他处理，比如消息重发等
        }
    });

这样就可以让生产者感知到消息是否投递到rabbitmq中了，当然这样还不够，稍后我会说一下极端情况。

那**消息持久化**呢？我们知道，rabbitmq收到消息后将这个消息暂时存在了内存中，那
这就会有个问题，如果rabbitmq挂了，那重启后数据就丢失了，所以相关的数据应该
持久化到硬盘中，这样就算rabbitmq重启后也可以到硬盘中取数据恢复。那如何持久化呢？

message消息到达rabbitmq后先是到exchange交换机中，然后路由给queue队列，最后发送给消费者。

所有需要给exchange、queue和message都进行持久化：

exchange持久化：

    //第三个参数true表示这个exchange持久化
    channel.exchangeDeclare(EXCHANGE_NAME, "direct", true);

queue持久化：

    //第二个参数true表示这个queue持久化
    channel.queueDeclare(QUEUE_NAME, true, false, false, null);
    
message持久化：
    
    //第三个参数MessageProperties.PERSISTENT_TEXT_PLAIN表示这条消息持久化
    channel.basicPublish(EXCHANGE_NAME, ROUTING_KEY, MessageProperties.PERSISTENT_TEXT_PLAIN, message.getBytes(StandardCharsets.UTF_8));

这样，如果rabbitmq收到消息后挂了，重启后会自行恢复消息。

到此，rabbitmq提供的几种机制都介绍完了，但这样还不足以保证消息可靠性投递rabbitmq中，上面我也提到了
会有极端情况，比如rabbitmq收到消息还没来得及将消息持久化到硬盘时，rabbitmq挂了，
这样消息还是丢失了，或者rabbitmq在发送确认消息给生产者的过程中，由于网络故障而导致
生产者没有收到确认消息，这样生产者就不知道rabbitmq到底有没有收到消息，就不好做
接下来的处理。所以除了rabbitmq提供的一些机制外，我们自己也要做一些消息补偿机制，
以应对一些极端情况。接下来我就介绍其中的一种解决方案——消息入库。

消息入库，顾名思义就是将要发送的消息保存到数据库中。

首先发送消息前先将消息保存到数据库中，有一个状态字段status=0，表示生产者将
消息发送给了rabbitmq但还没收到确认；在生产者收到确认后将status设为1，表示
rabbitmq已收到消息。这里有可能会出现上面说的两种情况，所以生产者这边开一个
定时器，定时检索消息表，将status=0并且超过固定时间后（可能消息刚发出去还没来得及确认
这边定时器刚好检索到这条status=0的消息，所以给个时间）还没收到确认的消息取出重发（
第二种情况下这里会造成消息重复，消费者端要做幂等性），可能重发还会失败，所以
可以做一个最大重发次数，超过就做另外的处理。

这样消息就可以可靠性投递到rabbitmq中了，而生产者也可以感知到了。

## 消费者数据不丢失

既然已经可以让生产者100%可靠性投递到rabbitmq了，那接下来就改看看消费者的了，
如何让消费者可以正确的消费完消息。

这里我把它分为两个步骤：

1、rabbitmq将消息发送给消费者

2、消费者消费消息

要保证上述两个过程中消息不丢失。

