package com.swiftmq.jms.v750;

import javax.jms.JMSConsumer;
import javax.jms.JMSRuntimeException;
import javax.jms.Message;
import javax.jms.MessageListener;

public class JMSConsumerImpl implements JMSConsumer {
    @Override
    public String getMessageSelector() {
        return null;
    }

    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {
        return null;
    }

    @Override
    public void setMessageListener(MessageListener messageListener) throws JMSRuntimeException {

    }

    @Override
    public Message receive() {
        return null;
    }

    @Override
    public Message receive(long l) {
        return null;
    }

    @Override
    public Message receiveNoWait() {
        return null;
    }

    @Override
    public <T> T receiveBody(Class<T> aClass) {
        return null;
    }

    @Override
    public <T> T receiveBody(Class<T> aClass, long l) {
        return null;
    }

    @Override
    public <T> T receiveBodyNoWait(Class<T> aClass) {
        return null;
    }

    @Override
    public void close() {

    }
}
