package com.swiftmq.util;

import java.security.KeyStore;

public class SwiftMQKeyStore extends CertManager {

    private static String password = "changeme";

    public SwiftMQKeyStore() throws Exception {
        store = KeyStore.getInstance(KeyStore.getDefaultType());
        store.load(null, password.toCharArray());
    }

    public KeyStore get() {
        return store;
    }


}
