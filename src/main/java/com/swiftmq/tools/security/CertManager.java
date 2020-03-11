package com.swiftmq.tools.security;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

public abstract class CertManager {

    protected KeyStore store;

    public void addCert(String alias, byte[] cert) throws Exception{
        CertificateFactory certFactory = CertificateFactory.getInstance("X.509");
        InputStream in = new ByteArrayInputStream(cert);
        X509Certificate x509Cert = (X509Certificate)certFactory.generateCertificate(in);

        store.setCertificateEntry(alias, x509Cert);
    }

    public void removeCert(String alias) throws Exception {
        store.deleteEntry(alias);
    }
}
