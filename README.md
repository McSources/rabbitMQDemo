# RabbitMQ

[TOC]

RabbitMQ的基本结构：

![20190610225910220](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/20190610225910220.png)

组成部分说明：

-   Broker：消息队列服务进程，RabbitMQ Server 就是 Message Broker。此进程包括两个部分：Exchange和Queue
-   Virtual host：出于多租户和安全因素设计的，把 AMQP 的基本组件划分到一个虚拟的分组中，类似 于网络中的 namespace 概念。当多个不同的用户使用同一个 RabbitMQ server 提供的服务时，可以划分出 多个 vhost，每个用户在自己的 vhost 创建 exchange／queue 等
-   Exchange：消息队列交换机，按一定的规则将消息路由转发到某个队列，对消息进行过虑。direct (point-to-point), topic (publish-subscribe) and fanout  (multicast)
-   Queue：消息队列，存储消息的队列，消息到达队列并转发给指定的
-   Connection：publisher／consumer 和 broker 之间的 TCP 连接
-   Channel：如果每一次访问 RabbitMQ 都建立一个 Connection，在消息量大的时候建立 TCP  Connection 的开销将是巨大的，效率也较低。Channel 是在 connection 内部建立的逻辑连接，如果应用程序支持多线程，通常每个 thread 创建单独的 channel 进行通讯，AMQP method 包含了 channel id 帮助客户端和 message broker 识别 channel，所以 channel 之间是完全隔离的。Channel 作为轻量级的 Connection 极大减少了操作系统建立 TCP connection 的开销。
-   Producer：消息生产者，即生产方客户端，生产方客户端将消息发送
-   Consumer：消息消费者，即消费方客户端，接收MQ转发的消息。

生产者发送消息流程：

1. 生产者和Broker建立TCP连接。

2. 生产者和Broker建立通道。

3. 生产者通过通道消息发送给Broker，由Exchange将消息进行转发。

4. Exchange将消息转发到指定的Queue（队列）

消费者接收消息流程：

1. 消费者和Broker建立TCP连接
2. 消费者和Broker建立通道
3. 消费者监听指定的Queue（队列）
4. 当有消息到达Queue时Broker默认将消息推送给消费者。
5. 消费者接收到消息。
6. ack回复

## 7种模式

### 0.需要先知道的

#### 0.我遇到的几种现象

生产者给队列发消息，消息进入队列，ready状态。消费者连接到队列后，队列根据Qos发送消息到消费者缓存。

1. Qos是0的话，表示无限。消息会全部发送到消费者缓存，等待ack。如果自动应答的话，消息进入消费者缓存时，消费者就反馈给队列，队列会删除这条消息；如果手动应答的话，在调用basicAck时，队列才会删除这条消息。

#### 1.交换机

有四种交换机：直接(direct), 主题(topic) ,标题(headers) , 扇出(fanout)

生产者生产的消息从不会直接发送到队列，生产者只能将消息发送到交换机(exchange)，交换机决定将消息发送到哪个或哪些队列。

交换机负责哪些队列通过binding来确定，binding就是交换机和队列的绑定关系，一个虚拟的概念。交换机只能发消息给与自己绑定的队列。

队列要与交换机绑定的时候需要指定一个routingKey，可以认为是队列的名字或别名。生产者发消息给交换机时需要告诉交换机发送到哪些队列，即指定routingKey，交换机就是根据routingKey来确定发给那些队列的。比如：队列msg与交换机ems绑定的routingKey是info，则生产者给ems发消息时指定routingKey为info，那消息就会放到msg队列中。

显示声明的具名队列都会自动绑定到默认交换机上，routingKey取队列名。

在发消息时如果不指定交换机（使用空字符串表示），则使用默认交换机。默认交换机时直接交换机。

#### 1.队列的特性

durable：队列是否持久化。服务器重启，true时，队列还在，消息没了；false时，队列没了。
exclusive：队列是否独占。true时，只能当前channel使用，
autoDelete：队列是否自动删除。true时，消费者断开连接后队列会自动删除，不管队列中是否有消息。

#### 2.消息应答

消费者在接收到消息并且处理该消息之后，告诉 rabbitmq 它已经处理了，rabbitmq 可以把该消息删除了。

有两种模式：自动应答和手动应答。需要在声明消费者的时候确定。

