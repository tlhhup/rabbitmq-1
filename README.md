# Rabbitmq
RabbitMQ是一个开源的AMQP实现，服务器端用Erlang语言编写，支持多种客户端，如：Python、Ruby、.NET、Java、JMS、C、PHP、ActionScript、XMPP、STOMP等，支持AJAX。用于在分布式系统中存储转发消息，在易用性、扩展性、高可用性等方面表现不俗。

AMQP，即Advanced Message Queuing Protocol，高级消息队列协议，是应用层协议的一个开放标准，为面向消息的中间件设计。消息中间件主要用于组件之间的解耦，消息的发送者无需知道消息使用者的存在，反之亦然。


![模型](http://i.imgur.com/7VPA1Fh.png)

##
1. 核心对象
	1. Channel 
	2. Connection 
	3. ConnectionFactory 
	4. Consumer：消费者(从消息队列中取出消息进行处理)
	5. Exchanges ：数据交换中心
	6. Queues：消息队列
7. 获取链接连接
	1. 通过参数进行设置

			ConnectionFactory factory = new ConnectionFactory();
			factory.setUsername(userName);
			factory.setPassword(password);
			factory.setVirtualHost(virtualHost);//可以不进行设置
			factory.setHost(hostName);
			factory.setPort(portNumber);
			Connection conn = factory.newConnection();
	2. 通过uri地址创建

			ConnectionFactory factory = new ConnectionFactory();
			factory.setUri("amqp://userName:password@hostName:portNumber/virtualHost");
			Connection conn = factory.newConnection();
3. 创建通道
	 
		Channel channel = conn.createChannel();
4. 创建数据交换中心及队列
	1. 一个客户端

			channel.exchangeDeclare(exchangeName, "direct", true);
			String queueName = channel.queueDeclare().getQueue();
			channel.queueBind(queueName, exchangeName, routingKey);
	2. 多个客户端

			channel.exchangeDeclare(exchangeName, "direct", true);
			channel.queueDeclare(queueName, true, false, false, null);
			channel.queueBind(queueName, exchangeName, routingKey);
	3. exchange的三种模式
		1. **direct exchange** 发送消息是要看routingKey的。举个例子，定义了一个direct exchange 名字是X1，然后一个queue名字为Q1 用routingKey=K1 绑定到exchange X1上，当一个routeKey为 K2 的消息到达X1上，那么只有**K1=K2**的时候，这个消息才能到达Q1上。**路由值相等**
		2. **fanout类型的exchange**就比较好理解。就是简单的**广播**，而且是忽略routingKey的。所以只要是有queue绑定到fanout exchange上，通过这个exchange发送的消息都会被发送到那些绑定的queue中，不管你有没有输入routingKey。
		3. **Topic类型的exchange**给与我们更大的灵活性。通过定义routingKey可以有选择的订阅某些消息，此时routingKey就会是一个表达式。exchange会通过**匹配绑定的routingKey**来决定是否要把消息放入对应的队列中。有两种表达式符号可以让我们选择：#和*。
			1. `*`（星号）：代表任意的一个词。 例：*.a会匹配a.a，b.a，c.a等
			2. #（井号）：代码任意的0个或多个词。 例：#.a会匹配a.a，aa.a，aaa.a等

5. 发送消息
	1. 使用 Channel.basicPublish 方法发送消息
2. 并发问题
	1. 多个线程不能共用一个channel
2. 接受消息数据
	1. 使用Consumer 进行消息的订阅并使用唯一的标识(consumer tags)

			boolean autoAck = false;//通知已经收到消息
			channel.basicConsume(queueName, autoAck, "myConsumerTag",
			     new DefaultConsumer(channel) {
			         @Override
					public void handleDelivery(String consumerTag, Envelope envelope,
							AMQP.BasicProperties properties, byte[] body) throws IOException {
						long deliveryTag = envelope.getDeliveryTag();
						String message=new String(body);
						System.out.println("收到的消息为："+message);
						channel.basicAck(deliveryTag, false);
					}
			     });
		1. 对autoAck的说明

				ack= true: Round-robin 转发   消费者被杀死，消息会丢失 
				ack=false:消息应答 ，为了保证消息永远不会丢失，RabbitMQ支持消息应答（message acknowledgments）。 
				消费者发送应答给RabbitMQ，告诉它信息已经被接收和处理，然后RabbitMQ可以自由的进行信息删除。 
				如果消费者被杀死而没有发送应答，RabbitMQ会认为该信息没有被完全的处理，然后将会重新转发给别的消费者。 
				通过这种方式，你可以确认信息不会被丢失，即使消者偶尔被杀死。 
				消费者需要耗费特别特别长的时间是允许的。
	2. Consumer中的方法
		1. handleDelivery(String, Envelope, BasicProperties, byte[])：接收到数据
		2. handleShutdownSignal(String, ShutdownSignalException)：通道和连接关闭时调用
		3. handleConsumeOk(String)：在任何回调函数执行之前调用
		4. basicCancel(String)：通过制定的consumerTag取消订阅 
	5. 接收特定的消息
		1. 使用Channel.basicGet，获取GetResponse对象(头信息及body)

				boolean autoAck = false;
				GetResponse response = channel.basicGet(queueName, autoAck);
				//接收到数据的标识，标识该消息已经被处理了
				channel.basicAck(deliveryTag, false);
	6. 处理无法通过路由发送的数据(发送数据是设置了mandatory标识,并且为一个direct方式的exchange且没有绑定消息队列)
		1. 通过设置ReturnListener监听器来获取数据

				channel.addReturnListener(new ReturnListener() {
				    public void handleBasicReturn(int replyCode,
				                                  String replyText,
				                                  String exchange,
				                                  String routingKey,
				                                  AMQP.BasicProperties properties,
				                                  byte[] body)
				    throws IOException {
				        ...
				    }
				});

