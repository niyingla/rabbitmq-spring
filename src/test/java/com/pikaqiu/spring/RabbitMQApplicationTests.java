package com.pikaqiu.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pikaqiu.spring.entity.Order;
import com.pikaqiu.spring.entity.Packaged;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitMQApplicationTests {
    /**
     * 消息队列的创建和队列中数据的清空
     */

    @Autowired
    private RabbitAdmin rabbitAdmin;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Test
    public void contextLoads() {

        //-------------------------------------创建--------------------------------------------

        //第一种创建方式
        rabbitAdmin.declareExchange(new DirectExchange("test.direct", false, false));

        rabbitAdmin.declareExchange(new TopicExchange("test.topic", false, false));

        rabbitAdmin.declareExchange(new FanoutExchange("test.fanout", false, false));

        rabbitAdmin.declareQueue(new Queue("test.direct.queue", false));

        rabbitAdmin.declareQueue(new Queue("test.topic.queue", false));

        rabbitAdmin.declareQueue(new Queue("test.fanout.queue", false));
        //一般来说destination就是queue name
        rabbitAdmin.declareBinding(new Binding("test.direct.queue", Binding.DestinationType.QUEUE, "test.direct", "direct", new HashMap<>()));

/*
        rabbitAdmin.declareBinding(new Binding("test.topic.queue", Binding.DestinationType.QUEUE, "test.topic", "direct", new HashMap<>()));

        rabbitAdmin.declareBinding(new Binding("test.fanout.queue", Binding.DestinationType.QUEUE, "test.fanout", "direct", new HashMap<>()));
*/

        //第二种创建方式 支持链式编程
        rabbitAdmin.declareBinding(
                //直接创建队列
                BindingBuilder.bind(new Queue("test.topic.queue", false))
                        //直接创建交换机 建立关系
                        .to(new TopicExchange("test.topic", false, false))
                        //指定路由key
                        .with("user.#"));


        rabbitAdmin.declareBinding(
                //直接创建队列
                BindingBuilder.bind(new Queue("test.fanout.queue", false))
                        //直接创建交换机 建立关系  没有with
                        .to(new FanoutExchange("test.fanout", false, false)));

//    --------------------------------------操作-------------------------------------
        //清空指定队列的数组         队列名称    不等待
        rabbitAdmin.purgeQueue("dlx.queue", false);
    }

    @Test
    public void testSendMessage() throws Exception {

        MessageProperties messageProperties = new MessageProperties();

        messageProperties.getHeaders().put("desc", "消息描述");

        messageProperties.getHeaders().put("type", "自定义消息描述。。。");

        Message message = new Message("这是我的第一个spring消息".getBytes(), messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.amqp", message, new MessagePostProcessor() {

            //发送前进行修改的信息

            @Override
            public Message postProcessMessage(Message message) throws AmqpException {

                System.out.println("---------------添加额外设置-------------");

                message.getMessageProperties().getHeaders().put("desc", "额外修改的描述");

                message.getMessageProperties().getHeaders().put("attr", "额外新加的");

                return message;
            }
        });
    }

    @Test
    public void testSendMessage2() throws Exception {

        /**/
        MessageProperties messageProperties = new MessageProperties();

        messageProperties.setContentType("text/plain");

        messageProperties.getHeaders().put("desc", "消息描述");

        messageProperties.getHeaders().put("type", "自定义消息描述。。。");

        Message message = new Message("mq的消息".getBytes(), messageProperties);

        //直接发送内容
        rabbitTemplate.convertAndSend("topic002", "rabbit.amqp", "你好啊--1");

        Message message1 = new Message("mq的消息--2".getBytes(), messageProperties);

        rabbitTemplate.convertAndSend("topic001", "spring.amqp", message1);

        rabbitTemplate.convertAndSend("topic002", "rabbit.abc", message);

    }

    @Test
    public void testSendMessage4Text() throws Exception {

        /**/
        MessageProperties messageProperties = new MessageProperties();

        messageProperties.setContentType("text/plain");

        Message message = new Message("mq的消息".getBytes(), messageProperties);

        //直接发送内容
        rabbitTemplate.send("topic001", "spring.amqp", message);


        rabbitTemplate.send("topic002", "rabbit.amqp", message);

    }

    @Test
    public void testSendJsonMessage() throws Exception {

        Order order = new Order();
        order.setId("001");
        order.setName("消息订单");
        order.setContent("描述信息");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.err.println("order 4 json: " + json);

        MessageProperties messageProperties = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties.setContentType("application/json");
        Message message = new Message(json.getBytes(), messageProperties);

        rabbitTemplate.send("topic001", "spring.order", message);
    }

    @Test
    public void testSendJavaMessage() throws Exception {

        Order order = new Order();
        order.setId("001");
        order.setName("订单消息");
        order.setContent("订单描述信息");
        ObjectMapper mapper = new ObjectMapper();
        String json = mapper.writeValueAsString(order);
        System.err.println("order 4 json: " + json);

        MessageProperties messageProperties = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties.setContentType("application/json");
        //这个head字段会被DefaultJackson2JavaTypeMapper识别转成对应的对象
        messageProperties.getHeaders().put("__TypeId__", "com.pikaqiu.spring.entity.Order");
        Message message = new Message(json.getBytes(), messageProperties);

        rabbitTemplate.send("topic001", "spring.order", message);
    }

    @Test
    public void testSendMappingMessage() throws Exception {

        ObjectMapper mapper = new ObjectMapper();

        Order order = new Order();
        order.setId("001");
        order.setName("订单消息");
        order.setContent("订单描述信息");

        String json1 = mapper.writeValueAsString(order);
        System.err.println("order 4 json: " + json1);

        MessageProperties messageProperties1 = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties1.setContentType("application/json");
        messageProperties1.getHeaders().put("__TypeId__", "order");
        Message message1 = new Message(json1.getBytes(), messageProperties1);
        rabbitTemplate.send("topic001", "spring.order", message1);

        Packaged pack = new Packaged();
        pack.setId("002");
        pack.setName("包裹消息");
        pack.setDescription("包裹描述信息");

        String json2 = mapper.writeValueAsString(pack);
        System.err.println("pack 4 json: " + json2);

        MessageProperties messageProperties2 = new MessageProperties();
        //这里注意一定要修改contentType为 application/json
        messageProperties2.setContentType("application/json");
//        messageProperties2.getHeaders().put("__TypeId__", "packaged");
        messageProperties2.getHeaders().put("__TypeId__", "packaged");
        Message message2 = new Message(json2.getBytes(), messageProperties2);
        rabbitTemplate.send("topic001", "spring.pack", message2);
    }

    @Test
    public void testSendExtConverterMessage() throws Exception {
//			byte[] body = Files.readAllBytes(Paths.get("d:/002_books", "picture.png"));
//			MessageProperties messageProperties = new MessageProperties();
//			messageProperties.setContentType("image/png");
//			messageProperties.getHeaders().put("extName", "png");
//			Message message = new Message(body, messageProperties);
//			rabbitTemplate.send("", "image_queue", message);

        byte[] body = Files.readAllBytes(Paths.get("d:/002_books", "mysql.pdf"));
        MessageProperties messageProperties = new MessageProperties();
        messageProperties.setContentType("application/pdf");
        Message message = new Message(body, messageProperties);
        rabbitTemplate.send("", "pdf_queue", message);
    }


}


