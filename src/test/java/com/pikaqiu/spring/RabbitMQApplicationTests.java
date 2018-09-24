package com.pikaqiu.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

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
        rabbitTemplate.send("topic002", "rabbit.amqp", message);


    }
}


