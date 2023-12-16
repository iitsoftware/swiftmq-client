package com.swiftmq.tools.collection;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Queue;

public class ExpandableList<T> {
    protected final ArrayList<T> list;
    protected final Queue<Integer> freeIndexes;

    public ExpandableList() {
        this.list = new ArrayList<>();
        this.freeIndexes = new LinkedList<>();
    }

    public int add(T element) {
        Integer freeIndex;
        if (!freeIndexes.isEmpty()) {
            freeIndex = freeIndexes.poll();
            list.set(freeIndex, element);
        } else {
            freeIndex = list.size();
            list.add(element);
        }
        return freeIndex;
    }

    public T get(int index) {
        return list.get(index);
    }

    public void remove(int index) {
        list.set(index, null);
        freeIndexes.offer(index);
    }

    public int indexOf(T element) {
        return list.indexOf(element);
    }

    public int size() {
        return list.size();
    }

    public void clear() {
        list.clear();
        freeIndexes.clear();
    }
}
