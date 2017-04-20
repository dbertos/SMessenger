package com.nottingham.messenger.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import org.spongycastle.jce.X509Principal;
import org.spongycastle.x509.X509V3CertificateGenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.Security;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.security.spec.ECGenParameterSpec;
import java.util.Calendar;
import java.util.Date;

import javax.crypto.SecretKey;

/**
 * Created by user on 10/03/2017.
 */

public class KeyStoreHelper {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 2);
    }

    public static final String KEY_EXPIRATION_DATE = "KEY_EXPIRATION_DATE:";
    public static final String SHA_1_WITH_RSA_ENCRYPTION = "SHA1WithRSAEncryption";
    public static final String SHA_1_WITH_ECDSA = "SHA1withECDSA";
    public static final String RSA = "RSA";
    public static final String PRIME_256_V_1 = "prime256v1";
    private static final String ECDH = "ECDH";
    private static final String SPONGY_CASTLE = "SC";
    private static final String KEYSTORE_NAME = "keystore.bks";
    private static final String KEYSTORE_TYPE = "UBER";
    private static final String RSA_PUBLIC = "RSA_PUBLIC";
    private static final String RSA_PRIVATE = "RSA_PRIVATE";
    private static final String ECDH_PUBLIC_FOR_ADDRESS = "ECDH_PUBLIC_FOR_ADDRESS:";
    private static final String ECDH_PRIVATE_FOR_ADDRESS = "ECDH_PRIVATE_FOR_ADDRESS:";
    private static final String AES_KEY_FOR_ADDRESS = "AES_FOR_ADDRESS:";

    public static synchronized void generateRSAKeyPairAndStore(Context context, char[] password) throws Exception {
        KeyPair keyPair = generateRSAKeyPair();

        Certificate[] certChain = new Certificate[1];
        certChain[0] = generateCertificate(keyPair, SHA_1_WITH_RSA_ENCRYPTION);

        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);
        keyStore.load(null, null);

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

            keyStore.setKeyEntry(RSA_PRIVATE, keyPair.getPrivate(), password, certChain);
            keyStore.setKeyEntry(RSA_PUBLIC, keyPair.getPublic(), password, certChain);

            keyStore.store(fileOutputStream, password);
        }
    }

    public static synchronized void regenerateECDHKeyPairToStore(Context context, String phoneNumber, char[] password) throws Exception {

        KeyPair keyPair = generateECDHKeyPair();

        Certificate[] certChain = new Certificate[1];
        certChain[0] = generateCertificate(keyPair, SHA_1_WITH_ECDSA);

        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);
        try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {
            keyStore.load(fileInputStream, password);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

            keyStore.setKeyEntry(ECDH_PRIVATE_FOR_ADDRESS + phoneNumber, keyPair.getPrivate(), password, certChain);
            keyStore.setKeyEntry(ECDH_PUBLIC_FOR_ADDRESS + phoneNumber, keyPair.getPublic(), password, certChain);

            keyStore.store(fileOutputStream, password);
        }
    }

    public static synchronized void deleteECDHKeys(Context context, String phoneNumber, char[] password) throws Exception {

        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);
        try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {
            keyStore.load(fileInputStream, password);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

            keyStore.deleteEntry(ECDH_PRIVATE_FOR_ADDRESS + phoneNumber);
            keyStore.deleteEntry(ECDH_PUBLIC_FOR_ADDRESS + phoneNumber);

            keyStore.store(fileOutputStream, password);
        }
    }

    public static synchronized boolean verifyPassword(Context context, char[] password) {
        try {
            try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

                final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);

                keyStore.load(fileInputStream, password);

                return true;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public static PrivateKey getRSAPrivateKey(Context context, char[] password) {
        return (PrivateKey) getKey(context, RSA_PRIVATE, password);
    }

    public static PublicKey getRSAPublicKey(Context context, char[] password) {
        return (PublicKey) getKey(context, RSA_PUBLIC, password);
    }

    public static PrivateKey getECDHPrivateKey(Context context, String phoneNumber, char[] password) {
        return (PrivateKey) getKey(context, ECDH_PRIVATE_FOR_ADDRESS + phoneNumber, password);
    }

    public static PublicKey getECDHPublicKey(Context context, String phoneNumber, char[] password) {
        return (PublicKey) getKey(context, ECDH_PUBLIC_FOR_ADDRESS + phoneNumber, password);
    }

    public static SecretKey getAESKey(Context context, String phoneNumber, char[] password) {
        return (SecretKey) getKey(context, AES_KEY_FOR_ADDRESS + phoneNumber, password);
    }

    public static synchronized void storeAESKeyWithTimeStamp(Context context, String phoneNumber, SecretKey key, char[] password) throws Exception {
        storeAESKey(context, phoneNumber, key, password);
        storeAESKeyExpirationDate(context, phoneNumber);
    }

    private static void storeAESKeyExpirationDate(Context context, String phoneNumber) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(new Date());
//        calendar.add(Calendar.DATE, SettingsManager.numberOfDaysAesKeyIsValidFor);

        calendar.add(Calendar.MINUTE, 5);

        editor.putLong(KEY_EXPIRATION_DATE + phoneNumber, calendar.getTime().getTime());
        editor.commit();
    }

    private static void storeAESKey(Context context, String phoneNumber, SecretKey key, char[] password) throws Exception {
        final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);
        try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {
            keyStore.load(fileInputStream, password);
        }

        try (FileOutputStream fileOutputStream = new FileOutputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {
            keyStore.setKeyEntry(AES_KEY_FOR_ADDRESS + phoneNumber, key, password, null);
            keyStore.store(fileOutputStream, password);
        }
    }

    public static synchronized Key getKey(Context context, String alias, char[] password) {
        try {
            try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

                final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);

                keyStore.load(fileInputStream, password);

                return keyStore.getKey(alias, password);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public static synchronized boolean containsKey(Context context, String keyName, char[] password) {
        try {
            try (FileInputStream fileInputStream = new FileInputStream(new File(context.getFilesDir(), KEYSTORE_NAME))) {

                final KeyStore keyStore = KeyStore.getInstance(KEYSTORE_TYPE, SPONGY_CASTLE);

                keyStore.load(fileInputStream, password);

                return keyStore.containsAlias(keyName);
            }
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private static KeyPair generateECDHKeyPair() throws Exception {
        ECGenParameterSpec ecParamSpecA = new ECGenParameterSpec(PRIME_256_V_1);
        KeyPairGenerator kpgA = KeyPairGenerator.getInstance(ECDH, SPONGY_CASTLE);
        kpgA.initialize(ecParamSpecA);
        return kpgA.generateKeyPair();
    }

    private static KeyPair generateRSAKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance(RSA, SPONGY_CASTLE);
        generator.initialize(2048, new SecureRandom());
        return generator.generateKeyPair();
    }

    private static X509Certificate generateCertificate(KeyPair keyPair, String signatureAlgorithm) throws Exception {
        X509V3CertificateGenerator cert = new X509V3CertificateGenerator();
        cert.setSerialNumber(BigInteger.valueOf(1));   //or generate a random number
        cert.setSubjectDN(new X509Principal("CN=localhost"));  //see examples to add O,OU etc
        cert.setIssuerDN(new X509Principal("CN=localhost")); //same since it is self-signed
        cert.setPublicKey(keyPair.getPublic());
        cert.setNotBefore(new Date(new Date().getTime() - 100000000));
        cert.setNotAfter(new Date(new Date().getTime() - 100000000));
        cert.setSignatureAlgorithm(signatureAlgorithm);
        PrivateKey signingKey = keyPair.getPrivate();
        return cert.generate(signingKey, SPONGY_CASTLE);
    }
}
