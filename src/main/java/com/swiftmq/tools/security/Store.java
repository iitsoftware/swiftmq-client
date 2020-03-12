package com.swiftmq.tools.security;

import java.io.*;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

abstract class Store {

    private String fileProperty;
    private String passwordProperty;
    private String filePath;
    private String password;
    protected final KeyStore store;

    public Store(String fileProperty, String passwordProperty) throws Exception {
        this.fileProperty = fileProperty;
        this.passwordProperty = passwordProperty;
        filePath = System.getProperty(fileProperty);
        password = System.getProperty(passwordProperty);

        store = loadStore();
    }

    private KeyStore loadStore() throws Exception {
        if (filePath == null) {
            throw new Exception("Store file required. Provide one with with " + fileProperty + ".");
        }

        if (password == null) {
            throw new Exception("Keystore file required. Provide one with " + passwordProperty + ".");
        }

        File file = new File(filePath);
        if (!file.exists()) {
            throw new Exception("Store file not found at: " + filePath + ".");
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(new FileInputStream(file), password.toCharArray());

        return ks;
    }

    public void addCert(String alias, byte[] cert) throws Exception {
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(cert);
        X509Certificate x509Cert = (X509Certificate)certFactory.generateCertificate(in);

        store.setCertificateEntry(alias, x509Cert);
        save();
    }

    public void removeCert(String alias) throws Exception {
        store.deleteEntry(alias);
        save();
    }


    private void save() throws Exception {
        File newFile = new File(filePath);
        FileOutputStream out = new FileOutputStream(newFile);
        store.store(out, password.toCharArray());
    }
}
