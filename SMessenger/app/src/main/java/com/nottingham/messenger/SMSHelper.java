package com.nottingham.messenger;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.telephony.SmsMessage;

import com.nottingham.messenger.security.EncryptionHelper;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SettingsManager;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;

/**
 * Created by Me on 25/02/2017.
 */

public class SMSHelper {
    public static void sendSMS(String phoneNumber, String message) {
        SmsManager smsManager = SmsManager.getDefault();

        if (message.length() <= 160) {
            smsManager.sendTextMessage(phoneNumber, null, message, null, null);
        } else {
            ArrayList<String> parts = smsManager.divideMessage(message);
            smsManager.sendMultipartTextMessage(phoneNumber, null, parts, null, null);
        }
    }

    public static void sendSecureSMS(Context context, String phoneNumber, String message) throws Exception {
        SecretKey key = KeyStoreHelper.getAESKey(context, phoneNumber, SettingsManager.enteredPassword);
        String encryptedMessage = EncryptionHelper.getEncryptedMessageWithIVAsPrefixInHex(key, message);

        sendSMS(phoneNumber, encryptedMessage);
    }

    public static String getTextFromSecureSMS(Context context, String phoneNumber, String message) {
        try {
            SecretKey key = KeyStoreHelper.getAESKey(context, phoneNumber, SettingsManager.enteredPassword);
            String messageInPlainText = EncryptionHelper.getPlainTextFromEncryptedMessageWithIVInHex(key, message);
            return messageInPlainText;
        } catch (Exception e) {
            return null;
        }
    }

    public static Map<String, String> retrieveReceivedSMSMessages(Intent intent) {
        Map<String, String> allMessages = null;
        SmsMessage[] messages = null;
        Bundle bundle = intent.getExtras();

        if (bundle != null && bundle.containsKey("pdus")) {
            Object[] pdus = (Object[]) bundle.get("pdus");

            if (pdus != null) {
                int numberOfPdus = pdus.length;
                allMessages = new HashMap<String, String>(numberOfPdus);
                messages = new SmsMessage[numberOfPdus];

                for (int i = 0; i < numberOfPdus; i++) {
                    messages[i] = SmsMessage.createFromPdu((byte[]) pdus[i]);

                    String address = messages[i].getOriginatingAddress();

                    if (!allMessages.containsKey(address)) {
                        allMessages.put(messages[i].getOriginatingAddress(), messages[i].getMessageBody());

                    } else {
                        String previousParts = allMessages.get(address);
                        String messageString = previousParts + messages[i].getMessageBody();
                        allMessages.put(address, messageString);
                    }
                }
            }
        }

        return allMessages;
    }
}
