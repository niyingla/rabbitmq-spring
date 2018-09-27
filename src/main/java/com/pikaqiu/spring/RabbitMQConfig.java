package com.pikaqiu.spring;


import com.pikaqiu.spring.adapter.MessageDelegate;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.adapter.MessageListenerAdapter;
import org.springframework.amqp.support.ConsumerTagStrategy;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

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
        adapter.setDefaultListenerMethod("consumeMessage");

        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        adapter.setMessageConverter(jackson2JsonMessageConverter);

        container.setMessageListener(adapter);*/



        // 1.2 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象转换
        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");

        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();
        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
        //防止信任报错
        javaTypeMapper.setTrustedPackages("*");

        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);


        //1.3 DefaultJackson2JavaTypeMapper & Jackson2JsonMessageConverter 支持java对象多映射转换
        /*MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");
        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();
        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        Map<String, Class<?>> idClassMapping = new HashMap<String, Class<?>>();
        idClassMapping.put("order", com.pikaqiu.spring.entity.Order.class);
        idClassMapping.put("packaged", com.pikaqiu.spring.entity.Packaged.class);

        javaTypeMapper.setIdClassMapping(idClassMapping);

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);
        adapter.setMessageConverter(jackson2JsonMessageConverter);
        container.setMessageListener(adapter);*/


        //1.4 ext convert

        /*MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());
        adapter.setDefaultListenerMethod("consumeMessage");

        //全局的转换器:
		ContentTypeDelegatingMessageConverter convert = new ContentTypeDelegatingMessageConverter();

		TextMessageConverter textConvert = new TextMessageConverter();
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
		convert.addDelegate("application/pdf", pdfConverter);*/


        //adapter.setMessageConverter(convert);
        container.setMessageListener(adapter);

        return container;

    }


    /*@Bean//消息消费者监听容器
    public SimpleMessageListenerContainer simpleMessageListenerContainer(ConnectionFactory connectionFactory) {

        SimpleMessageListenerContainer listenerContainer = new SimpleMessageListenerContainer(connectionFactory);

        //同时监控多个队列
        listenerContainer.setQueues(queue001(), queue002(), queue003(), queue_image(), queue_pdf());

        //设置当前的消费者数量
        listenerContainer.setConcurrentConsumers(1);

        //设置最大消费者数量
        listenerContainer.setMaxConcurrentConsumers(5);
        //设置重回队列
        listenerContainer.setDefaultRequeueRejected(false);
        //设置签收模式
        listenerContainer.setAcknowledgeMode(AcknowledgeMode.AUTO);
        //是否外漏
        listenerContainer.setExposeListenerChannel(true);
        //设置consumer唯一标签
        listenerContainer.setConsumerTagStrategy(new ConsumerTagStrategy() {
            @Override
            public String createConsumerTag(String queue) {

                return queue + "_" + UUID.randomUUID().toString().replace("-", "");
            }
        });

        //监听方式1
        //消费实例
       *//* listenerContainer.setMessageListener(new ChannelAwareMessageListener() {
            @Override
            public void onMessage(Message message, Channel channel) throws Exception {
                //消息监听器 对消息的消费处理
                String msg = new String(message.getBody());

                System.out.println("---------------" + msg + "---------------");
            }
        });*//*

        //监听方式2
        MessageListenerAdapter messageListenerAdapter = new MessageListenerAdapter(new MessageDelegate());

        //1  默认 自己指定消费方法名的消费方式
        messageListenerAdapter.setDefaultListenerMethod("consumeMessage");

        //设置消息转换器 默认是 字节数组 通过转换器可以转换成其他类型数据
        //messageListenerAdapter.setMessageConverter(new TextMessageConvert());

        //1.1 json格式消息转换器
        *//*Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        //1.2 支持java对象转换
        DefaultJackson2JavaTypeMapper defaultJackson2JavaTypeMapper = new DefaultJackson2JavaTypeMapper();

        jackson2JsonMessageConverter.setJavaTypeMapper(defaultJackson2JavaTypeMapper);

        messageListenerAdapter.setMessageConverter(jackson2JsonMessageConverter);*//*

        MessageListenerAdapter adapter = new MessageListenerAdapter(new MessageDelegate());

        adapter.setDefaultListenerMethod("consumeMessage");

        Jackson2JsonMessageConverter jackson2JsonMessageConverter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper javaTypeMapper = new DefaultJackson2JavaTypeMapper();

        jackson2JsonMessageConverter.setJavaTypeMapper(javaTypeMapper);

        adapter.setMessageConverter(jackson2JsonMessageConverter);

        listenerContainer.setMessageListener(adapter);

        //2  消费方法和queue或者tag对应关系 的消费方式

        //测试的时候可能因为加载顺序  有时候会走默认消费方法
        //但是服务启动时是没有问题的
//        HashMap<String, String> queueToMethod = new HashMap<>();
//
//        queueToMethod.put("queue001", "method1");
//
//        queueToMethod.put("queue002", "method2");
//
//        messageListenerAdapter.setQueueOrTagToMethodName(queueToMethod);

        listenerContainer.setMessageListener(messageListenerAdapter);

        return listenerContainer;
    }*/
}
