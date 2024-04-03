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

package com.swiftmq.swiftlet.auth;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * A ResourceLimitGroup contains the maximum values for connections, sessions per connection,
 * temp. queues/topics per connection, producers and consumers per connection a user can obtain from a SwiftMQ router.
 *
 * @author IIT GmbH, Bremen/Germany, Copyright (c) 2000-2004, All Rights Reserved
 */
public class ResourceLimitGroup {
    String name = null;
    final AtomicInteger maxConnections = new AtomicInteger();
    final AtomicInteger maxSessions = new AtomicInteger();
    final AtomicInteger maxTempQueues = new AtomicInteger();
    final AtomicInteger maxProducers = new AtomicInteger();
    final AtomicInteger maxConsumers = new AtomicInteger();
    final AtomicInteger sessions = new AtomicInteger();
    final AtomicInteger tempQueues = new AtomicInteger();
    final AtomicInteger producers = new AtomicInteger();
    final AtomicInteger consumers = new AtomicInteger();

    final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    /**
     * Creates a new ResourceLimitGroup.
     *
     * @param name           Name of the group
     * @param maxConnections max. connections
     * @param maxSessions    max. sessions
     * @param maxTempQueues  max. temp. queues/topics
     * @param maxProducers   max. producers
     * @param maxConsumers   max. consumers
     */
    public ResourceLimitGroup(String name, int maxConnections, int maxSessions, int maxTempQueues, int maxProducers, int maxConsumers) {
        this.name = name;
        this.maxConnections.set(maxConnections);
        this.maxSessions.set(maxSessions);
        this.maxTempQueues.set(maxTempQueues);
        this.maxProducers.set(maxProducers);
        this.maxConsumers.set(maxConsumers);
    }

    /**
     * Returns the group name
     *
     * @return group name
     */
    public String getName() {
        return (name);
    }

    /**
     * Returns the max. connections
     *
     * @return max. connections
     */
    public int getMaxConnections() {
        return maxConnections.get();
    }

    /**
     * Set the max. sessions
     *
     * @param maxConnections max. connections
     */
    public void setMaxConnections(int maxConnections) {
        this.maxConnections.set(maxConnections);
    }

    public void verifyConnectionLimit(int n) throws ResourceLimitException {
        lock.readLock().lock();
        try {
            if (maxConnections.get() != -1 && n >= maxConnections.get())
                throw new ResourceLimitException("Resource Limit Group '" + name + "': max connections exceeded. Resource limit is: " + maxConnections);
        } finally {
            lock.readLock().unlock();
        }

    }

    /**
     * Returns the max. sessions
     *
     * @return max. sessions
     */
    public int getMaxSessions() {
        return (maxSessions.get());
    }

    /**
     * Set the max. sessions
     *
     * @param maxSessions max. sessions
     */
    public void setMaxSessions(int maxSessions) {
        this.maxSessions.set(maxSessions);
    }

    /**
     * Returns the max. temp queues/topics
     *
     * @return max. temp queues/topics
     */
    public int getMaxTempQueues() {
        return (maxTempQueues.get());
    }

    /**
     * Set the max. temp queues/topics
     *
     * @param maxTempQueues max. temp queues/topics
     */
    public void setMaxTempQueues(int maxTempQueues) {
        this.maxTempQueues.set(maxTempQueues);
    }

    /**
     * Returns the max. producers
     *
     * @return max. producers
     */
    public int getMaxProducers() {
        return (maxProducers.get());
    }

    /**
     * Set the max. producers
     *
     * @param maxProducers max. producers
     */
    public void setMaxProducers(int maxProducers) {
        this.maxProducers.set(maxProducers);
    }

    /**
     * Returns the max. consumers
     *
     * @return max. consumers
     */
    public int getMaxConsumers() {
        return (maxConsumers.get());
    }

    /**
     * Set the max. consumers
     *
     * @param maxConsumers max. consumers
     */
    public void setMaxConsumers(int maxConsumers) {
        this.maxConsumers.set(maxConsumers);
    }

