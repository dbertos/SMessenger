package com.nottingham.messenger.security;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.nottingham.messenger.SMSHelper;

import org.spongycastle.asn1.nist.NISTObjectIdentifiers;

import java.security.Key;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.Security;
import java.security.spec.X509EncodedKeySpec;
import java.util.Date;

import javax.crypto.Cipher;
import javax.crypto.KeyAgreement;
import javax.crypto.SecretKey;

import static com.nottingham.messenger.security.KeyStoreHelper.KEY_EXPIRATION_DATE;

/**
 * Created by user on 16/03/2017.
 */

public class SecureConnectionHelper {

    static {
        Security.insertProviderAt(new org.spongycastle.jce.provider.BouncyCastleProvider(), 1);
    }

    public enum SecureConnectionStatus {
        ESTABLISHMENT_NOT_STARTED(0),
        ESTABLISHMENT_STARTED(1),
        RSA_KEYS_EXCHANGED(2),
        ESTABLISHED(3);

        private final int statusCode;

        SecureConnectionStatus(int statusCode) {
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return this.statusCode;
        }

        public static SecureConnectionStatus getStatus(int statusCode) {
            for (SecureConnectionStatus status : SecureConnectionStatus.values())
                if (status.statusCode == statusCode) return status;
            return null;
        }
    }

    public static final String RSA_ECB_OAEPWITH_SHA1_AND_MGF1_PADDING = "RSA/ECB/OAEPWithSHA1AndMGF1Padding";
    public static final String OK_LETS_EXCHANGE_RSA_KEYS = "OK. Let's exchange RSA public keys. BEGIN CERTIFICATE:";
    public static final String LETS_EXCHANGE_RSA_KEYS = "Let's exchange RSA public keys. BEGIN CERTIFICATE:";
    public static final String OK_LETS_EXCHANGE_ECDH_KEYS = "OK. Let's exchange ECDH public keys. BEGIN CERTIFICATE:";
    public static final String LETS_EXCHANGE_ECDH_KEYS = "Let's exchange ECDH public keys. BEGIN CERTIFICATE:";
    public static final String END_CERTIFICATE = ":END CERTIFICATE";
    private static final String EMPTY = "";
    private static final String ECDH = "ECDH";
    private static final String SPONGY_CASTLE = "SC";
    private static final String RSA_PUBLIC_KEY = "RSA_PUBLIC_KEY";

    public static SecretKey getAESKeyViaECDH(Context context, String phoneNumber, PublicKey ecdhKeyOfAnotherParty, char[] password) throws Exception {
        PrivateKey ecdhPrivateKey = KeyStoreHelper.getECDHPrivateKey(context, phoneNumber, password);
        return ecdh(ecdhPrivateKey, ecdhKeyOfAnotherParty);
    }

    private static SecretKey ecdh(PrivateKey myPrivKey, PublicKey otherPubKey) throws Exception {
        KeyAgreement keyAgreement = KeyAgreement.getInstance(ECDH);
        keyAgreement.init(myPrivKey);
        keyAgreement.doPhase(otherPubKey, true);

        SecretKey secretKey = keyAgreement.generateSecret(NISTObjectIdentifiers.id_aes256_GCM.getId());
        System.out.println(secretKey.getEncoded().length);
        return secretKey;
    }

    public static PublicKey getPublicKeyFromBytes(String algorithm, byte[] key) throws Exception {
        return KeyFactory.getInstance(algorithm, SPONGY_CASTLE).generatePublic(new X509EncodedKeySpec(key));
    }

    public static void setEstablishmentStarted(Context context, String phoneNumber) {
        putIntToSharedPreferences(context, phoneNumber, SecureConnectionStatus.ESTABLISHMENT_STARTED.getStatusCode());
    }

    public static void setRSAKeysExchanged(Context context, String phoneNumber) {
        putIntToSharedPreferences(context, phoneNumber, SecureConnectionStatus.RSA_KEYS_EXCHANGED.getStatusCode());
    }

    public static void setEstablished(Context context, String phoneNumber) {
        putIntToSharedPreferences(context, phoneNumber, SecureConnectionStatus.ESTABLISHED.getStatusCode());
    }

    public static boolean established(Context context, String phoneNumber) {
        int statusCode = PreferenceManager.getDefaultSharedPreferences(context).getInt(phoneNumber, 0);
        return statusCode == 3;
    }

    public static boolean rsaKeysExchanged(Context context, String phoneNumber) {
        int statusCode = PreferenceManager.getDefaultSharedPreferences(context).getInt(phoneNumber, 0);
        return statusCode == 2;
    }

    public static boolean establishmentStarted(Context context, String phoneNumber) {
        int statusCode = PreferenceManager.getDefaultSharedPreferences(context).getInt(phoneNumber, 0);
        return statusCode == 1;
    }

