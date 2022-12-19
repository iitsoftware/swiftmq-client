package com.swiftmq.jms.springsupport;

import javax.jms.Queue;
import javax.jms.*;
import java.util.*;

public class SharedJMSConnection
        implements Connection, QueueConnection, TopicConnection {
    static final boolean DEBUG = Boolean.valueOf(System.getProperty("swiftmq.springsupport.debug", "false"));
    Connection internalConnection;
    long poolExpiration;
    boolean firstTransacted = false;
    int firstAckMode = -1;
    final List pool = new LinkedList();
    final Timer timer = new Timer(true);
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
        if (DEBUG) System.out.println(this + "/created");
    }

    public long getPoolExpiration() {
        return poolExpiration;
    }

    public synchronized Session createSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(this + "/createSession, poolSize=" + pool.size());
        if (pool.size() > 0) {
            PoolEntry entry = (PoolEntry) pool.remove(0);
            if (DEBUG) System.out.println(this + "/createSession, returning session from pool: " + entry);
            return entry.pooledSession;
        }
        if (firstAckMode == -1) {
            firstTransacted = transacted;
            firstAckMode = ackMode;
        } else {
            if (transacted != firstTransacted || ackMode != firstAckMode)
                throw new javax.jms.IllegalStateException("SharedJMSConnection: all JMS session must have the same transacted flag and ackMode!");
        }
        if (DEBUG) System.out.println(this + "/createSession, returning a new session");
        return new PooledSession(this, internalConnection.createSession(transacted, ackMode));
    }

    protected synchronized void checkIn(PooledSession pooledSession) {
        PoolEntry entry = new PoolEntry(System.currentTimeMillis(), pooledSession);
        if (pool.size() == 0)
            pool.add(entry);
        else
            pool.add(0, entry);
        if (DEBUG) System.out.println(this + "/checkIn, poolSize=" + pool.size() + ", entry=" + entry);
    }

    public synchronized void checkExpired() {
        if (DEBUG) System.out.println(this + "/checkExpired, poolSize=" + pool.size());
        if (pool.size() == 0)
            return;
        for (Iterator iter = pool.iterator(); iter.hasNext(); ) {
            PoolEntry entry = (PoolEntry) iter.next();
            long now = System.currentTimeMillis();
            if (DEBUG)
                System.out.println(this + "/checkExpired, now=" + now + ", expTime=" + (entry.poolInsertionTime + poolExpiration));
            entry.pooledSession.checkExpired();
            if (entry.poolInsertionTime + poolExpiration <= now) {
                try {
                    if (DEBUG) System.out.println(this + "/checkExpired, closing session=" + entry.pooledSession);
                    entry.pooledSession.closeInternal();
                } catch (JMSException e) {
                }
                iter.remove();
            }
        }
    }

    public QueueSession createQueueSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(this + "/createQueueSession");
        return (QueueSession) createSession(transacted, ackMode);
    }

    public TopicSession createTopicSession(boolean transacted, int ackMode) throws JMSException {
        if (DEBUG) System.out.println(this + "/createTopicSession");
        return (TopicSession) createSession(transacted, ackMode);
    }

    public String getClientID() throws JMSException {
        if (DEBUG) System.out.println(this + "/getClientID");
        return internalConnection.getClientID();
    }

    public void setClientID(String cid) throws JMSException {
        if (DEBUG) System.out.println(this + "/setClientID, id=" + cid);
        internalConnection.setClientID(cid);
    }

    public ConnectionMetaData getMetaData() throws JMSException {
        if (DEBUG) System.out.println(this + "/getMetaData");
        return internalConnection.getMetaData();
    }

    public ExceptionListener getExceptionListener() throws JMSException {
        if (DEBUG) System.out.println(this + "/getExceptionListener");
        return internalConnection.getExceptionListener();
    }

    public void setExceptionListener(ExceptionListener exceptionListener) throws JMSException {
        if (DEBUG) System.out.println(this + "/setExceptionListener");
        internalConnection.setExceptionListener(exceptionListener);
    }

    public void start() throws JMSException {
        if (DEBUG) System.out.println(this + "/start");
        internalConnection.start();
    }

    public void stop() throws JMSException {
        if (DEBUG) System.out.println(this + "/stop");
        internalConnection.stop();
    }

    public void close() throws JMSException {
        if (DEBUG) System.out.println(this + "/close (ignore)");
    }

    public synchronized void destroy() throws Exception {
        if (DEBUG) System.out.println(this + "/destroy");
        if (pool.size() > 0) {
            for (Iterator iter = pool.iterator(); iter.hasNext(); ) {
                PoolEntry entry = (PoolEntry) iter.next();
                if (DEBUG) System.out.println(this + "/destroy, closing session=" + entry.pooledSession);
                entry.pooledSession.closeInternal();
                iter.remove();
            }
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

    /*
     * TODO: JMS.2.0
     */
    @Override
    public Session createSession(int i) throws JMSException {
        return null;
    }

    @Override
    public Session createSession() throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedConnectionConsumer(Topic topic, String s, String s1, ServerSessionPool serverSessionPool, int i) throws JMSException {
        return null;
    }

    @Override
    public ConnectionConsumer createSharedDurableConnectionConsumer(Topic topic, String s, String s1, ServerSessionPool serverSessionPool, int i) throws JMSException {
        return null;
    }

    public String toString() {
        return "/SharedJMSConnection";
    }

    private static class PoolEntry {
        long poolInsertionTime;
        PooledSession pooledSession;

        public PoolEntry(long poolInsertionTime, PooledSession pooledSession) {
            this.poolInsertionTime = poolInsertionTime;
            this.pooledSession = pooledSession;
        }

        public String toString() {
            return "/PoolEntry, insertionTime=" + poolInsertionTime + ", pooledSession=" + pooledSession;
        }
    }
}
