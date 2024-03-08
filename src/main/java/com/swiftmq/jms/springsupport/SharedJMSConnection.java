package com.swiftmq.jms.springsupport;

import javax.jms.*;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;

public class SharedJMSConnection
        implements Connection, QueueConnection, TopicConnection {
    static final boolean DEBUG = Boolean.valueOf(System.getProperty("swiftmq.springsupport.debug", "false")).booleanValue();
    Connection internalConnection = null;
    long poolExpiration = 0;
    boolean firstTransacted = false;
    int firstAckMode = -1;
    private final ConcurrentLinkedQueue<PoolEntry> pool = new ConcurrentLinkedQueue<>();
    private final Timer timer = new Timer(true);
    TimerTask expiryChecker = null;

    public SharedJMSConnection(Connection internalConnection, long poolExpiration) {
        this.internalConnection = internalConnection;
        this.poolExpiration = poolExpiration;
        if (poolExpiration > 0) {
            long delay = poolExpiration + 500;
            expiryChecker = new TimerTask() {
                public void run() {
                    checkExpired();
                }
            };
            timer.schedule(expiryChecker, delay, delay);
        }
        if (DEBUG) System.out.println(toString() + "/created");
    }

    public long getPoolExpiration() {
        return poolExpiration;
    }

    public Session createSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(toString() + "/createSession, poolSize=" + pool.size());
        PoolEntry entry = pool.poll();
        if (entry != null) {
            if (DEBUG) System.out.println(toString() + "/createSession, returning session from pool: " + entry);
            return entry.pooledSession;
        }
        if (firstAckMode == -1) {
            firstTransacted = transacted;
            firstAckMode = ackMode;
        } else {
            if (transacted != firstTransacted || ackMode != firstAckMode)
                throw new javax.jms.IllegalStateException("SharedJMSConnection: all JMS session must have the same transacted flag and ackMode!");
        }
        if (DEBUG) System.out.println(toString() + "/createSession, returning a new session");
        return new PooledSession(this, internalConnection.createSession(transacted, ackMode));
    }

    protected void checkIn(PooledSession pooledSession) {
        pool.offer(new PoolEntry(System.currentTimeMillis(), pooledSession));
        if (DEBUG) System.out.println(toString() + "/checkIn, poolSize=" + pool.size());
    }

    public void checkExpired() {
        if (DEBUG) System.out.println(toString() + "/checkExpired, poolSize=" + pool.size());
        long now = System.currentTimeMillis();

        PoolEntry entry;
        while ((entry = pool.peek()) != null && entry.poolInsertionTime + poolExpiration <= now) {
            if (DEBUG) {
                System.out.println(toString() + "/checkExpired, now=" + now + ", expTime=" + (entry.poolInsertionTime + poolExpiration));
                entry.pooledSession.checkExpired();
                System.out.println(toString() + "/checkExpired, closing session=" + entry.pooledSession);
            }
            try {
                entry.pooledSession.closeInternal();
            } catch (JMSException e) {
                // Exception handling
            }
            pool.poll(); // Remove the processed entry
        }
    }

    public QueueSession createQueueSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(toString() + "/createQueueSession");
        return (QueueSession) createSession(transacted, ackMode);
    }

    public TopicSession createTopicSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(toString() + "/createTopicSession");
        return (TopicSession) createSession(transacted, ackMode);
    }

    public String getClientID() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/getClientID");
        return internalConnection.getClientID();
    }

    public void setClientID(String cid) throws JMSException {
        if (DEBUG) System.out.println(toString() + "/setClientID, id=" + cid);
        internalConnection.setClientID(cid);
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/getMetaData");
        return internalConnection.getMetaData();
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/getExceptionListener");
        return internalConnection.getExceptionListener();
    }

    public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
        if (DEBUG) System.out.println(toString() + "/setExceptionListener");
        internalConnection.setExceptionListener(exceptionListener);
    }

    public void start() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/start");
        internalConnection.start();
    }

    public void stop() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/stop");
        internalConnection.stop();
    }

    public void close() throws JMSException {
        if (DEBUG) System.out.println(toString() + "/close (ignore)");
    }

    public void destroy() throws Exception {
        if (DEBUG) System.out.println(toString() + "/destroy");

        PoolEntry entry;
        while ((entry = pool.poll()) != null) {
            if (DEBUG) System.out.println(toString() + "/destroy, closing session=" + entry.pooledSession);
            entry.pooledSession.closeInternal();
        }

        internalConnection.close();
    }

    public ConnectionConsumer createConnectionConsumer(Destination destination, String string, ServerSessionPool serverSessionPool, int i) throws JMSException {
        throw new javax.jms.IllegalStateException("SharedJMSConnection: operation is not supported!");
    }

    public ConnectionConsumer createDurableConnectionConsumer(Topic topic, String string, String string1, ServerSessionPool serverSessionPool, int i) throws JMSException {
        throw new javax.jms.IllegalStateException("SharedJMSConnection: operation is not supported!");
    }

    public ConnectionConsumer createConnectionConsumer(Queue queue, String string, ServerSessionPool serverSessionPool, int i) throws JMSException {
        throw new javax.jms.IllegalStateException("SharedJMSConnection: operation is not supported!");
    }

    public ConnectionConsumer createConnectionConsumer(Topic topic, String string, ServerSessionPool serverSessionPool, int i) throws JMSException {
        throw new javax.jms.IllegalStateException("SharedJMSConnection: operation is not supported!");
    }

    public String toString() {
        return "/SharedJMSConnection";
    }

    private class PoolEntry {
        long poolInsertionTime = 0;
        PooledSession pooledSession = null;

        public PoolEntry(long poolInsertionTime, PooledSession pooledSession) {
            this.poolInsertionTime = poolInsertionTime;
            this.pooledSession = pooledSession;
        }

        public String toString() {
            return "/PoolEntry, insertionTime=" + poolInsertionTime + ", pooledSession=" + pooledSession;
        }
    }
}
