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
    }

    public void decreaseSize(int reduce, int minSize) {
        this.max = Math.max(minSize, this.max - reduce);
        this.resize(this.max);
    }

    public void resize(int newSize) {
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