自动应答会在收到消息后立刻给服务器反馈，不管消息是否处理完。

手动应答可以在消费者的回调函数中，通过调用以下方法给服务器反馈。

1. `channel.basicAck(deliveryTag,multiple)`：确认消息已经收到。
2. `channel.basicNack(deliveryTag, multiple, requeue)`：否认收到消息。
3. `channel.basicReject(deliveryTag, requeue)`：拒绝该消息。

deliveryTag：表示当前消息的传送编号，类似快递单号，同一条消息可能会被发送多次，所以需要deliveryTag来表示当次消息发送。

multiple：是否批量确认，true时会自动将当前消息之前未确认的消息一同确认。

requeue：是否重新放回队列。

#### 3. 通道缓存Qos

**通道上允许的未确认消息的最大数量，默认是0，代表无限。**此时，消费者可以接受任意数量的消息到缓存中，然后自己慢慢按序处理。如果开启自动应答，则这些消息都会被从队列中删除，如果此时消费者挂掉了，没处理的消息就丢失了。如果此时队列恰好空了，队列又开启了自动删除，消费者还没消费完消息。如果是手动应答，调用basicAck的消息才会被从队列删除，如果此时消费者挂掉了，没处理的消息会自动重新入队。

如果消费者的Qos是1的话，队列中的消息会被一个一个的分配到消费者通道中，当消费者ark后，才会分配下一个消息。这样配合手动应答，可以实现工作快的消费者多处理消息，即能者多劳。

### 1.简单模式Hello world

![python-one](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/python-one.png)

最简单的使用方式。一个生产者，一个消费者。

### 2.多消费者Work Queue

![python-two](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/python-two.png)

一个队列多个消费者，自动按照轮询的方式消费消息。

通道的Qos是指可以同时发送多少消息到通道中。`channel.basicQos()`

1. 如果消费者通道的Qos是0（无限）的话，队列中的消息会被你一个我一个的（不是你前n个，我后n个这样）平均分配到所有消费者通道中。所有消息都是Unacked状态。

   这种情况下不管消费者消费时间长短，它应该消费哪些消息就会消费哪些消息，并不会出现消费快的消费者消费消息多的情况，因为消息是被一股脑的塞给消费者的，队列中是空的了。

   如果消费者开启自动应答的话，消费者在接收到消息还没处理的时候，服务器就会把消息从队列中删除，并自动发送下一条消息。就会出现以下现象：生产者生产完消息，消息队列清空了，消费者还没处理完消息。如果此时开启了队列的自动删除的话，队列清空后已经自动删除，消费者还没处理完消息。

2. 如果消费者通道的Qos是1的话，队列中的消息会被一个一个的分配到消费者通道中，当消费者ark后，才会分配下一个消息。这样配合手动应答，可以实现工作快的消费者多处理消息，即能者多劳。

### 3.发布订阅模式Fanout

![exchanges](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/exchanges.png)

发布订阅模式需要使用Fanout（广播）交换机。

发布订阅模式是将消息发送到Fanout交换机，Fanout交换机将消息发送到所有跟他绑定的队列中，每个队列都会收到交换机发送的同一条消息。

**注意：Fanout交换机不存储消息，如果队列在生产者发送消息后再绑定到交换机上，队列无法收到绑定之前交换机发送的消息。**

### 4.路由模式routing

![direct-exchange](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/direct-exchange.png)

路由模式是一种特殊的发布订阅模式，需要使用direct（直连）交换机。

生产者将消息发送到direct交换机，临时队列绑定到交换机并指定自己routingKey，交换机根据routingKey发送消息到符合的队列中。

**注意：一个队列可以绑定多次，指定多个routingKey；多个队列可以绑定相同的routingKey**

### 5.主题模式topic

![python-five](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/python-five.png)

主题模式是一种特殊的路由模式，需要使用topic交换机。

topic交换机允许在routingKey中使用通配符。例如：`*.orange`，`rabbit.#`。主题模式的routingKey建议使用.分割多个单词。

-   `*`(star) 只能匹配一个单词，例如：`sun.*`只能匹配`sun.moon`，`sun.star`，不能匹配`sun.star.earth`和`sun`
-   `#`(hash) 匹配多个单词，例如：`sun.#`能匹配`sun.star.earth`，`sun.moon`，`sun.star`。

