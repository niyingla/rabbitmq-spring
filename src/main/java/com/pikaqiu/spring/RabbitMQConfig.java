package com.pikaqiu.spring;


import com.pikaqiu.spring.adapter.MessageDelegate;
import com.pikaqiu.spring.convert.ImageMessageConverter;
import com.pikaqiu.spring.convert.PDFMessageConverter;
import com.pikaqiu.spring.convert.TextMessageConvert;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.amqp.support.converter.ContentTypeDelegatingMessageConverter;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

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

        //必须设置为true 不然spring容器不会加载
        rabbitAdmin.setAutoStartup(true);

        return rabbitAdmin;

    }

    /**
     * 针对消费者配置
     * 1. 设置交换机类型
     * 2. 将队列绑定到交换机
     * FanoutExchange: 将消息分发到所有的绑定队列，无routingkey的概念
     * HeadersExchange ：通过添加属性key-value匹配
     * DirectExchange:按照routingkey分发到指定队列
     * TopicExchange:多关键字匹配
     */

    //第三种定义方式 直接在容器中创建bean 初始化加载的时候会根据类型去获取 exchange queue binding 然后进去初始化
    @Bean
    public TopicExchange exchange001() {
        return new TopicExchange("topic001", true, false);
    }

    @Bean
    public Queue queue001() {
        return new Queue("queue001", true); //队列持久
    }

    @Bean
    public Binding binding001() {
        return BindingBuilder.bind(queue001()).to(exchange001()).with("spring.*");
    }

    //队列二
    @Bean
    public TopicExchange exchange002() {
        return new TopicExchange("topic002", true, false);
    }

    @Bean
    public Queue queue002() {
        return new Queue("queue002", true); //队列持久
    }

    @Bean
    public Binding binding002() {
        return BindingBuilder.bind(queue002()).to(exchange002()).with("rabbit.*");
    }


    //队列三 和exchange001绑定关系  接受mq.*的路由key
    @Bean
    public Queue queue003() {
        return new Queue("queue003", true); //队列持久
    }

    @Bean
    public Binding binding003() {
        return BindingBuilder.bind(queue003()).to(exchange001()).with("mq.*");
    }

    @Bean
    public Queue queue_image() {
        return new Queue("image_queue", true); //队列持久
    }

    @Bean
    public Queue queue_pdf() {
        return new Queue("pdf_queue", true); //队列持久
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {

        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);

        return rabbitTemplate;
    }
    @Bean
    public SimpleMessageListenerContainer messageContainer(ConnectionFactory connectionFactory) {

        SimpleMessageListenerContainer container = new SimpleMessageListenerContainer(connectionFactory);
        container.setQueues(queue001(), queue002(), queue003(), queue_image(), queue_pdf());
        container.setConcurrentConsumers(1);
        container.setMaxConcurrentConsumers(5);
        container.setDefaultRequeueRejected(false);
        container.setAcknowledgeMode(AcknowledgeMode.AUTO);
        container.setExposeListenerChannel(true);
        container.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {
                return queue + "_" + UUID.randomUUID().toString();
            }
        });
        /**
         container.setMessageListener(new ChannelAwareMessageListener() {
        @Override
        public void onMessage(Message message, Channel channel) throws Exception {
        String msg = new String(message.getBody());
        System.err.println("----------消费者: " + msg);
        }
        });
         */

        /**
         * 1 适配器方式. 默认是有自己的方法名字的：handleMessage
         // 可以自己指定一个方法的名字: consumeMessage
         // 也可以添加一个转换器: 从字节数组转换为String
         MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
         adapter.setDefaultListenerMethod("consumeMessage");
         adapter.setMessageConverter(new TextMessageConverter());
         container.setMessageListener(adapter);
         */

        /**
         * 2 适配器方式: 我们的队列名称 和 方法名称 也可以进行一一的匹配
         *
         MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
         adapter.setMessageConverter(new TextMessageConverter());
         Map<String, String> queueOrTagToMethodName = new HashMap<>();
         queueOrTagToMethodName.put("queue001", "method1");
         queueOrTagToMethodName.put("queue002", "method2");
         adapter.setQueueOrTagToMethodName(queueOrTagToMethodName);
         container.setMessageListener(adapter);
         */

        // 1.1 支持json格式的转换器
        /*MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        //指定消费消息方法
        adapter.setDefaultListenerMethod("consumeMessage");
        //创建json转换类
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        adapter.setMessageConverter(jackson2JsonMessageConverter);

        container.setMessageListener(adapter);*/



        // 1.2 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象转换
        //            创建消息监听适配器                             并制定消息处理类
        /*MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());

        //指定消费消息方法
        adapter.setDefaultListenerMethod("consumeMessage");

        //创建json转换类
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        //创建java映射类
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);

        //设置rabbitmq 信任package
        javaTypeMapper.setTrustedPackages("*");

        adapter.setMessageConverter(jackson2JsonMessageConverter);

        container.setMessageListener(adapter);*/


        //1.3 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象多映射转换
        /*
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        //创建标签和类的映射map
        Map<String, Class<?>> idClassMapping = new HashMap<>(2);
        //order 标签绑定 Order类  发送时设置标签 "__TypeId__", "order"
        idClassMapping.put("order", com.pikaqiu.spring.entity.Order.class);
        idClassMapping.put("packaged", com.pikaqiu.spring.entity.Packaged.class);
        //放入
        javaTypeMapper.setIdClassMapping(idClassMapping);

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);*/


        //1.4 ext convert 多类型转换器 根据设置的contenType找到对应的转换器转换

        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");

        //全局的转换器:
		ContentTypeDelegatingMessageConverter convert = new ContentTypeDelegatingMessageConverter();

        TextMessageConvert textConvert = new TextMessageConvert();
		convert.addDelegate("text", textConvert);
		convert.addDelegate("html/text", textConvert);
		convert.addDelegate("xml/text", textConvert);
		convert.addDelegate("text/plain", textConvert);

		Jackson2JsonMessageConverter jsonConvert = new Jackson2JsonMessageConverter();
		convert.addDelegate("json", jsonConvert);
		convert.addDelegate("application/json", jsonConvert);

		ImageMessageConverter imageConverter = new ImageMessageConverter();
		convert.addDelegate("image/png", imageConverter);
		convert.addDelegate("image", imageConverter);

		PDFMessageConverter pdfConverter = new PDFMessageConverter();
		convert.addDelegate("application/pdf", pdfConverter);

        adapter.setMessageConverter(convert);
        container.setMessageListener(adapter);

        return container;

    }
}
