package com.rabbitmq.fount;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;
import java.util.Scanner;

import com.rabbitmq.client.Channel;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.ConnectionFactory;

public class SendMessage {

	public static void main(String[] args) {
		Connection conn = null;
		try(InputStream is = SendMessage.class.getClassLoader().getResourceAsStream("rabbitmq.properties");Scanner scanner=new Scanner(System.in)) {
			Properties properties=new Properties();
			properties.load(is);
			String userName=properties.getProperty("userName");
			String password=properties.getProperty("password");
			String hostName=properties.getProperty("hostName");
			int portNumber=Integer.valueOf(properties.getProperty("portNumber"));
			String exchangeName=properties.getProperty("fanoutExchangeName");
			//创建工厂
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(hostName);
			factory.setUsername(userName);
			factory.setPassword(password);
			factory.setPort(portNumber);
			//获取链接
			conn = factory.newConnection();
			//创建通道
			Channel channel = conn.createChannel();
			
			//创建交换中心及消息队列-->采用广播的方式发送数据
			channel.exchangeDeclare(exchangeName, "fanout",true);
			
			String message="";
			do{
				System.out.println("请输入数据：");
				message=scanner.nextLine();
				//发送数据-->等待将该数据消费了
				channel.basicPublish(exchangeName, "", null, message.getBytes());
				System.out.println("是否继续发送数据：是(y) 否(n)");
			}while("y".equalsIgnoreCase(message=scanner.nextLine()));
		} catch (Exception e) {
			e.printStackTrace();
		}finally {
			if(conn!=null){
				try {
					conn.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
}
