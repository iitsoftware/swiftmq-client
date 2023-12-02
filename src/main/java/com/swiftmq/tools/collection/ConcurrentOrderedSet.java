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
package com.swiftmq.tools.collection;
import java.util.Collection;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentOrderedSet extends OrderedSet {
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    public ConcurrentOrderedSet(int max) {
        super(max);
    }

    @Override
    public boolean add(Object o) {
        lock.writeLock().lock();
        try {
            return super.add(o);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void addAll(Collection<Object> c) {
        lock.writeLock().lock();
        try {
            super.addAll(c);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean remove(Object o) {
        lock.writeLock().lock();
        try {
            return super.remove(o);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean contains(Object o) {
        lock.readLock().lock();
        try {
            return super.contains(o);
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        lock.writeLock().lock();
        try {
            super.clear();
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void increaseSize(int extend) {
        lock.writeLock().lock();
        try {
            super.increaseSize(extend);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void decreaseSize(int reduce, int minSize) {
        lock.writeLock().lock();
        try {
            super.decreaseSize(reduce, minSize);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public void resize(int newSize) {
        lock.writeLock().lock();
        try {
            super.resize(newSize);
        } finally {
            lock.writeLock().unlock();
        }
    }
}
