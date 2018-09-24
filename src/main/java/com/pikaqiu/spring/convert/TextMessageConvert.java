package com.pikaqiu.spring.convert;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConversionException;
import org.springframework.amqp.support.converter.MessageConverter;

public class TextMessageConvert implements MessageConverter {


/**
 *自定义消息转换类
 */


    /**
     * java对象转换为message对象
     *
     * @param o
     * @param messageProperties
     * @return
     * @throws MessageConversionException
     */
    @Override
    public Message toMessage(Object o, MessageProperties messageProperties) throws MessageConversionException {

        return new Message(o.toString().getBytes(), messageProperties);
    }

    /**
     * message对象转换为java对象
     *
     * @param message
     * @return
     * @throws MessageConversionException
     */
    @Override
    public Object fromMessage(Message message) throws MessageConversionException {

        String contentType = message.getMessageProperties().getContentType();

        //判断类型 string一般为 text/plain
        if (null != contentType && contentType.contains("text")) {
            return new String(message.getBody());
        }

        //其他类型
        return message.getBody();
    }
}
