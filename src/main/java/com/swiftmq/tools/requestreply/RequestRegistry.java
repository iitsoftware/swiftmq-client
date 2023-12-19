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

package com.swiftmq.tools.requestreply;

import com.swiftmq.tools.collection.ConcurrentExpandableList;
import com.swiftmq.tools.concurrent.Semaphore;
import com.swiftmq.tools.timer.TimerEvent;
import com.swiftmq.tools.timer.TimerListener;
import com.swiftmq.tools.timer.TimerRegistry;

import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class RequestRegistry implements TimerListener {
    public static final long SWIFTMQ_REQUEST_TIMEOUT = Long.parseLong(System.getProperty("swiftmq.request.timeout", "60000"));
    public static final boolean DEBUG = Boolean.valueOf(System.getProperty("swiftmq.reconnect.debug", "false")).booleanValue();
    final static int TIMEOUT_CHECKINTERVAL = 10000;
    static boolean wrapPrivileged = false;
    ConcurrentExpandableList<Request> requestList = new ConcurrentExpandableList<>();
    RequestHandler requestHandler = null;
    boolean valid = true;
    boolean paused = false;
    volatile boolean requestTimeoutEnabled = true;
    Semaphore retrySem = null;
    Set<Request> retrySet = ConcurrentHashMap.newKeySet();
    String debugString = null;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public RequestRegistry() {
    }

    public RequestRegistry(String debugString) {
        this.debugString = debugString;
    }

    public static void setWrapPrivileged(boolean wrapPrivileged) {
        RequestRegistry.wrapPrivileged = wrapPrivileged;
    }

    public void setRequestTimeoutEnabled(boolean requestTimeoutEnabled) {
        this.requestTimeoutEnabled = requestTimeoutEnabled;
        if (requestTimeoutEnabled)
            TimerRegistry.Singleton().addTimerListener(TIMEOUT_CHECKINTERVAL, this);
    }

    public void setPaused(boolean paused) {
        lock.writeLock().lock();
        try {
            this.paused = paused;
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void setRequestHandler(RequestHandler requestHandler) {
        this.requestHandler = wrapPrivileged ? new PrivilegedRequestHandler(requestHandler) : requestHandler;
    }

    public Reply request(Request req) {
        if (!req.isReplyRequired()) {
            requestHandler.performRequest(req);
            return null;
        }

        // Process request as long as doRetry flag is set
        req._sem = new Semaphore(false); // not interruptable!
        do {
            processRequest(req);
            req._sem.waitHere();
            if (req.getReply() == null && req.isDoRetry()) {
                if (DEBUG) System.out.println(debugString + ": Retry: " + req);
                RequestRetryValidator validator = req.getValidator();
                if (validator != null) {
                    ValidationException validateException = null;
                    try {
                        validator.validate(req);
                    } catch (ValidationException e) {
                        validateException = e;
                    }

                    if (req.isCancelledByValidator()) {
                        Reply reply = req.createReply();
                        if (validateException != null) {
                            reply.setOk(false);
                            reply.setException(validateException);
                        } else
                            reply.setOk(true);
                        req.setReply(reply);
                        req.setDoRetry(false);
                        lock.writeLock().lock();
                        try {
                            retrySet.remove(req);
                            if (retrySet.size() == 0 && retrySem != null) {
                                retrySem.notifySingleWaiter();
                                retrySem = null;
                            }
                        } finally {
                            lock.writeLock().unlock();
                        }

                        if (DEBUG) System.out.println(debugString + ": Cancelled by Validator: " + req);
                    } else {
                        if (DEBUG) System.out.println(debugString + ": After validate: " + req);
                        req._sem.reset();
                    }
                } else {
                    if (DEBUG) System.out.println(debugString + ": No validator: " + req);
                    req._sem.reset();
                }
            }
            if (req.getReply() == null && req.isDoRetry()) {
                if (DEBUG) System.out.println(debugString + ": No Reply && isDoRetry: " + req);

            }
        } while (req.getReply() == null && req.isDoRetry());

        return req.getReply();
    }

    private void processRequest(Request req) {
        if (!valid)
            throw new RuntimeException("Invalid request (connection might be closed already)");

        req.setReply(null);
        req.setDoRetry(false);
        if (requestTimeoutEnabled)
            req.setTimeout(System.currentTimeMillis() + SWIFTMQ_REQUEST_TIMEOUT);

        // find next free index or add request to the end of the list
        req.setRequestNumber(requestList.add(req));

        // perform request via request handler
        if (!paused)
            requestHandler.performRequest(req);
        else {
            if (DEBUG) System.out.println(debugString + ": Paused, request NOT sent: " + req);
        }

    }

    private Semaphore setReplySynchronized(Reply reply) {
        lock.writeLock().lock();
        try {
            Semaphore sem = null;
            int reqNumber = reply.getRequestNumber();
            if (reqNumber < requestList.size()) {
                Request req = (Request) requestList.get(reqNumber);
                if (req != null) {
                    req.setReply(reply);
                    requestList.remove(reqNumber);
                    sem = req._sem;
                    if (req.isWasRetry()) {
                        if (DEBUG) System.out.println(debugString + ": Reply from Retry: " + reply);
                        retrySet.remove(req);
                        if (retrySet.size() == 0) {
                            retrySem.notifySingleWaiter();
                            retrySem = null;
                        }
                    }
                }
            } else
                System.out.println(debugString + ": reqNumber >= requestList.size(), " + reqNumber + ":" + requestList.size());
            return sem;
        } finally {
            lock.writeLock().unlock();
        }

    }

    // Required to force the thread to finish the synchronized setReply
    // before the sem is called...

    public void setReply(Reply reply) {
        Semaphore sem = setReplySynchronized(reply);
        if (sem != null)
            sem.notifySingleWaiter();
    }

    public void cancelAllRequests(TransportException exception, boolean valid) {
        lock.writeLock().lock();
        try {
            for (int i = 0; i < requestList.size(); i++) {
                Request req = requestList.get(i);
                if (req != null) {
                    Reply reply = req.createReply();
                    reply.setOk(false);
                    reply.setException(exception);
                    req.setReply(reply);
                    req._sem.notifySingleWaiter();
                }
            }
            requestList.clear();
            retrySet.clear();
            if (retrySem != null) {
                retrySem.notifySingleWaiter();
                retrySem = null;
            }
            this.valid = valid;
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void cancelRetryAllRequests() {
        lock.writeLock().lock();
        try {
            retrySet.clear();
            if (retrySem != null) {
                retrySem.notifySingleWaiter();
                retrySem = null;
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void retryAllRequests(Semaphore rSem) {
        lock.writeLock().lock();
        try {
            this.retrySem = rSem;
            retrySet.clear();
            for (int i = 0; i < requestList.size(); i++) {
                Request req = (Request) requestList.get(i);
                if (req != null) {
                    retrySet.add(req);
                    req.setDoRetry(true);
                    req._sem.notifySingleWaiter();
                }
            }
            requestList.clear();
            if (retrySet.isEmpty()) {
                retrySem.notifySingleWaiter();
                retrySem = null;
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void cancelAllRequests(TransportException exception) {
        cancelAllRequests(exception, true);
    }

    public void cancelRequest(Request request) {
        lock.writeLock().lock();
        try {
            int idx = requestList.indexOf(request);
            if (idx != -1) {
                requestList.remove(idx);
                request.setReply(null);
                request._sem.notifySingleWaiter();
                if (request.isDoRetry()) {
                    retrySet.remove(request);
                    if (retrySem != null) {
                        retrySem.notifySingleWaiter();
                        retrySem = null;
                    }
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void performTimeAction(TimerEvent evt) {
        lock.writeLock().lock();
        try {
            long actTime = System.currentTimeMillis();
            for (int i = 0; i < requestList.size(); i++) {
                Request req = (Request) requestList.get(i);
                if (req != null && req.getTimeout() != -1 && req.getTimeout() < actTime) {
                    requestList.remove(i);
                    Reply reply = req.createReply();
                    reply.setOk(false);
                    reply.setException(new TimeoutException("Request time out (" + SWIFTMQ_REQUEST_TIMEOUT + ") ms!"));
                    reply.setTimeout(true);
                    req.setReply(reply);
                    req._sem.notifySingleWaiter();
                }
            }
        } finally {
            lock.writeLock().unlock();
        }

    }

    public void close() {
        if (requestTimeoutEnabled)
            TimerRegistry.Singleton().removeTimerListener(TIMEOUT_CHECKINTERVAL, this);
    }

    private class PrivilegedRequestHandler implements RequestHandler {
        RequestHandler realHandler = null;

        public PrivilegedRequestHandler(RequestHandler realHandler) {
            this.realHandler = realHandler;
        }

        public void performRequest(Request request) {
            AccessController.doPrivileged(new PrivilegedRequestAction(request) {
                public Object run() {
                    realHandler.performRequest(myRequest);
                    return null;
                }
            });
        }
    }

    private abstract class PrivilegedRequestAction implements PrivilegedAction {
        Request myRequest = null;

        public PrivilegedRequestAction(Request request) {
            this.myRequest = request;
        }
    }
}