    public static boolean establishmentNotStarted(Context context, String phoneNumber) {
        int statusCode = PreferenceManager.getDefaultSharedPreferences(context).getInt(phoneNumber, 0);
        return statusCode == 0;
    }

    public static void sendPublicRsaKey(Context context, String phoneNumber) {
        PublicKey publicKey = KeyStoreHelper.getRSAPublicKey(context, SettingsManager.enteredPassword);
        String myPublicKeyString = Hex.bytesToHex(publicKey.getEncoded());
        SMSHelper.sendSMS(phoneNumber, LETS_EXCHANGE_RSA_KEYS + myPublicKeyString + END_CERTIFICATE);
    }

    public static void sendPublicRsaKeyBack(Context context, String phoneNumber) {
        PublicKey publicKey = KeyStoreHelper.getRSAPublicKey(context, SettingsManager.enteredPassword);
        String myPublicKeyString = Hex.bytesToHex(publicKey.getEncoded());
        SMSHelper.sendSMS(phoneNumber, OK_LETS_EXCHANGE_RSA_KEYS + myPublicKeyString + END_CERTIFICATE);
    }

    public static void sendEncryptedECDHKey(Context context, String phoneNumber) throws Exception {
        String encryptedECDHKeyInString = getEncryptedECDHKeyInHex(context, phoneNumber);
        SMSHelper.sendSMS(phoneNumber, LETS_EXCHANGE_ECDH_KEYS + encryptedECDHKeyInString + END_CERTIFICATE);
    }

    private static String getEncryptedECDHKeyInHex(Context context, String phoneNumber) throws Exception {
        Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPWITH_SHA1_AND_MGF1_PADDING);
        cipher.init(Cipher.ENCRYPT_MODE, KeyStoreHelper.getRSAPrivateKey(context, SettingsManager.enteredPassword));

        PublicKey publicECDHKey = KeyStoreHelper.getECDHPublicKey(context, phoneNumber, SettingsManager.enteredPassword);
        byte[] encryptedECDHKey = cipher.doFinal(publicECDHKey.getEncoded());

        return Hex.bytesToHex(encryptedECDHKey);
    }

    public static PublicKey getDecryptedECDHKeyFromHex(Context context, String phoneNumber, String encryptedECDHKeyInHex) throws Exception {

        byte[] encryptedECDHKey = Hex.hexStringToByteArray(encryptedECDHKeyInHex);

        Cipher cipher = Cipher.getInstance(RSA_ECB_OAEPWITH_SHA1_AND_MGF1_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, getRsaPublicKeyOfAnotherParty(context, phoneNumber));

        byte[] decryptedECDHKey = cipher.doFinal(encryptedECDHKey);
        return getPublicKeyFromBytes(ECDH, decryptedECDHKey);
    }

    public static void sendEncryptedECDHKeyBack(Context context, String phoneNumber) throws Exception {
        String encryptedECDHKeyInString = getEncryptedECDHKeyInHex(context, phoneNumber);
        SMSHelper.sendSMS(phoneNumber, OK_LETS_EXCHANGE_ECDH_KEYS + encryptedECDHKeyInString + END_CERTIFICATE);
    }

    public static void checkAndUpdateStatusAndKeyBasedOnAesKeyExpirationDate(Context context, String phoneNumber) {
        if (established(context, phoneNumber) && !isAESKeyStillValid(context, phoneNumber)) {
            setRSAKeysExchanged(context, phoneNumber);

            try {
                KeyStoreHelper.deleteECDHKeys(context, phoneNumber, SettingsManager.enteredPassword);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    private static boolean isAESKeyStillValid(Context context, String phoneNumber) {
        long expirationDate = PreferenceManager.getDefaultSharedPreferences(context).getLong(KEY_EXPIRATION_DATE + phoneNumber, 0);
        return new Date(expirationDate).after(new Date());
    }

    public static boolean containsAESKeyForAddress(Context context, String phoneNumber, char[] password) {
        return KeyStoreHelper.containsKey(context, phoneNumber, password);
    }

    public static void saveRsaPublicKeyOfAnotherParty(Context context, String phoneNumber, String keyInHex) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString(RSA_PUBLIC_KEY + phoneNumber, keyInHex);
        editor.commit();
    }

    public static PublicKey getRsaPublicKeyOfAnotherParty(Context context, String phoneNumber) throws Exception {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String publicKeyInHex = preferences.getString(RSA_PUBLIC_KEY + phoneNumber, EMPTY);
        return getPublicKeyFromBytes("RSA", Hex.hexStringToByteArray(publicKeyInHex));
    }

    private static void putIntToSharedPreferences(Context context, String phoneNumber, int intToSave) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt(phoneNumber, intToSave);
        editor.commit();
    }
}
