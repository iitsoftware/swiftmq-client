package com.swiftmq.jms.springsupport;

import javax.jms.*;

public class PooledProducer
        implements QueueSender, TopicPublisher {
    static final boolean DEBUG = Boolean.valueOf(System.getProperty("swiftmq.springsupport.debug", "false"));
    PooledSession pooledSession;
    MessageProducer internalProducer;
    Destination internalDestination;
    long checkInTime = -1;

    public PooledProducer(PooledSession pooledSession, MessageProducer internalProducer, Destination internalDestination) {
        this.pooledSession = pooledSession;
        this.internalProducer = internalProducer;
        this.internalDestination = internalDestination;
        if (DEBUG) System.out.println(this + "/created");
    }

    public boolean getDisableMessageID() throws JMSException {
        return internalProducer.getDisableMessageID();
    }

    public void setDisableMessageID(boolean b) throws JMSException {
        internalProducer.setDisableMessageID(b);
    }

    public boolean getDisableMessageTimestamp() throws JMSException {
        return internalProducer.getDisableMessageTimestamp();
    }

    public void setDisableMessageTimestamp(boolean b) throws JMSException {
        internalProducer.setDisableMessageTimestamp(b);
    }

    public int getDeliveryMode() throws JMSException {
        return internalProducer.getDeliveryMode();
    }

    public void setDeliveryMode(int i) throws JMSException {
        internalProducer.setDeliveryMode(i);
    }

    public int getPriority() throws JMSException {
        return internalProducer.getPriority();
    }

    public void setPriority(int i) throws JMSException {
        internalProducer.setPriority(i);
    }

    public long getTimeToLive() throws JMSException {
        return internalProducer.getTimeToLive();
    }

    public void setTimeToLive(long l) throws JMSException {
        internalProducer.setTimeToLive(l);
    }

    public Destination getDestination() throws JMSException {
        return internalDestination;
    }

    public long getCheckInTime() {
        return checkInTime;
    }

    protected void closeInternal() {
        if (DEBUG) System.out.println(this + "/closeInternal");
        try {
            internalProducer.close();
        } catch (JMSException e) {
        }
    }

    public void close() throws JMSException {
        if (DEBUG) System.out.println(this + "/close");
        checkInTime = System.currentTimeMillis();
        pooledSession.checkIn(this);
    }

    public void send(Destination destination, Message message) throws JMSException {
        if (DEBUG) System.out.println(this + "/send, destination=" + destination + ", message=" + message);
        internalProducer.send(destination, message);
    }

    public void send(Destination destination, Message message, int i, int i1, long l) throws JMSException {
        if (DEBUG)
            System.out.println(this + "/send, destination=" + destination + ", message=" + message + ", i=" + i + ", i1=" + i1 + ", l=" + l);
        internalProducer.send(destination, message, i, i1, l);
    }

    public Queue getQueue() throws JMSException {
        return (Queue) internalDestination;
    }

    public void send(Message message) throws JMSException {
        if (DEBUG) System.out.println(this + "/send, message=" + message);
        internalProducer.send(message);

    }

    public void send(Message message, int i, int i1, long l) throws JMSException {
        if (DEBUG)
            System.out.println(this + "/send, message=" + message + ", i=" + i + ", i1=" + i1 + ", l=" + l);
        internalProducer.send(message, i, i1, l);
    }

    public void send(Queue queue, Message message) throws JMSException {
        if (DEBUG) System.out.println(this + "/send, queue=" + queue + ", message=" + message);
        internalProducer.send(queue, message);
    }

    public void send(Queue queue, Message message, int i, int i1, long l) throws JMSException {
        if (DEBUG)
            System.out.println(this + "/send, queue=" + queue + ", message=" + message + ", i=" + i + ", i1=" + i1 + ", l=" + l);
        internalProducer.send(queue, message, i, i1, l);
    }

    public Topic getTopic() throws JMSException {
        return (Topic) internalDestination;
    }

    public void publish(Message message) throws JMSException {
        if (DEBUG) System.out.println(this + "/publish, message=" + message);
        internalProducer.send(message);
    }

    public void publish(Message message, int i, int i1, long l) throws JMSException {
        if (DEBUG)
            System.out.println(this + "/publish, message=" + message + ", i=" + i + ", i1=" + i1 + ", l=" + l);
        internalProducer.send(message, i, i1, l);
    }

    public void publish(Topic topic, Message message) throws JMSException {
        if (DEBUG) System.out.println(this + "/publish, topic=" + topic + ", message=" + message);
        internalProducer.send(topic, message);
    }

    public void publish(Topic topic, Message message, int i, int i1, long l) throws JMSException {
        if (DEBUG)
            System.out.println(this + "/publish, topic=" + topic + ", message=" + message + ", i=" + i + ", i1=" + i1 + ", l=" + l);
        internalProducer.send(topic, message, i, i1, l);
    }

    /*
     * JMS.2.0
     */
    @Override
    public void setDeliveryDelay(long l) throws JMSException {
        internalProducer.setDeliveryDelay(l);
    }

    @Override
    public long getDeliveryDelay() throws JMSException {
        return internalProducer.getDeliveryDelay();
    }

    @Override
    public void send(Message message, CompletionListener completionListener) throws JMSException {
        internalProducer.send(message, completionListener);
    }

    @Override
    public void send(Message message, int i, int i1, long l, CompletionListener completionListener) throws JMSException {
        internalProducer.send(message, i, i1, l, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, CompletionListener completionListener) throws JMSException {
        internalProducer.send(destination, message, completionListener);
    }

    @Override
    public void send(Destination destination, Message message, int i, int i1, long l, CompletionListener completionListener) throws JMSException {
        internalProducer.send(destination, message, i, i1, l, completionListener);
    }

    public String toString() {
        return "/PooledProducer, internalDestination=" + internalDestination;
    }
}
