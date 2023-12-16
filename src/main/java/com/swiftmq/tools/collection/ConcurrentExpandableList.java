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

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ConcurrentExpandableList<T> extends ExpandableList<T> {
    private final ReadWriteLock rwLock = new ReentrantReadWriteLock();

    public ConcurrentExpandableList() {
        super();
    }

    @Override
    public int add(T element) {
        rwLock.writeLock().lock();
        try {
            return super.add(element);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public T get(int index) {
        rwLock.readLock().lock();
        try {
            return super.get(index);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void remove(int index) {
        rwLock.writeLock().lock();
        try {
            super.remove(index);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public int indexOf(T element) {
        rwLock.readLock().lock();
        try {
            return super.indexOf(element);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public int size() {
        rwLock.readLock().lock();
        try {
            return super.size();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void clear() {
        rwLock.writeLock().lock();
        try {
            super.clear();
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
