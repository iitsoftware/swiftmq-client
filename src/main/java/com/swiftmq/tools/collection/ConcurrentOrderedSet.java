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
    public void add(Object o) {
        lock.writeLock().lock();
        try {
            super.add(o);
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

    public boolean containsOrAdd(String id) {
        lock.writeLock().lock();
        try {
            return super.containsOrAdd(id);
        } finally {
            lock.writeLock().unlock();
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
