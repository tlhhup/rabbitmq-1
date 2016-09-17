package com.rabbitmq.direct;

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
			String exchangeName=properties.getProperty("directExchangeName");
			String routingKey=properties.getProperty("routingKey");
			//��������
			ConnectionFactory factory = new ConnectionFactory();
			factory.setHost(hostName);
			factory.setUsername(userName);
			factory.setPassword(password);
			factory.setPort(portNumber);
			//��ȡ����
			conn = factory.newConnection();
			//����ͨ��
			Channel channel = conn.createChannel();
			
			//�����������ļ���Ϣ����
			channel.exchangeDeclare(exchangeName, "direct", true);
			//����һ���ͻ���-->���ɵ�
			String queueName = channel.queueDeclare().getQueue();
			//��ͨ���󶨵���������
			channel.queueBind(queueName, exchangeName, routingKey);
			
			String message="";
			do{
				System.out.println("���������ݣ�");
				message=scanner.nextLine();
				//��������-->�ȴ���������������
				channel.basicPublish(exchangeName, routingKey, null, message.getBytes());
				System.out.println("�Ƿ�����������ݣ���(y) ��(n)");
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
