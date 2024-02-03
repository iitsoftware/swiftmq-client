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
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

public class OrderedSet {
    private final Set<Object> set = new LinkedHashSet<>();
    private int max;

    public OrderedSet(int max) {
        this.max = max;
    }

    public void increaseSize(int extend) {
        this.max += extend;
        System.out.println("increase size by " + extend + " to " + max);
    }

    public void decreaseSize(int reduce, int minSize) {
        this.max = Math.max(minSize, this.max - reduce);
        this.reduceToSIze(this.max);
    }

    public void resize(int newSize) {
        if (newSize > max)
            max = newSize;
        else if (newSize < max)
            reduceToSIze(newSize);
    }

    private void reduceToSIze(int newSize) {
        Iterator<Object> iterator = set.iterator();
        while (set.size() > newSize && iterator.hasNext()) {
            iterator.next();
            iterator.remove();
        }
    }

    public boolean add(Object o) {
        if (!set.add(o)) {
            return true; // Already in the set, exit method
        }

        if (set.size() > max) {
            Object first = set.iterator().next();
            set.remove(first);
        }
        return false;
    }

    public void addAll(Collection<Object> c) {
        for (Object o : c)
            add(o);
    }

    public Set<Object> getSet() {
        return set;
    }

    public boolean remove(Object o) {
        return set.remove(o);
    }

    public boolean contains(Object o) {
        return set.contains(o);
    }

    public int size() {
        return set.size();
    }

    public int getMax() {
        return max;
    }

    public void clear() {
        set.clear();
    }
}
