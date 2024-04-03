/*
 * Copyright 2019 IIT Software GmbH
 *
 * IIT Software GmbH licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.swiftmq.jndi.v400;

import com.swiftmq.client.Versions;
import com.swiftmq.jms.*;
import com.swiftmq.jndi.StopRetryException;
import com.swiftmq.jndi.protocol.v400.*;
import com.swiftmq.swiftlet.jndi.JNDISwiftlet;
import com.swiftmq.tools.dump.Dumpalizer;
import com.swiftmq.tools.timer.TimerEvent;
import com.swiftmq.tools.timer.TimerListener;
import com.swiftmq.tools.timer.TimerRegistry;
import com.swiftmq.tools.util.DataByteArrayOutputStream;
import com.swiftmq.tools.versioning.Versionable;
import com.swiftmq.tools.versioning.Versioned;

import javax.jms.*;
import javax.naming.*;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ContextImpl implements Context, java.io.Serializable, AutoCloseable, TimerListener {
    Hashtable env = null;
    JNDIInfo jndiInfo = null;
    ConnectionFactory cf = null;
    final AtomicReference<Connection> connection = new AtomicReference<>();
    final AtomicReference<Session> session = new AtomicReference<>();
    final AtomicReference<MessageProducer> producer = new AtomicReference<>();
    final AtomicBoolean closed = new AtomicBoolean(true);
    boolean debug = false;
    final AtomicLong lastAccessTime = new AtomicLong();
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public ContextImpl(Hashtable env)
            throws NamingException {
        this.env = env;
        String url = (String) env.get(Context.PROVIDER_URL);
        if (url == null)
            throw new NamingException("missing JNDI environment property: Context.PROVIDER_URL (" + Context.PROVIDER_URL + ")");
        jndiInfo = URLParser.parseURL(url);
        debug = jndiInfo.isDebug();
        Map props = new HashMap();
        if (jndiInfo.isIntraVM()) {
            try {
                props.put(SwiftMQConnectionFactory.INTRAVM, "true");
            } catch (Exception e) {
                throw new NamingException("unable to connect, exception = " + e);
            }
        } else {
            String factoryClass = jndiInfo.getFactory();
            try {
                Class.forName(factoryClass);
            } catch (Exception e) {
                throw new NamingException("socket factory not found: " + factoryClass);
            }
            try {
                props.put(SwiftMQConnectionFactory.SOCKETFACTORY, factoryClass);
                props.put(SwiftMQConnectionFactory.HOSTNAME, jndiInfo.getHostname());
                props.put(SwiftMQConnectionFactory.PORT, String.valueOf(jndiInfo.getPort()));
                props.put(SwiftMQConnectionFactory.KEEPALIVEINTERVAL, String.valueOf(jndiInfo.getKeepalive()));
            } catch (Exception e) {
                throw new NamingException("unable to connect, exception = " + e);
            }
        }
        try {
            cf = SwiftMQConnectionFactory.create(props);
            createConnection();
            if (jndiInfo.getIdleclose() > 0)
                TimerRegistry.Singleton().addTimerListener(1000, this);
            closed.set(false);
        } catch (Exception e) {
            if (connection.get() != null) {
                try {
                    connection.get().close();
                } catch (Exception e1) {
                }
            }
            closed.set(true);
            if ((e instanceof JMSSecurityException) || (e instanceof InvalidVersionException))
                throw new StopRetryException(e.getMessage());
            throw new NamingException("unable to connect, exception = " + e);
        }
    }

    private void createConnection() throws JMSException {
        connection.set(cf.createConnection(jndiInfo.getUsername(), jndiInfo.getPassword()));
        session.set(connection.get().createSession(false, 0));
        producer.set(session.get().createProducer(null));
        connection.get().start();
        lastAccessTime.set(System.currentTimeMillis());
        if (debug)
            System.out.println(new Date() + " " + toString() + "/createConnection: " + env.get(Context.PROVIDER_URL));
    }

    private void checkConnection() throws JMSException {
        lock.writeLock().lock();
        try {
            if (!closed.get() && connection.get() == null)
                createConnection();
            lastAccessTime.set(System.currentTimeMillis());
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void performTimeAction(TimerEvent evt) {
        long delta = System.currentTimeMillis() - lastAccessTime.get() - jndiInfo.getIdleclose();
        if (debug)
            System.out.println(new Date() + " " + toString() + "/performTimeAction, connection=" + connection + ", delta=" + delta + ", lastAccessTime=" + lastAccessTime);
        if (connection.get() != null && lastAccessTime.get() + jndiInfo.getIdleclose() < System.currentTimeMillis()) {
            if (debug)
                System.out.println(new Date() + " " + toString() + "/createConnection, close connection (idle close)");
            ((SwiftMQConnection) connection.get()).cancel(true);
            connection.set(null);
        }
    }

    public Object addToEnvironment(String name, Object value)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    private Versioned createVersioned(int version, JNDIRequest request) throws Exception {
        DataByteArrayOutputStream dos = new DataByteArrayOutputStream();
        Dumpalizer.dump(dos, request);
        return new Versioned(version, dos.getBuffer(), dos.getCount());
    }

    private BytesMessageImpl createMessage(Versionable versionable, Destination replyTo) throws Exception {
        BytesMessageImpl msg = new BytesMessageImpl();
        versionable.transferToMessage(msg);
        if (replyTo != null)
            msg.setJMSReplyTo(replyTo);
        return msg;
    }

    public void bind(String name, Object obj)
            throws NamingException {
        if (closed.get())
            throw new NamingException("context is closed!");
        if (!(obj instanceof TemporaryTopicImpl || obj instanceof TemporaryQueueImpl))
            throw new OperationNotSupportedException("bind is only supported for TemporaryQueues/TemporaryTopics!");
        try {
            checkConnection();
            TemporaryTopic tt = session.get().createTemporaryTopic();
            MessageConsumer consumer = session.get().createConsumer(tt);

            Versionable versionable = new Versionable();
            versionable.addVersioned(Versions.JNDI_CURRENT, createVersioned(Versions.JNDI_CURRENT, new BindRequest(name, (QueueImpl) obj)),
                    "com.swiftmq.jndi.protocol.v" + Versions.JNDI_CURRENT + ".JNDIRequestFactory");
            BytesMessage request = createMessage(versionable, tt);
            producer.get().send(session.get().createTopic(JNDISwiftlet.JNDI_TOPIC), request, DeliveryMode.NON_PERSISTENT, MessageImpl.MAX_PRIORITY, 0);
            TextMessage reply = (TextMessage) consumer.receive();
            String text = reply.getText();
            consumer.close();
            tt.delete();
            if (text != null)
                throw new Exception(text);
        } catch (Exception e) {
            throw new NamingException("exception occurred during bind: " + e);
        }
    }

    public void bind(Name p0, Object p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void close()
            throws NamingException {
        try {
            if (connection.get() != null) {
                if (debug)
                    System.out.println(new Date() + " " + toString() + "/close");
                connection.get().close();
            }
        } catch (Exception ignored) {
        }
        if (!jndiInfo.isIntraVM() && jndiInfo.getIdleclose() > 0)
            TimerRegistry.Singleton().removeTimerListener(1000, this);
        closed.set(true);
    }

    public String composeName(String p0, String p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Name composeName(Name p0, Name p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Context createSubcontext(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Context createSubcontext(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void destroySubcontext(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void destroySubcontext(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Hashtable getEnvironment()
            throws NamingException {
        return env;
    }

    public String getNameInNamespace()
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NameParser getNameParser(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NameParser getNameParser(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NamingEnumeration list(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NamingEnumeration list(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NamingEnumeration listBindings(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public NamingEnumeration listBindings(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    private Destination getLookupDestination(Session session) throws JMSException {
        if (Versions.JMS_CURRENT < 630)
            return session.createTopic(JNDISwiftlet.JNDI_TOPIC);
        return session.createQueue(JNDISwiftlet.JNDI_QUEUE);
    }

    public Object lookup(String name)
            throws NamingException {
        if (closed.get())
            throw new NamingException("context is closed!");
        if (name == null)
            throw new OperationNotSupportedException("context cloning is not supported!");
        boolean connectionClosed = false;
        Object obj = null;
        try {
            checkConnection();
            TemporaryQueue tq = session.get().createTemporaryQueue();
            MessageConsumer consumer = session.get().createConsumer(tq);

            Versionable versionable = new Versionable();
            versionable.addVersioned(Versions.JNDI_CURRENT,
                    createVersioned(Versions.JNDI_CURRENT, new LookupRequest(name)),
                    "com.swiftmq.jndi.protocol.v" + Versions.JNDI_CURRENT + ".JNDIRequestFactory");
            BytesMessage request = createMessage(versionable, tq);
            producer.get().send(getLookupDestination(session.get()), request, DeliveryMode.NON_PERSISTENT, MessageImpl.MAX_PRIORITY, 0);
            MessageImpl reply = null;
            if (jndiInfo.getTimeout() == 0)
                reply = (MessageImpl) consumer.receive();
            else
                reply = (MessageImpl) consumer.receive(jndiInfo.getTimeout());
            if (reply != null) {
                if (reply instanceof ObjectMessageImpl)
                    obj = ((ObjectMessageImpl) reply).getObject();
                else {
                    BytesMessageImpl msg = (BytesMessageImpl) reply;
                    Versionable vreply = Versionable.toVersionable(msg);
                    vreply.selectVersions(Versions.cutAfterIndex(Versions.getSelectedIndex(Versions.JMS_CURRENT, Versions.JMS), Versions.JMS));
                    obj = vreply.createVersionedObject();
                }
            }
            if (!((SwiftMQMessageConsumer) consumer).isClosed()) {
                consumer.close();
                tq.delete();
            } else
                connectionClosed = true;
        } catch (Exception e) {
            throw new CommunicationException("exception occurred during lookup: " + e);
        }
        if (connectionClosed)
            throw new CommunicationException("Connection lost!");
        if (obj == null)
            throw new NameNotFoundException("Name '" + name + "' not found (timeout occured)!");
        return obj;
    }

    public Object lookup(Name name)
            throws NamingException {
        return lookup(name.get(0));
    }

    public Object lookupLink(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Object lookupLink(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void rebind(String name, Object obj)
            throws NamingException {
        if (closed.get())
            throw new NamingException("context is closed!");
        if (!(obj instanceof TemporaryTopicImpl || obj instanceof TemporaryQueueImpl))
            throw new OperationNotSupportedException("rebind is only supported for TemporaryQueues/TemporaryTopics!");
        try {
            checkConnection();
            TemporaryTopic tt = session.get().createTemporaryTopic();
            MessageConsumer consumer = session.get().createConsumer(tt);

            Versionable versionable = new Versionable();
            versionable.addVersioned(Versions.JNDI_CURRENT,
                    createVersioned(Versions.JNDI_CURRENT, new RebindRequest(name, (QueueImpl) obj)),
                    "com.swiftmq.jndi.protocol.v" + Versions.JNDI_CURRENT + ".JNDIRequestFactory");
            BytesMessage request = createMessage(versionable, tt);
            producer.get().send(session.get().createTopic(JNDISwiftlet.JNDI_TOPIC), request, DeliveryMode.NON_PERSISTENT, MessageImpl.MAX_PRIORITY, 0);
            TextMessage reply = (TextMessage) consumer.receive();
            String text = reply.getText();
            consumer.close();
            tt.delete();
            if (text != null)
                throw new Exception(text);
        } catch (Exception e) {
            throw new NamingException("exception occurred during rebind: " + e);
        }
    }

    public void rebind(Name p0, Object p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public Object removeFromEnvironment(String p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void rename(String p0, String p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void rename(Name p0, Name p1)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }

    public void unbind(String name)
            throws NamingException {
        if (closed.get())
            throw new NamingException("context is closed!");
        try {
            checkConnection();
            Versionable versionable = new Versionable();
            versionable.addVersioned(400, createVersioned(400, new UnbindRequest(name)), "com.swiftmq.jndi.protocol.v400.JNDIRequestFactory");
            BytesMessage request = createMessage(versionable, null);
            producer.get().send(session.get().createTopic(JNDISwiftlet.JNDI_TOPIC), request, DeliveryMode.NON_PERSISTENT, MessageImpl.MAX_PRIORITY, 0);
        } catch (Exception e) {
            throw new NamingException("exception occurred during unbind: " + e);
        }
    }

    public void unbind(Name p0)
            throws NamingException {
        throw new OperationNotSupportedException("not supported");
    }
}

