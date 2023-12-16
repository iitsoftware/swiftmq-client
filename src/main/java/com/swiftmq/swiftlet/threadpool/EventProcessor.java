package com.swiftmq.swiftlet.threadpool;

import java.util.List;

@FunctionalInterface
public interface EventProcessor {
    void process(List<Object> events);
}
