package com.swiftmq.jms.v750;

import com.swiftmq.swiftlet.queue.MessageIndex;

import javax.jms.*;
import java.io.Serializable;

public class JMSContextImpl implements JMSContext {
    private final Connection connection;
    private Session session = null;
    private boolean autostart = true;
    private boolean closed = false;
    private MessageIndex lastMessageIndex = null;

    JMSContextImpl(Connection connection) {
        this.connection = connection;
    }

    void createSession(int sessionMode) throws JMSRuntimeException {
        try {
            this.session = connection.createSession(sessionMode);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.toString());
        }
    }

    private void checkDefaultSession() {
        if (session == null)
            createSession(Session.AUTO_ACKNOWLEDGE);
    }

    private void checkAutoStart() {
        if (autostart)
            start();
    }

    private void checkClosed() {
        if (closed)
            throw new IllegalStateRuntimeException("Context is closed");
    }

    public synchronized void setLastMessageIndex(MessageIndex lastMessageIndex) {
        this.lastMessageIndex = lastMessageIndex;
    }

    @Override
    public JMSContext createContext(int sessionMode) {
        checkClosed();
        JMSContextImpl context = new JMSContextImpl(connection);
        context.createSession(sessionMode);
        return context;
    }

    @Override
    public JMSProducer createProducer() {
        checkClosed();
        checkDefaultSession();
        try {
            return new JMSProducerImpl((MessageProducerImpl) session.createProducer(null));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public String getClientID() {
        checkClosed();
        try {
            return connection.getClientID();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void setClientID(String clientID) {
        checkClosed();
        try {
            connection.setClientID(clientID);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public ConnectionMetaData getMetaData() {
        checkClosed();
        try {
            return connection.getMetaData();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public ExceptionListener getExceptionListener() {
        checkClosed();
        try {
            return connection.getExceptionListener();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void setExceptionListener(ExceptionListener listener) {
        checkClosed();
        try {
            connection.setExceptionListener(listener);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void start() {
        checkClosed();
        try {
            connection.start();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void stop() {
        checkClosed();
        try {
            connection.stop();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void setAutoStart(boolean autoStart) {
        checkClosed();
        this.autostart = autoStart;
    }

    @Override
    public boolean getAutoStart() {
        checkClosed();
        return autostart;
    }

    @Override
    public void close() {
        checkClosed();
        try {
            connection.close();
            closed = true;
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public BytesMessage createBytesMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createBytesMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public MapMessage createMapMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createMapMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Message createMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public ObjectMessage createObjectMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createObjectMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public ObjectMessage createObjectMessage(Serializable object) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createObjectMessage(object);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public StreamMessage createStreamMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createStreamMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public TextMessage createTextMessage() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createTextMessage();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public TextMessage createTextMessage(String text) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createTextMessage(text);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public boolean getTransacted() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.getTransacted();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public int getSessionMode() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.getAcknowledgeMode();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void commit() {
        checkClosed();
        checkDefaultSession();
        try {
            session.commit();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void rollback() {
        checkClosed();
        checkDefaultSession();
        try {
            session.rollback();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void recover() {
        checkClosed();
        checkDefaultSession();
        try {
            session.recover();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createConsumer(Destination destination) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createConsumer(destination));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createConsumer(destination, messageSelector));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createConsumer(Destination destination, String messageSelector, boolean noLocal) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createConsumer(destination, messageSelector, noLocal));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Queue createQueue(String queueName) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createQueue(queueName);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public Topic createTopic(String topicName) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createTopic(topicName);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createDurableConsumer(topic, name));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createDurableConsumer(Topic topic, String name, String messageSelector, boolean noLocal) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createDurableConsumer(topic, name, messageSelector, noLocal));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createSharedDurableConsumer(topic, name));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createSharedDurableConsumer(Topic topic, String name, String messageSelector) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createSharedDurableConsumer(topic, name, messageSelector));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createSharedConsumer(topic, sharedSubscriptionName));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public JMSConsumer createSharedConsumer(Topic topic, String sharedSubscriptionName, String messageSelector) {
        checkClosed();
        checkDefaultSession();
        checkAutoStart();
        try {
            return new JMSConsumerImpl((MessageConsumerImpl) session.createSharedConsumer(topic, sharedSubscriptionName, messageSelector));
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public QueueBrowser createBrowser(Queue queue) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createBrowser(queue);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public QueueBrowser createBrowser(Queue queue, String messageSelector) {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createBrowser(queue, messageSelector);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public TemporaryQueue createTemporaryQueue() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createTemporaryQueue();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public TemporaryTopic createTemporaryTopic() {
        checkClosed();
        checkDefaultSession();
        try {
            return session.createTemporaryTopic();
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void unsubscribe(String name) {
        checkClosed();
        checkDefaultSession();
        try {
            session.unsubscribe(name);
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }

    @Override
    public void acknowledge() {
        checkClosed();
        checkDefaultSession();
        try {
            if (session.getAcknowledgeMode() == Session.CLIENT_ACKNOWLEDGE && lastMessageIndex != null) {
                ((SessionImpl) session).acknowledgeMessage(lastMessageIndex);
                lastMessageIndex = null;
            }
        } catch (JMSException e) {
            throw new JMSRuntimeException(e.getMessage());
        }
    }
}
