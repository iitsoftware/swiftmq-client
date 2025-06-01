package com.swiftmq.client.thread;

import com.swiftmq.swiftlet.SwiftletManager;
import com.swiftmq.swiftlet.threadpool.AsyncTask;
import com.swiftmq.swiftlet.threadpool.ThreadPool;
import com.swiftmq.swiftlet.threadpool.ThreadpoolSwiftlet;
import com.swiftmq.swiftlet.threadpool.event.FreezeCompletionListener;

public class IVMTaskScheduler implements ThreadPool {

    ThreadpoolSwiftlet threadpoolSwiftlet = null;

    public IVMTaskScheduler() {
        threadpoolSwiftlet = (ThreadpoolSwiftlet) SwiftletManager.getInstance().getSwiftlet("sys$threadpool");
        if (threadpoolSwiftlet != null)
            System.out.println("[" + this + "] App uses virtual threads, scheduled in: adhocvirtual");
    }

    /**
     * Closes the pool.
     * Internal use only.
     */
    @Override
    public void close() {

    }

    /**
     * Returns the pool name.
     *
     * @return pool name.
     */
    @Override
    public String getPoolName() {
        return toString();
    }

    /**
     * Returns the number of currently idling threads.
     * Used from management tools only.
     *
     * @return number of idling threads.
     */
    @Override
    public int getNumberIdlingThreads() {
        return 0;
    }

    /**
     * Returns the number of currently running threads.
     * Used from management tools only.
     *
     * @return number of running threads.
     */
    @Override
    public int getNumberRunningThreads() {
        return 0;
    }

    /**
     * Dispatch a task into the pool.
     *
     * @param asyncTask the task to dispatch.
     */
    @Override
    public void dispatchTask(AsyncTask asyncTask) {
        if (threadpoolSwiftlet != null)
            threadpoolSwiftlet.runAsync(asyncTask, true);
        else
            System.err.println("[IVMTaskScheduler] dispatchTask() no threadpool swiftlet");
    }

    /**
     * Freezes this pool. That is, the current running tasks are completed but
     * no further tasks will be scheduled until unfreeze() is called. It is possible
     * to dispatch tasks during freeze. However, these will be executed after unfreeze()
     * is called.
     *
     * @param listener will be called when the pool is freezed.
     */
    @Override
    public void freeze(FreezeCompletionListener listener) {

    }

    /**
     * Unfreezes this pool.
     */
    @Override
    public void unfreeze() {

    }

    /**
     * Stops the pool.
     * Internal use only.
     */
    @Override
    public void stop() {

    }

    @Override
    public String toString() {
        return IVMTaskScheduler.class.getSimpleName();
    }
}
