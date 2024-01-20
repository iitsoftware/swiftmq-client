package com.swiftmq.swiftlet.threadpool;

public interface EventLoop {
    void submit(Object event);

    void close();
}
