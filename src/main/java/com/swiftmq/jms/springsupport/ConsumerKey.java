package com.swiftmq.jms.springsupport;

public class ConsumerKey {
    String key;

    public ConsumerKey(String destName, String selector, boolean noLocal, String durName) {
        String b = destName + "/" +
                (selector == null ? "null" : selector) +
                "/" +
                (noLocal ? "true" : "false") +
                "/" +
                (durName == null ? "null" : durName);
        key = b;
    }

    public String getKey() {
        return key;
    }

    public String toString() {
        return key;
    }
}
