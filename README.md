# Rabbitmq
RabbitMQ是一个开源的AMQP实现，服务器端用Erlang语言编写，支持多种客户端，如：Python、Ruby、.NET、Java、JMS、C、PHP、ActionScript、XMPP、STOMP等，支持AJAX。用于在分布式系统中存储转发消息，在易用性、扩展性、高可用性等方面表现不俗。

AMQP，即Advanced Message Queuing Protocol，高级消息队列协议，是应用层协议的一个开放标准，为面向消息的中间件设计。消息中间件主要用于组件之间的解耦，消息的发送者无需知道消息使用者的存在，反之亦然。


![模型](http://i.imgur.com/7VPA1Fh.png)

## 安装
1. 首先下载安装erlang:http//www.erlang.org/
2. 在安装rabbitmq:http://www.rabbitmq.com/
	1. 启动web管理器：执行命令rabbitmq-plugins enable rabbitmq_management(注意在sbin目录下执行)
    2. The web UI is located at: http://server-name:15672/
    3. The HTTP API and its documentation are both located at: http://server-name:15672/api/

## 使用
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
	3. exchange的三种模式(在webui管理器中可以查询到)
		1. **direct exchange** 发送消息是要看routingKey的。举个例子，定义了一个direct exchange 名字是X1，然后一个queue名字为Q1 用routingKey=K1 绑定到exchange X1上，当一个routeKey为 K2 的消息到达X1上，那么只有**K1=K2**的时候，这个消息才能到达Q1上。**路由值相等**

			![](http://i.imgur.com/bvMe0mV.png)
		2. **fanout类型的exchange**就比较好理解。就是简单的**广播**，而且是忽略routingKey的。所以只要是有queue绑定到fanout exchange上，通过这个exchange发送的消息都会被发送到那些绑定的queue中，不管你有没有输入routingKey。
		3. **Topic类型的exchange**给与我们更大的灵活性。通过定义routingKey可以有选择的订阅某些消息，此时routingKey就会是一个表达式。exchange会通过**匹配绑定的routingKey**来决定是否要把消息放入对应的队列中。有两种表达式符号可以让我们选择：#和*。
			1. `*`（星号）：代表任意的一个词。 例：*.a会匹配a.a，b.a，c.a等
			2. #（井号）：代码任意的0个或多个词。 例：#.a会匹配a.a，aa.a，aaa.a等

			![](http://i.imgur.com/g8DxhHJ.png)
	4. **注意事项**
		1. 使用不同类型的exchange时需要通过webui中得到对应的exchange信息(当然也可以先通过web管理器创建)
				
				//该amq.fanout已经在rabbitmq server中定义好的
				channel.exchangeDeclare("amq.fanout", "fanout",true);
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
2. 默认用户guest、guest只能方法localhost的主机
##
###和spring整合
1. 重要的类
	1. Message : Spring AMQP定义的Message类是AMQP域模型中代表之一。Message类封装了body(消息BODY)和properties（消息属性） 
	2. MessageProperties类中定义了例如messageId、timestamp、contentType等属性。这此属性可以扩展到用户通过setHeader(String key, Object value)方法来自定义“headers”。
	2. Exchange接口代表一个AMQP的Exchange，决定消息生产者发送消息。每个Exchange都包括一个特定的唯一名字的虚拟主机的代理和一些其他属性。
	3. Queue类是消息消费者接收消息中重要的一个组成部分。通过与Exchange判定来肯定消费者所接收的消息
	4. Bingding类通过多种构造参数来判定Exchange,Queue,routingkey
	5. AmqpTemplate是用来发送消息的模板类 
	6. AmqpAdmin和RabbitAdmin:用户配置Queue、Exchange、Binding的代理类。代理类会自动声明或创建这些配置信息。
	7. 异步接收消息的处理类
		1. MessageConverter 消息转换器类
		2. SimpleMessageListenerContainer 监听消息容器类
3. 整合
	1. jar包：amqp-client、spring-rabbit、spring-amqp
	2. 在rabbitmq的配置文件中配置以下信息
		1. 采用bean的方式配置
			1. 配置连接：connectionFactory-->CachingConnectionFactory工厂bean
			2. 配置rabbitTemplate 消息模板类：---->RabbitTemplate类
			3. 配置rabbitAdmin代理--->RabbitAdmin
			4. 配置消息队列：Queue
		3. 采用spring提供的rabbitmq的文档声明

				<rabbit:connection-factory id="connectionFactory" username="guest" password="guest" host="localhost" channel-cache-size="25" />
				<rabbit:template id="amqpTemplate" connection-factory="connectionFactory" />
				<rabbit:admin connection-factory="connectionFactory" />
				<rabbit:queue name="examplus.answersheetToScoreMaker" />
		4. 发送消息
			1. amqpTemplate.convertAndSend
		2. 获取消息
			1. 同步
				1. amqpTemplate.receiveAndConvert()  
				2. 使用传统的方式从consumer中获取  
			3. 异步：在接收者配置文件中通过消息监听器获取消息并处理消息
				1. 配置消息的处理器类

						 <bean id="receiveHandler" class="cn.slimsmart.rabbitmq.demo.spring.async.ReceiveMsgHandler"></bean> 
				2. 配置消息转换器

						<bean id="messageConverter" class="org.springframework.amqp.support.converter.SimpleMessageConverter"> </bean> 
				3. 配置消息适配器

						<bean id="receiveListenerAdapter"    
						    class="org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter">    
						    <constructor-arg ref="receiveHandler" />    
						    <property name="defaultListenerMethod" value="handleMessage"></property>    
						    <property name="messageConverter" ref="messageConverter"></property>    
						</bean>  
				4. 配置消息监听器

						<bean id="listenerContainer"    
						    class="org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer">    
						    <property name="queueNames" value="${rabbit.queue}"></property>   
						    <property name="connectionFactory" ref="rabbitConnectionFactory"></property>    
						    <property name="messageListener" ref="receiveListenerAdapter"></property>    
						</bean>

