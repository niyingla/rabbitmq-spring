package com.pikaqiu.spring;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.HashMap;

@RunWith(SpringRunner.class)
@SpringBootTest
public class RabbitMQApplicationTests {

    @Autowired
    private RabbitAdmin rabbitAdmin;

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
}


