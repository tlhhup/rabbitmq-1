package com.rabbitmq.fount;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import com.rabbitmq.client.AMQP;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.DefaultConsumer;
import com.rabbitmq.client.Envelope;

public class ReceiveMessage {

	public static void main(String[] args) {
		for (int i = 0; i < 5; i++) {
			new Thread(new ReceiverClient()).start();
		}
	}

	public static class ReceiverClient implements Runnable {

		@Override
		public void run() {
			Connection conn = null;
			try (InputStream is = ReceiveMessage.class.getClassLoader().getResourceAsStream("rabbitmq.properties")) {
				Properties properties = new Properties();
				properties.load(is);
				String userName = properties.getProperty("userName");
				String password = properties.getProperty("password");
				String hostName = properties.getProperty("hostName");
				int portNumber = Integer.valueOf(properties.getProperty("portNumber"));
				String exchangeName = properties.getProperty("exchangeName");
				// 创建工厂
				ConnectionFactory factory = new ConnectionFactory();
				factory.setHost(hostName);
				factory.setUsername(userName);
				factory.setPassword(password);
				// factory.setVirtualHost(virtualHost);
				factory.setPort(portNumber);
				// 获取链接
				conn = factory.newConnection();

				// 创建通道
				Channel channel = conn.createChannel();

				// 创建交换中心及消息队列
				channel.exchangeDeclare(exchangeName, "fanout");
				String queueName = channel.queueDeclare().getQueue();
				// 使用指定的路由key将通道绑定到交换中心
				channel.queueBind(queueName, exchangeName, "");

				// 接受数据
				channel.basicConsume(queueName, true, "myConsumerTag", new DefaultConsumer(channel) {
					@Override
					public void handleDelivery(String consumerTag, Envelope envelope, AMQP.BasicProperties properties,
							byte[] body) throws IOException {
						String message = new String(body);
						System.out.println("收到的消息为：" + message);
					}
				});
			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

}
