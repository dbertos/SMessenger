package com.nottingham.messenger.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import android.support.annotation.Nullable;

import com.nottingham.messenger.SMSHelper;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SettingsManager;
import com.nottingham.messenger.security.SecureConnectionHelper;

import java.security.PublicKey;
import java.util.Map;

import javax.crypto.SecretKey;

public class SecureConversationEstablishmentListener extends Service {

    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Map<String, String> addressToMessageMap = SMSHelper.retrieveReceivedSMSMessages(intent);

            for (Map.Entry<String, String> addressToMessage : addressToMessageMap.entrySet()) {
                String phoneNumber = addressToMessage.getKey();
                String message = addressToMessage.getValue();

                try {
                    if (message.startsWith(SecureConnectionHelper.LETS_EXCHANGE_RSA_KEYS)) {
                        SecureConnectionHelper.saveRsaPublicKeyOfAnotherParty(getApplicationContext(), phoneNumber, extractKeyInHex(message));
                        SecureConnectionHelper.sendPublicRsaKeyBack(getApplicationContext(), phoneNumber);
                        SecureConnectionHelper.setRSAKeysExchanged(getApplicationContext(), phoneNumber);
                    }

                    if (message.startsWith(SecureConnectionHelper.OK_LETS_EXCHANGE_RSA_KEYS)) {
                        SecureConnectionHelper.saveRsaPublicKeyOfAnotherParty(getApplicationContext(), phoneNumber, extractKeyInHex(message));
                        KeyStoreHelper.regenerateECDHKeyPairToStore(getApplicationContext(), phoneNumber, SettingsManager.enteredPassword);
                        SecureConnectionHelper.sendEncryptedECDHKey(getApplicationContext(), phoneNumber);
                        SecureConnectionHelper.setRSAKeysExchanged(getApplicationContext(), phoneNumber);
                    }

                    if (message.startsWith(SecureConnectionHelper.LETS_EXCHANGE_ECDH_KEYS)) {
                        KeyStoreHelper.regenerateECDHKeyPairToStore(getApplicationContext(), phoneNumber, SettingsManager.enteredPassword);
                        SecureConnectionHelper.sendEncryptedECDHKeyBack(getApplicationContext(), phoneNumber);
                        PublicKey ecdhKeyFromAnotherParty = SecureConnectionHelper.getDecryptedECDHKeyFromHex(context, phoneNumber, extractKeyInHex(message));
                        SecretKey aesKey = SecureConnectionHelper.getAESKeyViaECDH(getApplicationContext(), phoneNumber, ecdhKeyFromAnotherParty, SettingsManager.enteredPassword);
                        KeyStoreHelper.storeAESKeyWithTimeStamp(getApplicationContext(), phoneNumber, aesKey, SettingsManager.enteredPassword);
                        SecureConnectionHelper.setEstablished(getApplicationContext(), phoneNumber);
                    }

                    if (message.startsWith(SecureConnectionHelper.OK_LETS_EXCHANGE_ECDH_KEYS)) {
                        PublicKey ecdhKeyFromAnotherParty = SecureConnectionHelper.getDecryptedECDHKeyFromHex(context, phoneNumber, extractKeyInHex(message));
                        SecretKey aesKey = SecureConnectionHelper.getAESKeyViaECDH(getApplicationContext(), phoneNumber, ecdhKeyFromAnotherParty, SettingsManager.enteredPassword);
                        KeyStoreHelper.storeAESKeyWithTimeStamp(getApplicationContext(), phoneNumber, aesKey, SettingsManager.enteredPassword);
                        SecureConnectionHelper.setEstablished(getApplicationContext(), phoneNumber);
                    }

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        private String extractKeyInHex(String message) {
            final String EMPTY = "";
            return message
                    .replace(SecureConnectionHelper.OK_LETS_EXCHANGE_ECDH_KEYS, EMPTY)
                    .replace(SecureConnectionHelper.OK_LETS_EXCHANGE_RSA_KEYS, EMPTY)
                    .replace(SecureConnectionHelper.LETS_EXCHANGE_ECDH_KEYS, EMPTY)
                    .replace(SecureConnectionHelper.LETS_EXCHANGE_RSA_KEYS, EMPTY)
                    .replace(SecureConnectionHelper.END_CERTIFICATE, EMPTY);
        }
    };


    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        System.out.println("service created");
    }
}
