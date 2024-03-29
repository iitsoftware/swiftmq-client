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

package com.swiftmq.tools.queue;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class SingleProcessorQueue {
    private Object[] elements;
    private int first = 0;
    private volatile int size = 0;
    private final int bucketSize;
    private volatile boolean closed = false;
    private volatile boolean started = false;
    private volatile boolean processorActive = false;
    private Object[] bulkWrapper = null;
    private Object[] passDirectWrapper = new Object[1];
    private Object[] nullArray = null;
    private ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public SingleProcessorQueue(int bucketSize, int bulkSize) {
        elements = new Object[bucketSize];
        this.bucketSize = bucketSize;
        bulkWrapper = new Object[bulkSize == -1 ? bucketSize : bulkSize];
        nullArray = new Object[bulkSize == -1 ? bucketSize : bulkSize];
    }

    public SingleProcessorQueue(int bulkSize) {
        this(32, bulkSize);
    }

    public SingleProcessorQueue() {
        this(32, 1);
    }

    public int getSize() {
        return size;
    }

    public boolean isClosed() {
        return closed;
    }

    public boolean isStarted() {
        return started;
    }

    private void doEnqueue(Object obj) {
        if (size == elements.length) {
            int newSize = elements.length + bucketSize;
            Object[] newElements = new Object[newSize];
            int n = elements.length - first;
            System.arraycopy(elements, first, newElements, 0, n);
            if (first != 0)
                System.arraycopy(elements, 0, newElements, n, first);
            elements = newElements;
            first = 0;
        }
        elements[(first + size) % elements.length] = obj;
        size++;
    }

    private void clearBulk() {
        System.arraycopy(nullArray, 0, bulkWrapper, 0, bulkWrapper.length); // To force GC!
    }

    public void enqueue(Object obj) {
        lock.writeLock().lock();
        try {
            if (closed)
                return;
            doEnqueue(obj);
            if (!processorActive && started) {
                startProcessor();
                processorActive = true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public boolean dequeue() {
        int n = 0;
        lock.writeLock().lock();
        try {
            if (closed || !started || size == 0) {
                processorActive = false;
                return false;
            }
            n = getBulk();
        } finally {
            lock.writeLock().unlock();
        }
        process(bulkWrapper, n);
        clearBulk();
        boolean rc;
        lock.writeLock().lock();
        try {
            if (size == 0) {
                processorActive = false;
            }
            rc = size > 0;
        } finally {
            lock.writeLock().unlock();
        }
        return rc;
    }

    private int getBulk() {
        int n = 0;
        int bl = Math.min(size, bulkWrapper.length);
        for (int i = 0; i < bl; i++) {
            if (size > 0) {
                bulkWrapper[n++] = elements[first];
                elements[first] = null;
                first++;
                size--;
                if (first == elements.length)
                    first = 0;
            } else {
                break;
            }
        }
        return n;
    }

    protected abstract void startProcessor();

    protected abstract void process(Object[] bulk, int n);

    public void startQueue() {
        lock.writeLock().lock();
        try {
            closed = false;
            started = true;
            if (!processorActive && size > 0) {
                startProcessor();
                processorActive = true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void stopQueue() {
        lock.writeLock().lock();
        try {
            started = false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    protected boolean validateClearElement(Object obj) {
        return true;
    }

    public void clear() {
        lock.writeLock().lock();
        try {
            int n = 0;
            for (int i = 0; i < elements.length; i++) {
                if (elements[i] != null) {
                    if (validateClearElement(elements[i]))
                        elements[i] = null;
                    else
                        n++;
                }
            }
            if (n > 0) {
                Object[] o = new Object[elements.length];
                int m = 0;
                for (int i = 0; i < elements.length; i++) {
                    Object obj = elements[first++];
                    if (obj != null)
                        o[m++] = obj;
                    if (first == elements.length)
                        first = 0;
                }
                elements = o;
                size = m;
                first = 0;
            } else {
                size = 0;
                first = 0;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }

    public void close() {
        lock.writeLock().lock();
        try {
            closed = true;
            elements = null;
        } finally {
            lock.writeLock().unlock();
        }
    }
}

