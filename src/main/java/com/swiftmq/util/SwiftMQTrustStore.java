package com.swiftmq.util;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;

public class SwiftMQTrustStore extends CertManager {

    private static String DEFAULT_TRUST_STORE_PASSWORD = "changeit";

    public SwiftMQTrustStore() throws Exception {
        store = defaultTrustStore();
        updateSSLContext();
    }

    private KeyStore defaultTrustStore() throws  Exception {
        String filename = System.getProperty("java.home") + "/lib/security/cacerts".replace('/', File.separatorChar);
        FileInputStream is = new FileInputStream(filename);
        KeyStore keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        keystore.load(is, DEFAULT_TRUST_STORE_PASSWORD.toCharArray());

        return keystore;
    }

    public void addCert(String alias, byte[] cert) throws Exception {
        super.addCert(alias, cert);
        updateSSLContext();
    }

    public void removeCert(String alias) throws Exception {
        super.removeCert(alias);
        updateSSLContext();
    }

    private void updateSSLContext() throws Exception {
        TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
        tmf.init(store);
        SSLContext context = SSLContext.getInstance("TLS");
        context.init(null, tmf.getTrustManagers(), null);
    }
}
