package com.swiftmq.tools.gc;

import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

/**
 * The WeakPool class provides a generic object pooling mechanism using weak references,
 * allowing pooled objects to be garbage-collected when memory is needed. This pool is designed
 * to reduce the overhead of object creation and destruction, particularly for objects that
 * are expensive to create and are used frequently, but should not prevent the JVM from
 * reclaiming memory when the objects are no longer in use.
 * <p>
 * Objects are held in the pool using WeakReferences, which allows the garbage collector to
 * reclaim the objects when there are no strong references to them. When an object is requested,
 * the pool either provides a pooled object or creates a new one if all pooled objects have been
 * reclaimed. When an object is returned to the pool, it is wrapped in a WeakReference and
 * added to the queue.
 * <p>
 * This pool does not guarantee the availability of objects and should be used when object
 * reuse is beneficial but not critical to application logic. It is thread-safe and can be
 * used in concurrent scenarios.
 */
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public class WeakPool<T> {
    private final ConcurrentLinkedQueue<WeakReference<T>> pool = new ConcurrentLinkedQueue<>();
    private final ReferenceQueue<T> refQueue = new ReferenceQueue<>();

    public T get(Supplier<T> creator) {
        cleanUp();

        WeakReference<T> ref;
        T object;

        // Look for an existing object
        while ((ref = pool.poll()) != null) {
            object = ref.get();
            if (object != null) {
                return object;
            }
        }

        // No objects in the pool, create a new one
        return creator.get();
    }

    public void checkIn(T object) {
        pool.offer(new WeakReference<>(object, refQueue));
    }

    private void cleanUp() {
        WeakReference<T> ref;
        while ((ref = (WeakReference<T>) refQueue.poll()) != null) {
            pool.remove(ref);
        }
    }
}
