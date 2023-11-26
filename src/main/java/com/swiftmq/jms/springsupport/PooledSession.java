package com.swiftmq.jms.springsupport;

import javax.jms.*;
import java.io.Serializable;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class PooledSession
        implements Session, QueueSession, TopicSession {
    static final boolean DEBUG = Boolean.parseBoolean(System.getProperty("swiftmq.springsupport.debug", "false"));
    private static final String NULL_DESTINATION = "_NULL_";
    SharedJMSConnection internalConnection;
    Session internalSession;
    Map<String, PooledConsumer> consumerPool = new ConcurrentHashMap<>();
    Map<String, PooledProducer> producerPool = new ConcurrentHashMap<>();

    public PooledSession(SharedJMSConnection internalConnection, Session internalSession) {
        this.internalConnection = internalConnection;
        this.internalSession = internalSession;
        if (DEBUG) System.out.println(this + "/created");
    }

    protected void checkIn(PooledConsumer pooledConsumer) {
        if (DEBUG) System.out.println(this + "/checkIn, pooledConsumer=" + pooledConsumer);
        consumerPool.put(pooledConsumer.getKey().getKey(), pooledConsumer);
    }

    protected void checkIn(PooledProducer pooledProducer) {
        if (DEBUG) System.out.println(this + "/checkIn, pooledProducer=" + pooledProducer);
        try {
            Destination dest = pooledProducer.getDestination();
            String name = dest != null ? dest.toString() : NULL_DESTINATION;
            producerPool.put(name, pooledProducer);
        } catch (JMSException e) {
        }
    }

    public void checkExpired() {
        if (consumerPool.size() == 0 && producerPool.size() == 0)
            return;
        for (Iterator<Map.Entry<String, PooledConsumer>> iter = consumerPool.entrySet().iterator(); iter.hasNext(); ) {
            PooledConsumer pc = (PooledConsumer) ((Map.Entry<?, ?>) iter.next()).getValue();
            if (pc.getCheckInTime() + internalConnection.getPoolExpiration() <= System.currentTimeMillis()) {
                if (DEBUG) System.out.println(this + "/checkExpired, expired=" + pc);
                pc.closeInternal();
                iter.remove();
            }
        }
        for (Iterator<Map.Entry<String, PooledProducer>> iter = producerPool.entrySet().iterator(); iter.hasNext(); ) {
            PooledProducer pp = (PooledProducer) ((Map.Entry<?, ?>) iter.next()).getValue();
            if (pp.getCheckInTime() + internalConnection.getPoolExpiration() <= System.currentTimeMillis()) {
                if (DEBUG) System.out.println(this + "/checkExpired, expired=" + pp);
                pp.closeInternal();
                iter.remove();
            }
        }
    }

    public BytesMessage createBytesMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createBytesMessage");
        return internalSession.createBytesMessage();
    }

    public MapMessage createMapMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createMapMessage");
        return internalSession.createMapMessage();
    }

    public Message createMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createMessage");
        return internalSession.createMessage();
    }

    public ObjectMessage createObjectMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createObjectMessage");
        return internalSession.createObjectMessage();
    }

    public ObjectMessage createObjectMessage(Serializable object) throws JMSException {
        if (DEBUG) System.out.println(this + "/createObjectMessage2");
        return internalSession.createObjectMessage(object);
    }

    public StreamMessage createStreamMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createStreamMessage");
        return internalSession.createStreamMessage();
    }

    public TextMessage createTextMessage() throws JMSException {
        if (DEBUG) System.out.println(this + "/createTextMessage");
        return internalSession.createTextMessage();
    }

    public TextMessage createTextMessage(String s) throws JMSException {
        if (DEBUG) System.out.println(this + "/createTextMessage2");
        return internalSession.createTextMessage(s);
    }

    public boolean getTransacted() throws JMSException {
        if (DEBUG) System.out.println(this + "/getTransacted");
        return internalSession.getTransacted();
    }

    public int getAcknowledgeMode() throws JMSException {
        if (DEBUG) System.out.println(this + "/getAcknowledgeMode");
        return internalSession.getAcknowledgeMode();
    }

    public void commit() throws JMSException {
        if (DEBUG) System.out.println(this + "/commit");
        internalSession.commit();
    }

    public void rollback() throws JMSException {
        if (DEBUG) System.out.println(this + "/rollback");
        internalSession.rollback();
    }

    public void close() throws JMSException {
        if (DEBUG) System.out.println(this + "/close");
        internalConnection.checkIn(this);
    }

    protected void closeInternal() throws JMSException {
        if (DEBUG) System.out.println(this + "/closeInternal");
        for (Iterator<Map.Entry<String, PooledConsumer>> iter = consumerPool.entrySet().iterator(); iter.hasNext(); ) {
            PooledConsumer pc = (PooledConsumer) ((Map.Entry<?, ?>) iter.next()).getValue();
            if (DEBUG) System.out.println(this + "/closeInternal, close=" + pc);
            pc.closeInternal();
            iter.remove();
        }
        for (Iterator<Map.Entry<String, PooledProducer>> iter = producerPool.entrySet().iterator(); iter.hasNext(); ) {
            PooledProducer pp = (PooledProducer) ((Map.Entry<?, ?>) iter.next()).getValue();
            if (DEBUG) System.out.println(this + "/closeInternal, close=" + pp);
            pp.closeInternal();
            iter.remove();
        }
        internalSession.close();
    }

    public void recover() throws JMSException {
        if (DEBUG) System.out.println(this + "/recover");
        internalSession.recover();
    }

    public MessageListener getMessageListener() throws JMSException {
        if (DEBUG) System.out.println(this + "/getMessageListener");
        return internalSession.getMessageListener();
    }

    public void setMessageListener(MessageListener ml) throws JMSException {
        if (DEBUG) System.out.println(this + "/setMessageListener, ml=" + ml);
        internalSession.setMessageListener(ml);
    }

    public void run() {
        if (DEBUG) System.out.println(this + "/run");
        internalSession.run();
    }

    public Queue createQueue(String s) throws JMSException {
        if (DEBUG) System.out.println(this + "/createQueue, s=" + s);
        return internalSession.createQueue(s);
    }

    public Topic createTopic(String s) throws JMSException {
        if (DEBUG) System.out.println(this + "/createTopic, s=" + s);
        return internalSession.createTopic(s);
    }

    public QueueBrowser createBrowser(Queue queue) throws JMSException {
        if (DEBUG) System.out.println(this + "/createBrowser, queue=" + queue);
        return internalSession.createBrowser(queue);
    }

    public QueueBrowser createBrowser(Queue queue, String string) throws JMSException {
        if (DEBUG) System.out.println(this + "/createBrowser, queue=" + queue + ", sel=" + string);
        return internalSession.createBrowser(queue, string);
    }

    public TemporaryQueue createTemporaryQueue() throws JMSException {
        if (DEBUG) System.out.println(this + "/createTemporaryQueue");
        return internalSession.createTemporaryQueue();
    }

    public TemporaryTopic createTemporaryTopic() throws JMSException {
        if (DEBUG) System.out.println(this + "/createTemporaryTopic");
        return internalSession.createTemporaryTopic();
    }

    public void unsubscribe(String string) throws JMSException {
        if (DEBUG) System.out.println(this + "/unsubscribe, s=" + string);
        internalSession.unsubscribe(string);
    }

    public QueueSender createSender(Queue queue) throws JMSException {
        String name = queue != null ? queue.toString() : NULL_DESTINATION;
        PooledProducer pp = producerPool.remove(name);
        if (pp != null) {
            if (DEBUG) System.out.println(this + "/createSender, queue=" + name + ", return from pool");
            return pp;
        }
        if (DEBUG) System.out.println(this + "/createSender, queue=" + name + ", creating new");
        return new PooledProducer(this, internalSession.createProducer(queue), queue);
    }

    public TopicPublisher createPublisher(Topic topic) throws JMSException {
        String name = topic != null ? topic.toString() : NULL_DESTINATION;
        PooledProducer pp = producerPool.remove(name);
        if (pp != null) {
            if (DEBUG) System.out.println(this + "/createPublisher, topic=" + name + ", return from pool");
            return pp;
        }
        if (DEBUG) System.out.println(this + "/createPublisher, topic=" + name + ", creating new");
        return new PooledProducer(this, internalSession.createProducer(topic), topic);
    }

    public MessageProducer createProducer(Destination destination) throws JMSException {
        String name = destination != null ? destination.toString() : NULL_DESTINATION;
        PooledProducer pp = producerPool.remove(name);
        if (pp != null) {
            if (DEBUG) System.out.println(this + "/createProducer, destination=" + name + ", return from pool");
            return pp;
        }
        if (DEBUG) System.out.println(this + "/createProducer, destination=" + name + ", creating new");
        return new PooledProducer(this, internalSession.createProducer(destination), destination);
    }

    public QueueReceiver createReceiver(Queue queue) throws JMSException {
        ConsumerKey key = new ConsumerKey(queue.getQueueName(), null, true, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createReceiver, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createReceiver, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(queue), queue, true, key);
    }

    public QueueReceiver createReceiver(Queue queue, String s) throws JMSException {
        ConsumerKey key = new ConsumerKey(queue.getQueueName(), s, true, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createReceiver, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createReceiver, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(queue, s), queue, true, key);
    }

    public TopicSubscriber createSubscriber(Topic topic) throws JMSException {
        ConsumerKey key = new ConsumerKey(topic.getTopicName(), null, true, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createSubscriber, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createSubscriber, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(topic), topic, true, key);
    }

    public TopicSubscriber createSubscriber(Topic topic, String s, boolean b) throws JMSException {
        ConsumerKey key = new ConsumerKey(topic.getTopicName(), s, b, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createSubscriber, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createSubscriber, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(topic, s, b), topic, b, key);
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String s) throws JMSException {
        ConsumerKey key = new ConsumerKey(topic.getTopicName(), null, true, s);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createDurableSubscriber, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createDurableSubscriber, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createDurableSubscriber(topic, s), topic, true, key);
    }

    public TopicSubscriber createDurableSubscriber(Topic topic, String s, String s1, boolean b) throws JMSException {
        ConsumerKey key = new ConsumerKey(topic.getTopicName(), s1, b, s);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createDurableSubscriber, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createDurableSubscriber, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createDurableSubscriber(topic, s, s1, b), topic, b, key);
    }

    public MessageConsumer createConsumer(Destination destination) throws JMSException {
        ConsumerKey key = new ConsumerKey(destination.toString(), null, true, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(destination), destination, true, key);
    }

    public MessageConsumer createConsumer(Destination destination, String s) throws JMSException {
        ConsumerKey key = new ConsumerKey(destination.toString(), s, true, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(destination, s), destination, true, key);
    }

    public MessageConsumer createConsumer(Destination destination, String s, boolean b) throws JMSException {
        ConsumerKey key = new ConsumerKey(destination.toString(), s, b, null);
        PooledConsumer pc = consumerPool.remove(key.getKey());
        if (pc != null) {
            if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", return from pool");
            return pc;
        }
        if (DEBUG) System.out.println(this + "/createConsumer, key=" + key + ", creating new");
        return new PooledConsumer(this, internalSession.createConsumer(destination, s, b), destination, b, key);
    }

    public String toString() {
        return "/PooledSession, internalSession=" + internalSession;
    }
}
