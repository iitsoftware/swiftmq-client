package com.swiftmq.jms;

import javax.jms.CompletionListener;
import javax.jms.Message;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Completioner {
    private static Completioner _instance;
    private ExecutorService service = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r);
        thread.setDaemon(true);
        return thread;
    });

    private Completioner() {
    }

    public static Completioner instance() {
        if (_instance == null)
            _instance = new Completioner();
        return _instance;
    }

    public void complete(Message message, CompletionListener completionListener) {
        service.submit(() -> completionListener.onCompletion(message));
    }

    public void complete(Message message, Exception exception, CompletionListener completionListener) {
        service.submit(() -> completionListener.onException(message, exception));
    }
}
