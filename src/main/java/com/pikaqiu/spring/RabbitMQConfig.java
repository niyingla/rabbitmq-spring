package com.pikaqiu.spring;


import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

@Configuration
@ComponentScan({"com.pikaqiu.spring.*"})
public class RabbitMQConfig {

    /**
     * 创建连接工厂类
     *
     * @return
     */

    @Bean
    public ConnectionFactory connectionFactory() {

        CachingConnectionFactory cachingConnectionFactory = new CachingConnectionFactory();

        cachingConnectionFactory.setAddresses("134.175.5.236:5672");

        cachingConnectionFactory.setUsername("guest");

        cachingConnectionFactory.setPassword("guest");

        cachingConnectionFactory.setVirtualHost("/");

        return cachingConnectionFactory;

    }

    @Bean//connectionFactory 要和容器中bean的名字一致才能注入
    public RabbitAdmin rabbitAdmin(ConnectionFactory connectionFactory) {

        RabbitAdmin rabbitAdmin = new RabbitAdmin(connectionFactory);

        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;

    }

}