    /**
     * Increments the number of sessions in use
     *
     * @throws ResourceLimitException if max. sessions is exceeded
     */
    public void incSessions()
            throws ResourceLimitException {
        lock.writeLock().lock();
        try {
            if (sessions.get() >= maxSessions.get())
                throw new ResourceLimitException("Resource Limit Group '" + name + "': max sessions per connection exceeded. Resource limit is: " + maxSessions);
            sessions.getAndIncrement();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Decrements the number of sessions in use
     */
    public void decSessions() {
        lock.writeLock().lock();
        try {
            sessions.getAndDecrement();
            if (sessions.get() < 0)
                sessions.set(0);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the number of sessions in use
     *
     * @return number of sessions in use
     */
    public int getSessions() {
        return (sessions.get());
    }

    /**
     * Increments the number of temp. queues/topics in use
     *
     * @throws ResourceLimitException if max. temp. queues/topics is exceeded
     */
    public void incTempQueues()
            throws ResourceLimitException {
        lock.writeLock().lock();
        try {
            if (tempQueues.get() >= maxTempQueues.get())
                throw new ResourceLimitException("Resource Limit Group '" + name + "': max temp. queues per connection exceeded. Resource limit is: " + maxTempQueues);
            tempQueues.getAndIncrement();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Decrements the number of temp. queues/topics in use
     */
    public void decTempQueues() {
        lock.writeLock().lock();
        try {
            tempQueues.getAndDecrement();
            if (tempQueues.get() < 0)
                tempQueues.set(0);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the number of temp. queues/topics in use
     *
     * @return number of temp. queues/topics in use
     */
    public int getTempQueues() {
        return (tempQueues.get());
    }

    /**
     * Increments the number of producers in use
     *
     * @throws ResourceLimitException if max. producers is exceeded
     */
    public void incProducers()
            throws ResourceLimitException {
        lock.writeLock().lock();
        try {
            if (producers.get() >= maxProducers.get())
                throw new ResourceLimitException("Resource Limit Group '" + name + "': max producers per connection exceeded. Resource limit is: " + maxProducers);
            producers.getAndIncrement();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Decrements the number of producers in use
     */
    public void decProducers() {
        lock.writeLock().lock();
        try {
            producers.getAndDecrement();
            if (producers.get() < 0)
                producers.set(0);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the number of producers in use
     *
     * @return number of producers in use
     */
    public int getProducers() {
        return (producers.get());
    }

    /**
     * Increments the number of consumers in use
     *
     * @throws ResourceLimitException if max. consumers is exceeded
     */
    public void incConsumers()
            throws ResourceLimitException {
        lock.writeLock().lock();
        try {
            if (consumers.get() >= maxConsumers.get())
                throw new ResourceLimitException("Resource Limit Group '" + name + "': max consumers per connection exceeded. Resource limit is: " + maxConsumers);
            consumers.getAndIncrement();
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Decrements the number of consumers in use
     */
    public void decConsumers() {
        lock.writeLock().lock();
        try {
            consumers.getAndDecrement();
            if (consumers.get() < 0)
                consumers.set(0);
        } finally {
            lock.writeLock().unlock();
        }

    }

    /**
     * Returns the number of consumers in use
     *
     * @return number of consumers in use
     */
    public int getConsumers() {
        return (consumers.get());
    }

    public String toString() {
        StringBuffer b = new StringBuffer("[ResourceLimitGroup ");
        b.append(name);
        b.append(", maxConnections=");
        b.append(maxConnections.get());
        b.append(", maxSessions=");
        b.append(maxSessions.get());
        b.append(", sessions=");
        b.append(sessions.get());
        b.append(", maxTempQueues=");
        b.append(maxTempQueues.get());
        b.append(", tempQueues=");
        b.append(tempQueues.get());
        b.append(", maxProducers=");
        b.append(maxProducers.get());
        b.append(", producers=");
        b.append(producers.get());
        b.append(", maxConsumers=");
        b.append(maxConsumers.get());
        b.append(", consumers=");
        b.append(consumers.get());
        b.append("]");
        return b.toString();
    }
}

