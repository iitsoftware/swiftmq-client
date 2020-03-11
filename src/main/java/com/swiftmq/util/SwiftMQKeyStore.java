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


    public static void main(String[] args) throws Exception {

        String demoCert = "-----BEGIN CERTIFICATE-----\n" +
                "MIIDiTCCAnGgAwIBAgIECrLqazANBgkqhkiG9w0BAQsFADB1MQswCQYDVQQGEwJE\n" +
                "RTEMMAoGA1UECBMDTlJXMREwDwYDVQQHEwhNdWVuc3RlcjEaMBgGA1UEChMRSUlU\n" +
                "IFNvZnR3YXJlIEdtYkgxDDAKBgNVBAsTA0RldjEbMBkGA1UEAxMSU3dpZnRNUSBT\n" +
                "ZWxmU2lnbmVkMB4XDTE5MDQwODA2MzI0NVoXDTQ2MDgyMzA2MzI0NVowdTELMAkG\n" +
                "A1UEBhMCREUxDDAKBgNVBAgTA05SVzERMA8GA1UEBxMITXVlbnN0ZXIxGjAYBgNV\n" +
                "BAoTEUlJVCBTb2Z0d2FyZSBHbWJIMQwwCgYDVQQLEwNEZXYxGzAZBgNVBAMTElN3\n" +
                "aWZ0TVEgU2VsZlNpZ25lZDCCASIwDQYJKoZIhvcNAQEBBQADggEPADCCAQoCggEB\n" +
                "AJDZhKTjZAqdfV3P3JL1B1Tlr4gglDDmpKdLL7CLHz7y/r6f2vqwU77j9LUl4wM+\n" +
                "yjWXQC5cuExTIUe3N/qP/bMbf6pfPnUWikl0LlIm2FuXtBhwZP3/joJugaVBMESw\n" +
                "sNpELJsjJ6HoFBi6bNLWzfWIJyIVC1DQRPGOoNAPEjbqP0KYW10vNjBPnXAmfcxR\n" +
                "MMo2t3cOzgumd+fkpSTz6wAtInqv9UB6Yb4nSspY775D0RUeW6OK0GO6kiEnDeZt\n" +
                "roUHtrObXiM876pjZSv7xbkfewFzOC+3+48hcj0PIzM8yxAz5zS1XLCMzaN8PHw7\n" +
                "JhipKiL8PiKjhFv/bQSJeRcCAwEAAaMhMB8wHQYDVR0OBBYEFDF17SSRob4h6eZD\n" +
                "NtvhcHtUZ7crMA0GCSqGSIb3DQEBCwUAA4IBAQAGBLegi11gXeS8pyY0rs5zU5F4\n" +
                "8dvD86e1RpxRjBqgscG8ieSTsQRlSoIn35n55RRxUnLXAU8mkO3BsiBSmdevLQ7v\n" +
                "9/jlf8ErkZ3r3OTQCbLsKYz/HY8kDHGADrU9dxT55PQ9nZk4DdP7boba0a3Eoahw\n" +
                "pOmgEguYkvOP2uXeIksoMVivFW6XfCnM0gITcb9YV2CNgQqSpc4HdUl8HXwAlvHZ\n" +
                "F2aliGBoIlRP1EFiEDKSXWQWl+0HWKLUn1HA/EcwiJxejCrRVPuSl0AK0+8IVdx9\n" +
                "/mJTt9psMYpvA2Uuntmon0tL0FyIiiMNDbhUOwvzq/SMmiECq19wovTtyphb\n" +
                "-----END CERTIFICATE-----\n";

        byte[] cert = demoCert.getBytes();

        SwiftMQKeyStore ks = new SwiftMQKeyStore();


        ks.addCert("demo", cert);
        System.out.println("Stored cert:");
        System.out.println(ks.get().getCertificate("demo"));

        ks.removeCert("demo");
        System.out.println("Removed cert:");
        System.out.println(ks.get().getCertificate("demo"));

    }

}
