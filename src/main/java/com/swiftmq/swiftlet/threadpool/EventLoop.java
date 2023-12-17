package com.swiftmq.swiftlet.threadpool;

import java.util.concurrent.CompletableFuture;

public interface EventLoop {
    void submit(Object event);

    CompletableFuture<?> executeInNewThread(Runnable task);

    void close();
}
