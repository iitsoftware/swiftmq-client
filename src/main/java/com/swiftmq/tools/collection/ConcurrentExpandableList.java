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

import java.util.ArrayList;
import java.util.concurrent.locks.ReentrantLock;

public class ConcurrentExpandableList<T> {
    private final ArrayList<T> list;
    private final ReentrantLock lock = new ReentrantLock();

    public ConcurrentExpandableList() {
        this.list = new ArrayList<>();
    }

    public int add(T element) {
        lock.lock();
        try {
            // Find first null (free) index
            int freeIndex = list.indexOf(null);
            if (freeIndex == -1) {
                freeIndex = list.size();
                list.add(element);
            } else
                list.set(freeIndex, element);
            return freeIndex;
        } finally {
            lock.unlock();
        }
    }

    public T get(int index) {
        lock.lock();
        try {
            return list.get(index);
        } finally {
            lock.unlock();
        }
    }

    public void remove(int index) {
        lock.lock();
        try {
            list.set(index, null); // Set to null to mark as free
        } finally {
            lock.unlock();
        }
    }

    public int indexOf(T element) {
        lock.lock();
        try {
            return list.indexOf(element);
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        lock.lock();
        try {
            return list.size();
        } finally {
            lock.unlock();
        }
    }

    public void clear() {
        lock.lock();
        try {
            list.clear();
        } finally {
            lock.unlock();
        }
    }
}
