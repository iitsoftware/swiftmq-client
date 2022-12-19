package com.swiftmq.jms.springsupport;

import javax.jms.*;

public class PooledConsumer
        implements QueueReceiver, TopicSubscriber {
    static final boolean DEBUG = Boolean.valueOf(System.getProperty("swiftmq.springsupport.debug", "false"));
    PooledSession pooledSession;
    MessageConsumer internalConsumer;
    long checkInTime = -1;
    ConsumerKey key;
    Destination dest;
    boolean noLocal;

    public PooledConsumer(PooledSession pooledSession, MessageConsumer internalConsumer, Destination dest, boolean noLocal, ConsumerKey key) {
        this.pooledSession = pooledSession;
        this.internalConsumer = internalConsumer;
        this.dest = dest;
        this.noLocal = noLocal;
        this.key = key;
        if (DEBUG) System.out.println(this + "/created");
    }

    public ConsumerKey getKey() {
        return key;
    }

    public long getCheckInTime() {
        return checkInTime;
    }

    public String getMessageSelector() throws JMSException {
        return internalConsumer.getMessageSelector();
    }

    public MessageListener getMessageListener() throws JMSException {
        return internalConsumer.getMessageListener();
    }

    public void setMessageListener(MessageListener messageListener) throws JMSException {
        if (DEBUG) System.out.println(this + "/setMessageListener, ml=" + messageListener);
        internalConsumer.setMessageListener(messageListener);
    }

    public Message receive() throws JMSException {
        if (DEBUG) System.out.println(this + "/receive");
        return internalConsumer.receive();
    }

    public Message receive(long l) throws JMSException {
        if (DEBUG) System.out.println(this + "/receive, to=" + l);
        return internalConsumer.receive(l);
    }

    public Message receiveNoWait() throws JMSException {
        if (DEBUG) System.out.println(this + "/receiveNoWait");
        return internalConsumer.receiveNoWait();
    }

    protected void closeInternal() {
        if (DEBUG) System.out.println(this + "/closeInternal");
        try {
            internalConsumer.close();
        } catch (JMSException e) {
        }
    }

    public void close() throws JMSException {
        if (DEBUG) System.out.println(this + "/close");
        checkInTime = System.currentTimeMillis();
        pooledSession.checkIn(this);
    }

    public Queue getQueue() throws JMSException {
        return (Queue) dest;
    }

    public Topic getTopic() throws JMSException {
        return (Topic) dest;
    }

    public boolean getNoLocal() throws JMSException {
        return noLocal;
    }

    public String toString() {
        return "/PooledConsumer, key=" + key;
    }
}
