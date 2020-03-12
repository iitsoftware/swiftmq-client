package com.swiftmq.tools.security;

import java.io.*;
import java.security.*;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

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

    public void addKeyPair(String name, String privateKey, String publicKey) throws Exception {
        PrivateKey pk = buildPrivateKey(privateKey);
        Certificate cert = buildPublicKeyCert(publicKey);
        Certificate[] certChain = new Certificate[] { cert };

        store.setKeyEntry(name, pk, password.toCharArray(), certChain);
        save();
    }

    public void removeKey(String name) throws Exception {
        removeCert(name);
    }

    private PrivateKey buildPrivateKey(String key) throws Exception {
        String encodedPrivateKey = key.replaceAll("\\n", "")
                .replace("-----BEGIN PRIVATE KEY-----", "")
                .replace("-----END PRIVATE KEY-----", "");
        Base64.Decoder b64 = Base64.getDecoder();
        byte[] decodedPrivateKey = b64.decode(encodedPrivateKey);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(decodedPrivateKey);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private Certificate buildPublicKeyCert(String key) throws Exception {
        InputStream is = new ByteArrayInputStream(key.getBytes());
        return CertificateFactory.getInstance("X509").generateCertificate(is);
    }

    public void save() throws Exception {
        File newFile = new File(filePath);
        FileOutputStream out = new FileOutputStream(newFile);
        store.store(out, password.toCharArray());
    }
}