### 6.RPC模式（不常用）

![python-six](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/python-six.png)

创建一个队列用来存放Request请求，一个队列用来存放请求结果。

服务A请求服务B：

A往请求队列中发一条消息，消息至少要包含两个数据：1.放请求结果的队列 2.请求的唯一id

B消费请求，往结果队列发一条消息，消息至少也要包含两个数据：1.请求结果 2.请求的唯一id

A消费结果，匹配唯一id，确认结果。

代码实现：

[RPCServer](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCServer.java )

[RPCClient](https://github.com/rabbitmq/rabbitmq-tutorials/blob/master/java/RPCClient.java)

### 7.发布确认模式

生产者将信道设置成 confirm 模式，一旦信道进入 confirm 模式，所有在该信道上面发布的消息都将会被指派一个唯一的 ID(从 1 开始)，一旦消息被投递到所有匹配的队列之后，broker 就会发送一个确认给生产者(包含消息的唯一 ID)，这就使得生产者知道消息已经正确到达目的队 列了，如果消息和队列是可持久化的，那么确认消息会在将消息写入磁盘之后发出，broker 回传给生产者的确认消息中 delivery-tag 域包含了确认消息的序列号，此外 broker 也可以设置 basic.ack 的 multiple 域，表示到这个序列号之前的所有消息都已经得到了处理。

有两种使用方式：

1. 同步：调用`channel.waitForConfirms()`同步等待broker返回刚发的几条消息确认结果。true是全部发送成功，false是有消息发送失败。
2. 异步：调用`channel.addConfirmListener(ackCallback, nackCallback)`：通过回调来确定哪些消息发送成功，哪些消息发送失败。

由以上两种方式可以想到三种实现：

1. 利用同步方式一条一条的确认。

   每发送一条消息就调用一次`channel.waitForConfirms()`，等待返回结果，再决定是重新发送还是继续发送下一条。

2. 利用同步方式一批一批的确认。

   每发送多条调用一次`channel.waitForConfirms()`，等待返回结果，但是不知道具体哪条消息没有发送成功，所以需要重新发送整批消息。

3. 利用异步方式确认。

   给channel绑定发送成功的回调和发送失败的回调，当消息发送失败时，将消息放到线程安全的队列中，例如：ConcurrentLinkedQueue。最后将失败队列中的消息重新发送。

## 死信队列

死信是指 queue 中无法被消费的某些消息，比如：

* 消息 TTL 过期
* 队列达到最大长度(队列满了，无法再添加数据到 mq 中)
* 消息被拒绝(basic.reject 或 basic.nack)并且 requeue=false

![image-20210729143117563](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210729143117563.png)

声明队列的时候，通过`x-dead-letter-exchange`指定死信交换机，通过`x-dead-letter-routing-key`指定私信队列的routingKey。

这样当队列中的消息无法被消费时就会自动转移到死信队列中保存，可以通过死信队列的消费者做一些特殊处理。

## 延迟队列

由死信队列延伸出延迟队列，用来实现消息在指定时间之后做一些特殊处理。TTL：time to live。

比如：订单15分钟不支付自动删除、在活动开始前10分钟通知用户做好准备等。

生产者可以在发消息时指定消息的过期时间，消费者也可以在声明自己的队列时通过参数`x-message-ttl`指定队列的过期时间。

如果设置了队列的 TTL 属性，那么一旦消息过期，就会被队列丢弃(如果配置了死信队列被丢到死信队 列中)，而另一种方式，消息即使过期，也不一定会被马上丢弃，因为消息是否过期是在即将投递到消费者 之前判定的，如果当前队列有严重的消息积压情况，则已过期的消息也许还能存活较长时间；另外，还需 要注意的一点是，如果不设置 TTL，表示消息永远不会过期。如果将 TTL 设置为 0，则表示除非此时可以 直接投递该消息到消费者，否则该消息将会被丢弃。

![image-20210729211609599](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210729211609599.png)

由于业务需要创建两个时间不同的延迟队列。两个TTL队列中的消息会在超时后进入死信队列消费，可以在死信消费者做些特殊处理。

但是，这样不够灵活，像会议开始前10分钟，由于我们不知道用户什么时候预约会议室，也就不知道需要多长时间才能处理消息，所以需要动态的设置超时时间。

前面说过给消息设置过期时间，可以实现动态设置过期时间，但是消息的过期时间并不准确，只有在消息要被消费时才会过期。

要实现动态的设置过期时间，并准确的过期，需要rabbitMQ的延时队列插件：`rabbitmq_delayed_message_exchange`([浏览插件目录](https://www.rabbitmq.com/community-plugins.html))。

下载ez文件放到`/usr/lib/rabbitmq/lib/rabbitmq_server-3.9.0/plugins/`，然后执行：

```
rabbitmq-server -detached
# 启用插件
rabbitmq-plugins enable rabbitmq_delayed_message_exchange
# 重启rabbitmq服务
systemctl restart rabbitmq-server
```

插件启用成功后会多一个交换机类型：`x-delayed-message`，用来实现延迟队列。

该交换机支持消息延迟投递机制，消息传递后并不会立即投递到目标队列中，而是存储在 mnesia(一个分布式数据系统)表中，当达到投递时间时，才投递到目标队列中。

![image-20210730131709186](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210730131709186.png)

## 高级发布确认模式SpringBoot

需要配置以下两个参数：

```yml
spring:
  rabbitmq:
    publisher-confirm-type: correlated
    publisher-returns: true
```

`publisher-confirm-type`有三种取值：

* simple：`RabbitTemplate#waitForConfirms()`或`RabbitTemplate#waitForConfirmsOrDie()`做同步确认。
* **correlated：使用回调函数做异步发布确认。**
* none：禁用发布确认，默认值。

`publisher-returns`为true时，提供消息发送到队列的确认；为false时，只提供消息发送到交换机的确认。

> 注意：有个Mandatory参数，他指定消息发送到交换机，但是无法路由到队列时消息如何处理。为true时，将消息返回，为false时，将消息丢弃。`publisher-returns`属性只在Mandatory为true时生效；`ReturnsCallback`也只在Mandatory为true时触发。通过`rabbitTemplate.setMandatory(true)`修改Mandatory的取值。默认是true。

使用回调函数做异步发布确认有两种方式：

1. `RabbitTemplate#setConfirmCallback()`和`rabbitTemplate.setReturnsCallback()`给全局设置回调函数。
2. 发布消息时指定`correlationData`并给`correlationData`绑定回调函数`correlationData.getFuture().addCallback(SUCCESS_CALLBACK,FAILURE_CALLBACK)`来给单次发送设置回调函数。

发布消息有三种情况：

1. 没找到对应的交换机：

   会触发`ConfirmCallback`和`SuccessCallback`。两者都包含错误信息和ack状态信息，ack都是false。

2. 没找到对应的队列：

   消息发送到交换机成功，发送到队列失败，会触发`ConfirmCallback`、`ReturnsCallback`和`SuccessCallback`。`SuccessCallback`只有一个ack状态信息为true。`ReturnsCallback`有消息信息（消息头、消息内容等）和发送队列失败的错误信息。`ConfirmCallback`包括前面两者全部信息。

3. 发送成功：

   会触发`ConfirmCallback`和`SuccessCallback`。都只包含值为true的ack状态信息。

综上，`ConfirmCallback`获取到的信息是最全的，而且触发的机会也最多。因此，使用一个`ConfirmCallback`可以对任何消息发送情况做处理，只是他是全局的，注册进`RabbitTemplate`之后就会一直触发。而且，设置了之后不能再改。

### 备份交换机

有了 mandatory 参数和回退消息，我们获得了对无法投递消息的感知能力，有机会在生产者的消息无法被投递时发现并处理。但有时候，我们并不知道该如何处理这些无法路由的消息，最多打个日志，然后触发报警，再来手动处理。而通过日志来处理这些无法路由的消息是很不优雅的做法，特别是当生产者所在的服务有多台机器的时候，手动复制日志会更加麻烦而且容易出错。而且设置 mandatory 参数会增加生产者的复杂性，需要添加处理这些被退回的消息的逻辑。如果既不想丢失消息，又不想增加生产者的复杂性，该怎么做呢？前面在设置死信队列的文章中，我们提到，可以为队列设置死信交换机来存储那些处理失败的消息，可是这些不可路由消息根本没有机会进入到队列，因此无法使用死信队列来保存消息。在 RabbitMQ 中，有一种**备份交换机**的机制存在，可以很好的应对这个问题。什么是备份交换机呢？备份交换机可以理解为 RabbitMQ 中交换机的“备胎”，当我们为某一个交换机声明一个对应的备份交换机时，就是为它创建一个备胎，当交换机接收到一条不可路由消息时，将会把这条消息转发到备份交换机中，由备份交换机来进行转发和处理，通常**备份交换机的类型为 Fanout** ，这样就能把所有消息都投递到与其绑定的队列中，然后我们在备份交换机下绑定一个队列，这样所有那些原交换机无法被路由的消息，就会都进入这个队列了。当然，我们还可以建立一个报警队列，用独立的消费者来进行监测和报警。

![image-20210730165421734](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210730165421734.png)

## 幂等性问题

### 概念

用户对于同一操作发起的一次请求或者多次请求的结果是一致的，不会因为多次点击而产生了副作用。举个最简单的例子，那就是支付，用户购买商品后支付，支付扣款成功，但是返回结果的时候网络异常，此时钱已经扣了，用户再次点击按钮，此时会进行第二次扣款，返回结果成功，用户查询余额发现多扣钱了，流水记录也变成了两条。在以前的单应用系统中，我们只需要把数据操作放入事务中即可，发生错误立即回滚，但是在响应客户端的时候也有可能出现网络中断或者异常等等。

**会导致消息重复消费**

消费者在消费 MQ 中的消息时，MQ 已把消息发送给消费者，消费者在给 MQ 返回 ack 时网络中断，故 MQ 未收到确认信息，该条消息会重新发给其他的消费者，或者在网络重连后再次发送给该消费者，但 实际上该消费者已成功消费了该条消息，造成消费者消费了重复的消息。

### 解决思路

MQ 消费者的幂等性的解决一般使用全局 ID 或者写个唯一标识比如时间戳 或者 UUID 或者订单消费 者消费 MQ 中的消息也可利用 MQ 的该 id 来判断，或者可按自己的规则生成一个全局唯一 id，每次消费消 息时用该 id 先判断该消息是否已消费过。

#### 消费端的幂等性保障

在海量订单生成的业务高峰期，生产端有可能就会重复发生了消息，这时候消费端就要实现幂等性， 这就意味着我们的消息永远不会被消费多次，即使我们收到了一样的消息。

业界主流的幂等性有两种操作：

1. 唯一 ID+指纹码机制，利用数据库主键去重
2. 利用 redis 的原子性去实现

#### 唯一 ID+指纹码机制

指纹码：我们的一些规则或者时间戳加别的服务给到的唯一信息码，它并不一定是我们系统生成的，基本都是由我们的业务规则拼接而来，但是一定要保证唯一性，然后就利用查询语句进行判断这个 id 是否存在数据库中，优势就是实现简单就一个拼接，然后查询判断是否重复；劣势就是在高并发时，如果是单个数据库就会有写入性能瓶颈当然也可以采用分库分表提升性能，但也不是我们最推荐的方式。

#### Redis 原子性 

利用 redis 执行 setnx 命令，天然具有幂等性。从而实现不重复消费。

## 优先级队列

### 使用场景

在我们系统中有一个订单催付的场景，我们的客户在天猫下的订单,淘宝会及时将订单推送给我们，如果在用户设定的时间内未付款那么就会给用户推送一条短信提醒，很简单的一个功能对吧，但是，tmall 商家对我们来说，肯定是要分大客户和小客户的对吧，比如像苹果，小米这样大商家一年起码能给我们创 造很大的利润，所以理应当然，他们的订单必须得到优先处理，而曾经我们的后端系统是使用 redis 来存 放的定时轮询，大家都知道 redis 只能用 List 做一个简简单单的消息队列，并不能实现一个优先级的场景， 所以订单量大了后采用 RabbitMQ 进行改造和优化,如果发现是大客户的订单给一个相对比较高的优先级， 否则就是默认优先级。

### 注意事项

要让队列实现优先级需要做的事情有如下事情：队列需要设置为优先级队列，消息需要设置消息的优先级，消费者需要等待消息已经发送到队列中才去消费因为，这样才有机会对消息进行排序。

### 使用

设置队列的时候通过`x-max-priority`设置队列的最大优先级。

生产者发消息是设置`priority`，指定消息优先级。

## 惰性队列

### 使用场景

RabbitMQ 从 3.6.0 版本开始引入了惰性队列的概念。惰性队列会尽可能的将消息存入磁盘中，而在消 费者消费到相应的消息时才会被加载到内存中，它的一个重要的设计目标是能够支持更长的队列，即支持 更多的消息存储。当消费者由于各种各样的原因(比如消费者下线、宕机亦或者是由于维护而关闭等)而致 使长时间内不能消费消息造成堆积时，惰性队列就很有必要了。 默认情况下，当生产者将消息发送到 RabbitMQ 的时候，队列中的消息会尽可能的存储在内存之中， 这样可以更加快速的将消息发送给消费者。即使是持久化的消息，在被写入磁盘的同时也会在内存中驻留 一份备份。当 RabbitMQ 需要释放内存的时候，会将内存中的消息换页至磁盘中，这个操作会耗费较长的 时间，也会阻塞队列的操作，进而无法接收新的消息。虽然 RabbitMQ 的开发者们一直在升级相关的算法， 但是效果始终不太理想，尤其是在消息量特别大的时候。

### 设置方式

有两种设置方式：

1. 通过指定队列参数`x-queue-mode`的值为lazy
2. 通过policy模式设置`rabbitmqctl set_policy Lazy "^lazy-queue$" '{"queue-mode":"lazy"}' --apply-to queues`

## RabbitMQ 集群

1. 修改 3 台机器的主机名称

   ```
   vim /etc/hostname 
   ```

2. 配置各个节点的 hosts 文件，让各个节点都能互相识别对方

   ```
   vim /etc/hosts 10.211.55.74 node1 10.211.55.75 node2 10.211.55.76 node3 
   ```

3. 以确保各个节点的 cookie 文件使用的是同一个值 在 node1 上执行远程操作命令

   ```
   scp /var/lib/rabbitmq/.erlang.cookie root@node2:/var/lib/rabbitmq/.erlang.cookie
   scp /var/lib/rabbitmq/.erlang.cookie root@node3:/var/lib/rabbitmq/.erlang.cookie
   ```

4. 启动 RabbitMQ 服务，顺带启动 Erlang 虚拟机和 RbbitMQ 应用服务(在三台节点上分别执行以 下命令) 

   ```
   rabbitmq-server -detached
   ```

5. 在节点 2 执行

   ```
   rabbitmqctl stop_app
   (rabbitmqctl stop 会将 Erlang 虚拟机关闭，rabbitmqctl stop_app 只关闭 RabbitMQ 服务)
   rabbitmqctl reset
   rabbitmqctl join_cluster rabbit@node1
   rabbitmqctl start_app(只启动应用服务)
   ```

6. 在节点 3 执行

   ```
   rabbitmqctl stop_app
   rabbitmqctl reset
   rabbitmqctl join_cluster rabbit@node2
   rabbitmqctl start_app
   ```

7. 集群状态

   ```
   rabbitmqctl cluster_status
   ```

8. 需要重新设置用户

   ```
   创建账号rabbitmqctl add_user admin 123设置用户角色rabbitmqctl set_user_tags admin administrator设置用户权限rabbitmqctl set_permissions -p "/" admin ".*" ".*" ".*"
   ```

9. 解除集群节点(node2 和 node3 机器分别执行)

   ```
   在从节点上执行rabbitmqctl stop_apprabbitmqctl resetrabbitmqctl start_apprabbitmqctl cluster_status在主节点上执行rabbitmqctl forget_cluster_node rabbit@node2
   ```

## 镜像队列

> 注意：
>
> quorum类型的队列应该是可复制队列的默认选择。classic类型的队列的镜像模式会在未来删除。
>
> ![image-20210802123715370](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210802123715370.png)

RabbitMQ的Cluster集群模式一般分为两种，**普通模式和镜像模式**。

普通模式：默认的集群模式，以两个节点（rabbit01、rabbit02）为例来进行说明。对于Queue来说，消息实体只存在于其中一个节点rabbit01（或者rabbit02），rabbit01和rabbit02两个节点仅有相同的元数据，即队列的结构。当消息进入rabbit01节点的Queue后，consumer从rabbit02节点消费时，RabbitMQ会临时在rabbit01、rabbit02间进行消息传输，把A中的消息实体取出并经过B发送给consumer。所以consumer应尽量连接每一个节点，从中取消息。即对于同一个逻辑队列，要在多个节点建立物理Queue。否则无论consumer连rabbit01或rabbit02，出口总在rabbit01，会产生瓶颈。当rabbit01节点故障后，rabbit02节点无法取到rabbit01节点中还未消费的消息实体。如果做了消息持久化，那么得等rabbit01节点恢复，然后才可被消费；如果没有持久化的话，就会产生消息丢失的现象。

镜像模式：将需要消费的队列变为镜像队列，存在于多个节点，这样就可以实现RabbitMQ的HA高可用性。作用就是消息实体会主动在镜像节点之间实现同步，而不是像普通模式那样，在consumer消费数据时临时读取。缺点就是，集群内部的同步通讯会占用大量的网络带宽。

通过添加policy实现。

```
rabbitmqctl set_policy [-p Vhost] Name Pattern Definition [Priority]-p Vhost： 可选参数，针对指定vhost下的queue进行设置Name: policy的名称Pattern: queue的匹配模式(正则表达式)Definition：镜像定义，包括三个部分ha-mode, ha-params, ha-sync-mode	ha-mode:指明镜像队列的模式，有效值为 all/exactly/nodes		all：表示在集群中所有的节点上进行镜像		exactly：表示在指定个数的节点上进行镜像，节点的个数由ha-params指定		nodes：表示在指定的节点上进行镜像，节点名称通过ha-params指定	ha-params：ha-mode模式需要用到的参数	ha-sync-mode：进行队列中消息的同步方式，有效值为automatic和manualpriority：可选参数，policy的优先级# 示例：队列名称以“queue_”开头的所有队列进行镜像，并在集群的两个节点上完成进行rabbitmqctl set_policy --priority 0 --apply-to queues mirror_queue "^queue_" '{"ha-mode":"exactly","ha-params":2,"ha-sync-mode":"automatic"}'
```

镜像队列的模式说明：

| ha-mode | ha-params | 功能                                                         |
| ------- | --------- | ------------------------------------------------------------ |
| all     | 空        | 镜像队列将会在整个集群中复制。当一个新的节点加入后，也会在这 个节点上复制一份。 |
| exactly | count     | 镜像队列将会在集群上复制count份。如果集群数量少于count时候，队列会复制到所有节点上。如果大于Count集群，有一个节点crash后，新进入节点也不会做新的镜像。 |
| nodes   | node name | 镜像队列会在node name中复制。如果这个名称不是集群中的一个，这不会触发错误。如果在这个node list中没有一个节点在线，那么这个queue会被声明在client连接的节点。 |

对应的界面：

![image-20210801152159079](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210801152159079.png)

镜像队列会保持队列的数量，如果集群中有3个节点，镜像配置的节点个数为2，则当一个节点挂掉了，会自动再其他可用节点上再创建一个镜像。

## Haproxy+Keepalive 实现高可用负载均衡

生产者一次只能连接一个rabbit服务器，即使是集群也会导致生产者无法发布消息，因为生产者没法知道集群中其他节点的地址。

![image-20210802114451032](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210802114451032.png)

### Haproxy 实现负载均衡 

HAProxy 提供高可用性、负载均衡及基于 TCPHTTP 应用的代理，支持虚拟主机，它是免费、快速并 且可靠的一种解决方案，包括 Twitter,Reddit,StackOverflow,GitHub 在内的多家知名互联网公司在使用。 HAProxy 实现了一种事件驱动、单一进程模型，此模型支持非常大的井发连接数。

*扩展 nginx,lvs,haproxy 之间的区别: http://www.ha97.com/5646.html*

#### 搭建步骤

1. 下载 haproxy(在 node1 和 node2)

   ```
    yum -y install haproxy 
   ```

2. 修改 node1 和 node2 的 haproxy.cfg

   ```
   vim /etc/haproxy/haproxy.cfg
   ```

   需要修改红色 IP 为当前机器 IP

   ![image-20210802114847788](https://machao-pictures.oss-cn-beijing.aliyuncs.com/img/image-20210802114847788.png)

3. 在两台节点启动 haproxy

   ```
   haproxy -f /etc/haproxy/haproxy.cfgps -ef | grep haproxy
   ```

4. 访问地址 http://10.211.55.71:8888/stats

### Keepalived 实现双机(主备)热备

上面搭建了一台Haproxy 主机，如果Haproxy 挂掉的话，rabbitmq集群就算能用也访问不到了。

这里就要引入 Keepalived 它能够通过自身健康检查、资源接管功能做高可用(双机热备)，实现故障转移

#### 搭建步骤

1. 下载 keepalived

  ```
yum -y install keepalived
  ```

2. 节点 node1 配置文件

  ```
vim /etc/keepalived/keepalived.conf
  ```

  把资料里面的 keepalived.conf 修改之后替换

3. 节点 node2 配置文件
   需要修改 global_defs 的 router_id,如:nodeB
     其次要修改 vrrp_instance_VI 中 state 为"BACKUP"；
     最后要将 priority 设置为小于 100 的值

4. 添加 haproxy_chk.sh
   (为了防止 HAProxy 服务挂掉之后 Keepalived 还在正常工作而没有切换到 Backup 上，所以
     这里需要编写一个脚本来检测 HAProxy 务的状态,当 HAProxy 服务挂掉之后该脚本会自动重启
     HAProxy 的服务，如果不成功则关闭 Keepalived 服务，这样便可以切换到 Backup 继续工作)

  ```
vim /etc/keepalived/haproxy_chk.sh #(可以直接上传文件)chmod 777 /etc/keepalived/haproxy_chk.sh #修改权限 
  ```

5. 启动 keepalive 命令(node1 和 node2 启动)

  ```
systemctl start keepalived
  ```

6. 观察 Keepalived 的日志

  ```
tail -f /var/log/messages -n 200
  ```

7. 观察最新添加的 vip

  ```
ip add show
  ```

8. node1 模拟 keepalived 关闭状态

  ```
systemctl stop keepalived 
  ```

9. 使用 vip 地址来访问 rabbitmq 集群

## Federation

在rabbitmq的分布式集群中，我们都是通过配置集群的模式进行分布式部署的，一般都是在内网中使用客户端进行连接调用，但是如果我们遇到大型的分布式集群的时候，比如一个部署在南方，一个部署在北方，然而rabbitmq集群只是部署在了南方，如果北方的分布式程序要来调用rabbitmq集群，那么就只能通过网络来进行远程调用了，在这个过程中我们是不能保证网络的状态的，因此rabbitmq也考虑到了这个因素，因此也就有了federation插件的诞生，它主要解决了以下两个问题：

1. 针对不同的erlang版本和rabbitmq版本，只要都是采用的AMQP 0.9.1作为传输协议都可以进行连接，而不需要建立集群。

2. 针对广域网中的复杂网络环境，针对不在同一个地区的分布式部署，可以采用federation联合的方式进行数据传输。
3. 它也可以在同一台服务器的不同virtual上面进行数交互。

但是我们需要注意的是federation联合的数据在queue中并没有被转移到联合的一方，而是仍然保留在联合的一方，这个后面提到的shovel铲子不一样。

**一句话：Federation无须创建集群就可以将上游upstream的数据同步到下游downstream**

upstream：上游服务器，上游服务器数据会同步给下游服务器。

需要启用以下两个插件：

```
rabbitmq-plugins enable rabbitmq_federationrabbitmq-plugins enable rabbitmq_federation_management
```

### 应用场景

* Federation插件使RabbitMQ在不同Broker节点间进行消息传递而无须建立集群，在不同管理域(不同的用户和vhost、不同版本的RabbitMQ Erlang上)中的Broker或集群间传递消息。
* Federation插件基于AMQP 0-9-1协议在不同的Broker之间通信，能容忍不稳定的网络连接情况
* 一个Broker节点中可以同时存在联邦交换器(或队列)或者本地交换器(或队列)，只需对特定交换器(或队列)创建Federation连接(Federation link)
* Federation插件可以让多个交换器或者多个队列进行联邦
* 一个联邦交换器federated exchange或者一个联邦队列federated queue接收上游upstream的消息，这里的上游指的是其他Broker上的交换器或者队列
* 联邦交换器能够将原本发送给上游交换器的消息路由到本地的某个队列中；联邦队列则允许本地消费者接收来自上游队列的消息

## Shovel

类似联邦，是将原队列的信息同步到目标队列。

需要启用以下两个插件：

```
#启用插件rabbitmq-plugins enable rabbitmq_shovel#启用管理插件rabbitmq-plugins enable rabbitmq_shovel_management
```

