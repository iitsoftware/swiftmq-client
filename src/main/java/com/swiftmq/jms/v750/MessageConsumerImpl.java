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

package com.swiftmq.jms.v750;

import com.swiftmq.jms.ExceptionConverter;
import com.swiftmq.jms.MessageImpl;
import com.swiftmq.jms.SwiftMQMessageConsumer;
import com.swiftmq.jms.smqp.v750.*;
import com.swiftmq.swiftlet.queue.MessageEntry;
import com.swiftmq.swiftlet.queue.MessageIndex;
import com.swiftmq.tools.collection.RingBuffer;
import com.swiftmq.tools.collection.RingBufferThreadsafe;
import com.swiftmq.tools.concurrent.Semaphore;
import com.swiftmq.tools.requestreply.*;
import com.swiftmq.tools.util.IdGenerator;
import com.swiftmq.tools.util.UninterruptableWaiter;

import javax.jms.IllegalStateException;
import javax.jms.*;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class MessageConsumerImpl implements MessageConsumer, SwiftMQMessageConsumer, Recreatable, RequestRetryValidator {
    final String uniqueConsumerId = IdGenerator.getInstance().nextId('/');
    final AtomicBoolean closed = new AtomicBoolean(false);
    final AtomicInteger consumerId = new AtomicInteger();
    final AtomicBoolean transacted = new AtomicBoolean(false);
    final AtomicInteger acknowledgeMode = new AtomicInteger();
    RequestRegistry requestRegistry = null;
    String messageSelector = null;
    MessageListener messageListener = null;
    SessionImpl mySession = null;
    int serverQueueConsumerId = -1;
    boolean useThreadContextCL = false;
    final AtomicBoolean cancelled = new AtomicBoolean(false);
    RingBuffer messageCache = null;
    final AtomicBoolean doAck = new AtomicBoolean(false);
    final AtomicBoolean reportDelivered = new AtomicBoolean(false);
    final AtomicBoolean recordLog = new AtomicBoolean(true);
    final AtomicBoolean receiverWaiting = new AtomicBoolean(false);
    final AtomicBoolean wasRecovered = new AtomicBoolean(false);
    final AtomicBoolean fillCachePending = new AtomicBoolean(false);
    final AtomicBoolean receiveNoWaitFirstCall = new AtomicBoolean(true);
    final AtomicBoolean consumerStarted = new AtomicBoolean(false);
    final Lock lock = new ReentrantLock();
    final Lock fillCacheLock = new ReentrantLock();
    final UninterruptableWaiter waiter = new UninterruptableWaiter(lock);

    public MessageConsumerImpl(boolean transacted, int acknowledgeMode, RequestRegistry requestRegistry,
                               String messageSelector, SessionImpl session) {
        this.transacted.set(transacted);
        this.acknowledgeMode.set(acknowledgeMode);
        this.requestRegistry = requestRegistry;
        this.messageSelector = messageSelector;
        this.mySession = session;
        useThreadContextCL = mySession.getMyConnection().isUseThreadContextCL();
        reportDelivered.set(transacted || acknowledgeMode == Session.CLIENT_ACKNOWLEDGE);
        messageCache = new RingBufferThreadsafe(mySession.getMyConnection().getSmqpConsumerCacheSize());
    }

    public Request getRecreateRequest() {
        return null;
    }

    public void setRecreateReply(Reply reply) {

    }

    public List getRecreatables() {
        return null;
    }

    public void validate(Request request) throws ValidationException {
        request.setDispatchId(mySession.dispatchId);
        if (request instanceof CloseConsumerRequest) {
            CloseConsumerRequest r = (CloseConsumerRequest) request;
            r.setSessionDispatchId(mySession.dispatchId);
            r.setQueueConsumerId(serverQueueConsumerId);
        } else {
            request.setCancelledByValidator(true);
        }
    }

    protected void verifyState() throws JMSException {
        if (closed.get()) {
            throw new javax.jms.IllegalStateException("Message consumer is closed");
        }

        mySession.verifyState();
    }

    public boolean isConsumerStarted() {
        return consumerStarted.get();
    }

    void setWasRecovered(boolean wasRecovered) {
        this.wasRecovered.set(wasRecovered);
    }

    void setDoAck(boolean doAck) {
        this.doAck.set(doAck);
    }

    public void setRecordLog(boolean recordLog) {
        this.recordLog.set(recordLog);
    }

    void addToCache(AsyncMessageDeliveryRequest request) {
        if (isClosed())
            return;
        if (request.isRequiresRestart())
            fillCachePending.set(false);
        messageCache.add(request);
    }

    void addToCache(AsyncMessageDeliveryRequest[] requests, boolean lastRestartRequired) {
        for (int i = 0; i < requests.length; i++) {
            if (lastRestartRequired && i == requests.length - 1)
                requests[i].setRequiresRestart(true);
            addToCache(requests[i]);
        }
    }

    boolean invokeConsumer() {
        boolean shouldSignal = false;

        // Locking block to safely update receiverWaiting
        lock.lock();
        try {
            if (messageCache.getSize() > 0) {
                if (messageListener == null) {
                    if (receiverWaiting.get()) {
                        receiverWaiting.set(false);
                        shouldSignal = true; // Set flag to signal after releasing the lock
                    }
                } else {
                    invokeMessageListener();
                }
            }
        } finally {
            lock.unlock();
        }

        // Signal outside of the lock
        if (shouldSignal) {
            waiter.signal();
        }
        return messageCache.getSize() > 0 && (messageListener != null || receiverWaiting.get()) && !isClosed();
    }

    void fillCache(boolean force) {
        fillCacheLock.lock();
        try {
            if (isClosed() || fillCachePending.get() && !force)
            return;
            fillCachePending.set(true);
            consumerStarted.set(true);
            requestRegistry.request(new StartConsumerRequest(this, mySession.dispatchId, serverQueueConsumerId,
                    mySession.getMyDispatchId(), consumerId.get(), mySession.getMyConnection().getSmqpConsumerCacheSize(), mySession.getMyConnection().getSmqpConsumerCacheSizeKB()));
        } finally {
            fillCacheLock.unlock();
        }
    }

    void fillCache() {
        fillCache(false);
    }

    void clearCache() {
        fillCachePending.set(false);
        messageCache.clear();
    }

    public boolean isClosed() {
        return closed.get() || mySession.isClosed();
    }

    int getConsumerId() {
        return consumerId.get();
    }

    void setConsumerId(int id) {
        consumerId.set(id);
    }

    int getServerQueueConsumerId() {
        return serverQueueConsumerId;
    }

    void setServerQueueConsumerId(int id) {
        serverQueueConsumerId = id;
    }

    public String getMessageSelector() throws JMSException {
        verifyState();
        return messageSelector;
    }

    public MessageListener getMessageListener() throws JMSException {
        verifyState();
        return (messageListener);
    }

    public void setMessageListener(MessageListener listener) throws JMSException {
        verifyState();
        if (listener != null && !consumerStarted.get())
            fillCache();
        messageListener = listener;
        if (listener != null)
            mySession.triggerInvocation();
    }

    private void invokeMessageListener() {
        lock.lock();
        try {
            if (isClosed())
                return;
            AsyncMessageDeliveryRequest request = (AsyncMessageDeliveryRequest) messageCache.remove();
            MessageEntry messageEntry = request.getMessageEntry();
            MessageImpl msg = messageEntry.getMessage();
            messageEntry.moveMessageAttributes();
            MessageIndex msgIndex = msg.getMessageIndex();
            msg.setMessageConsumerImpl(this);
            try {
                msg.reset();
            } catch (JMSException e) {
                e.printStackTrace();
            }
            msg.setReadOnly(true);
            msg.setUseThreadContextCL(useThreadContextCL);
            String id = null;
            boolean duplicate = false;
            if (recordLog.get()) {
                id = SessionImpl.buildId(uniqueConsumerId, msg);
                duplicate = mySession.myConnection.isDuplicateMessageDetection() && mySession.isDuplicate(id);
            }
            if (reportDelivered.get())
                reportDelivered(msg, false);
            try {
                if (!duplicate) {
                    if (recordLog.get() && mySession.myConnection.isDuplicateMessageDetection())
                        mySession.addCurrentTxLog(id);
                    mySession.withinOnMessage = true;
                    mySession.onMessageMessage = msg;
                    mySession.onMessageConsumer = this;
                    mySession.setTxCancelled(false);
                    messageListener.onMessage(msg);
                    mySession.onMessageMessage = null;
                    mySession.onMessageConsumer = null;
                    mySession.withinOnMessage = false;
                    if (mySession.isTxCancelled() || mySession.acknowledgeMode == Session.CLIENT_ACKNOWLEDGE && msg.isCancelled()) {
                        wasRecovered.set(false);
                        return;
                    }
                }
            } catch (RuntimeException e) {
                System.err.println("ERROR! MessageListener throws RuntimeException, shutting down consumer!");
                e.printStackTrace();
                try {
                    close(e.toString());
                } catch (JMSException e1) {
                }
                return;
            }
            if (!wasRecovered.get()) {
                if (request.isRequiresRestart())
                    fillCache();
                if (doAck.get()) {
                    try {
                        acknowledgeMessage(msgIndex, false);
                    } catch (JMSException e) {
                        throw new RuntimeException(e);
                    }
                }
            } else
                wasRecovered.set(false);
        } finally {
            lock.unlock();
        }

    }

    protected void reportDelivered(Message message, boolean duplicate) {
        try {
            MessageIndex messageIndex = ((MessageImpl) message).getMessageIndex();
            requestRegistry.request(new MessageDeliveredRequest(this, mySession.dispatchId, serverQueueConsumerId, messageIndex, duplicate));
        } catch (Exception e) {
        }
    }

    public boolean acknowledgeMessage(MessageImpl message) throws JMSException {
        if (transacted.get())
            throw new IllegalStateException("acknowledge not possible, session is transacted!");
        if (!(acknowledgeMode.get() == Session.CLIENT_ACKNOWLEDGE))
            throw new IllegalStateException("acknowledge not possible, session was not created in mode CLIENT_ACKNOWLEDGE!");
        return acknowledgeMessage(message.getMessageIndex(), true);
    }

    private boolean acknowledgeMessage(MessageIndex messageIndex, boolean replyRequired) throws JMSException {
        if (isClosed())
            throw new javax.jms.IllegalStateException("Connection is closed");

        Reply reply = null;
        boolean cancelled = false;
        try {
            if (messageIndex == null)
                throw new JMSException("Unable to acknowledge message - missing message key!");

            AcknowledgeMessageRequest request = new AcknowledgeMessageRequest(this, mySession.dispatchId, serverQueueConsumerId, messageIndex);
            request.setReplyRequired(replyRequired);
            reply = requestRegistry.request(request);
            if (request.isCancelledByValidator()) {
                cancelled = true;
                mySession.addCurrentTxToDuplicateLog();
            }
            mySession.removeCurrentTxFromRollbackLog();
            mySession.clearCurrentTxLog();
        } catch (Exception e) {
            if (isClosed()) throw new javax.jms.IllegalStateException("Connection is closed: " + e);
            throw ExceptionConverter.convert(e);
        }

        if (replyRequired && !reply.isOk()) {
            if (isClosed()) throw new javax.jms.IllegalStateException("Connection is closed: " + reply.getException());
            throw ExceptionConverter.convert(reply.getException());
        }
        return cancelled;
    }

    Message receiveMessage(boolean block, long timeout) throws JMSException {
        lock.lock();
        try {
            verifyState();

            if (messageListener != null) {
                throw new JMSException("receive not allowed while a message listener has been set");
            }
            boolean wasDuplicate = false;
            boolean wasInvalidConnectionId = false;
            MessageImpl msg = null;
            String id = null;
            do {
                wasDuplicate = false;
                wasInvalidConnectionId = false;
                if (!consumerStarted.get())
                    fillCache();
                do {
                    if (messageCache.getSize() == 0) {
                        if (block) {
                            receiverWaiting.set(true);
                            if (timeout == 0) {
                                waiter.doWait();
                            } else {
                                long to = timeout;
                                do {
                                    long startWait = System.currentTimeMillis();
                                    waiter.doWait(to);
                                    long delta = System.currentTimeMillis() - startWait;
                                    to -= delta;
                                }
                                while (to > 0 && messageCache.getSize() == 0 && fillCachePending.get() && !cancelled.get() && !isClosed());
                            }
                        } else {
                            if (fillCachePending.get() && receiveNoWaitFirstCall.get()) {
                                receiverWaiting.set(true);
                                waiter.doWait(1000);
                            }
                        }
                        if (cancelled.get())
                            return null;
                    }
                } while (mySession.resetInProgress);
                receiverWaiting.set(false);
                if (messageCache.getSize() == 0 || isClosed())
                    return null;

                AsyncMessageDeliveryRequest request = (AsyncMessageDeliveryRequest) messageCache.remove();
                if (request.getConnectionId() != mySession.myConnection.getConnectionId()) {
                    wasInvalidConnectionId = true;
                } else {
                    MessageEntry messageEntry = request.getMessageEntry();
                    msg = messageEntry.getMessage();
                    messageEntry.moveMessageAttributes();
                    msg.setMessageConsumerImpl(this);
                    msg.reset();
                    msg.setReadOnly(true);
                    msg.setUseThreadContextCL(useThreadContextCL);
                    if (request.isRequiresRestart())
                        fillCache();
                    if (recordLog.get()) {
                        id = SessionImpl.buildId(uniqueConsumerId, msg);
                        wasDuplicate = mySession.myConnection.isDuplicateMessageDetection() && mySession.isDuplicate(id);
                    }
                    if (reportDelivered.get())
                        reportDelivered(msg, false);
                    if (doAck.get()) {
                        try {
                            acknowledgeMessage(msg.getMessageIndex(), false);
                        } catch (JMSException ignored) {
                        }
                    }
                    if (wasDuplicate) {
                        msg = null;
                    }
                }
            } while (wasDuplicate || wasInvalidConnectionId);

            if (recordLog.get() && mySession.myConnection.isDuplicateMessageDetection())
                mySession.addCurrentTxLog(id);
            return msg;
        } finally {
            lock.unlock();
        }

    }

    public Message receive() throws JMSException {
        return receiveMessage(true, 0);
    }

    public Message receive(long timeOut) throws JMSException {
        return receiveMessage(true, timeOut);
    }

    public Message receiveNoWait() throws JMSException {
        Message msg = receiveMessage(false, 0);
        receiveNoWaitFirstCall.set(false);
        return msg;
    }

    void close(String exception) throws JMSException {
        lock.lock();
        try {
            if (isClosed())
                return;
            closed.set(true);
            messageCache.clear();
        } finally {
            lock.unlock();
        }

        waiter.signal();

        Reply reply = null;

        // must be released by the connection!
        try {
            reply = requestRegistry.request(new CloseConsumerRequest(this, mySession.dispatchId, mySession.dispatchId, serverQueueConsumerId, exception));
        } catch (Exception e) {
            throw ExceptionConverter.convert(e);
        }

        if (!reply.isOk()) {
            throw ExceptionConverter.convert(reply.getException());
        }
        mySession.removeMessageConsumerImpl(this);
    }

    public void close() throws JMSException {
        if (closed.get())
            return;
        if (!mySession.isSessionStarted()) {
            close(null);
            return;
        }
        CloseConsumer request = new CloseConsumer(consumerId.get());
        request._sem = new Semaphore();
        mySession.serviceRequest(request);
        request._sem.waitHere();
    }

    void cancel() {
        lock.lock();
        try {
            cancelled.set(true);
            closed.set(true);
            messageCache.clear();
        } finally {
            lock.unlock();
        }

        waiter.signal();
    }

}



