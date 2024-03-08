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

package com.swiftmq.swiftlet.queue;

import com.swiftmq.mgmt.EntityList;

import java.util.SortedSet;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A QueueBrowser is created by the QueueManager. It provides a method for
 * browsing queue messages outside a transaction.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2002, All Rights Reserved
 * @see QueueManager#createQueueBrowser
 */
public class QueueBrowser extends QueueHandler {
    Selector selector = null;
    SortedSet queueIndex = null;
    MessageIndex lastMessageIndex = null;
    EntityList browserEntityList = null;
    int viewId = -1;
    ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new QueueBrowser
     *
     * @param activeQueue       the active queue
     * @param selector          an optional selector
     * @param browserEntityList the browser usage entity list
     */
    public QueueBrowser(ActiveQueue activeQueue, Selector selector, EntityList browserEntityList) {
        super(activeQueue.getAbstractQueue());
        this.selector = selector;
        this.browserEntityList = browserEntityList;
    }

    public void setLastMessageIndex(MessageIndex lastMessageIndex) {
        lock.writeLock().lock();
        try {
            this.lastMessageIndex = lastMessageIndex;
        } finally {
            lock.writeLock().unlock();
        }

    }

    private MessageIndex getNextEntry(MessageIndex storeId) {
        MessageIndex rMessageIndex = null;
        if (storeId == null) {
            if (queueIndex.size() > 0)
                rMessageIndex = (MessageIndex) queueIndex.first();
        } else {
            for (Object index : queueIndex) {
                MessageIndex s = (MessageIndex) index;
                if (s.compareTo(storeId) > 0) {
                    rMessageIndex = s;
                    break;
                }
            }
        }
        return rMessageIndex;
    }


    /**
     * Reset the browser.
     * It will start at the beginning next time a message is fetched.
     */
    public void resetBrowser() {
        lock.writeLock().lock();
        try {
            queueIndex = null;
            lastMessageIndex = null;
            if (viewId != -1)
                abstractQueue.deleteView(viewId);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Get the next available message from the queue.
     *
     * @return message or null
     * @throws QueueException              thrown by the queue
     * @throws QueueHandlerClosedException if the handler is closed
     */
    public MessageEntry getNextMessage()
            throws QueueException, QueueHandlerClosedException {
        lock.writeLock().lock();
        try {
            verifyQueueHandlerState();
            MessageEntry me = null;
            if (queueIndex == null) {
                if (selector == null)
                    queueIndex = abstractQueue.getQueueIndex();
                else {
                    viewId = abstractQueue.createView(selector);
                    queueIndex = abstractQueue.getQueueIndex(viewId);
                }
            }
            boolean found = false;
            while (!found) {
                MessageIndex s = getNextEntry(lastMessageIndex);
                if (s == null)
                    found = true;
                else {
                    lastMessageIndex = s;
                    MessageEntry m = abstractQueue.getMessageByIndex(s);
                    if (m != null) {
                        me = m;
                        found = true;
                    }
                }
            }
            return me;
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Close the queue browser
     *
     * @throws QueueException              thrown by the queue
     * @throws QueueHandlerClosedException if the browser is already closed
     */
    public void close()
            throws QueueException, QueueHandlerClosedException {
        lock.writeLock().lock();
        try {
            super.close();
            if (browserEntityList != null) {
                browserEntityList.removeDynamicEntity(this);
                browserEntityList = null;
            }
            queueIndex = null;
            lastMessageIndex = null;
            if (viewId != -1)
                abstractQueue.deleteView(viewId);
        } finally {
            lock.writeLock().unlock();
        }

    }
}

