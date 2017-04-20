package com.nottingham.messenger.security;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.Cipher;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

public class EncryptionHelper {

    public static final String AES_GCM_NO_PADDING = "AES/GCM/NoPadding";
    private static final String UTF_8 = "UTF-8";
    private static final int IV_SIZE_IN_HEX = 24;

    public static String getEncryptedMessageWithIVAsPrefixInHex(SecretKey secretKey, String plaintext) throws Exception {
        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);

        byte[] iv = generateIv();
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

        byte[] encryptedTextBytes = cipher.doFinal(plaintext.getBytes(UTF_8));
        String encryptedTextInHex = Hex.bytesToHex(encryptedTextBytes);

        String ivInHex = Hex.bytesToHex(iv);
        String ivWithEncryptedText = ivInHex + encryptedTextInHex;

        return ivWithEncryptedText;
    }

    private static byte[] generateIv() throws NoSuchAlgorithmException {
        SecureRandom random = new SecureRandom();
        byte[] iv = new byte[12];
        random.nextBytes(iv);
        return iv;
    }

    private static byte[] extractIVFromText(String ivWithEncryptedTextInHex) {
        String ivInHex = ivWithEncryptedTextInHex.substring(0, IV_SIZE_IN_HEX);
        return Hex.hexStringToByteArray(ivInHex);
    }

    public static String getPlainTextFromEncryptedMessageWithIVInHex(SecretKey secretKey, String ivWithEncryptedTextInHex) throws Exception {
        byte[] ivBytes = extractIVFromText(ivWithEncryptedTextInHex);

        String encryptedText = ivWithEncryptedTextInHex.substring(IV_SIZE_IN_HEX);

        Cipher cipher = Cipher.getInstance(AES_GCM_NO_PADDING);
        cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(ivBytes));

        byte[] decryptedTextBytes = cipher.doFinal(Hex.hexStringToByteArray(encryptedText));

        return new String(decryptedTextBytes);
    }
}
