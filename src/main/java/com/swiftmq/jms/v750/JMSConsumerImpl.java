package com.swiftmq.jms.v750;

import javax.jms.*;

public class JMSConsumerImpl implements JMSConsumer {
    MessageConsumer consumer;

    public JMSConsumerImpl(MessageConsumer consumer) {
        this.consumer = consumer;
    }

    @Override
    public String getMessageSelector() {
        try {
            return consumer.getMessageSelector();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public MessageListener getMessageListener() throws JMSRuntimeException {
        try {
            return consumer.getMessageListener();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void setMessageListener(MessageListener messageListener) throws JMSRuntimeException {
        try {
            consumer.setMessageListener(messageListener);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Message receive() {
        try {
            return consumer.receive();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Message receive(long timeout) {
        try {
            return consumer.receive(timeout);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Message receiveNoWait() {
        try {
            return consumer.receiveNoWait();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public <T> T receiveBody(Class<T> aClass) {
        Message msg = receive();
        try {
            return msg == null ? null : msg.getBody(aClass);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public <T> T receiveBody(Class<T> aClass, long timeout) {
        Message msg = receive(timeout);
        try {
            return msg == null ? null : msg.getBody(aClass);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public <T> T receiveBodyNoWait(Class<T> aClass) {
        Message msg = receiveNoWait();
        try {
            return msg == null ? null : msg.getBody(aClass);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void close() {
        try {
            consumer.close();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }
}
