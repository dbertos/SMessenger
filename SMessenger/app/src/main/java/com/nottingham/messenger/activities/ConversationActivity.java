package com.nottingham.messenger.activities;

import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nottingham.messenger.R;
import com.nottingham.messenger.SMSHelper;
import com.nottingham.messenger.recylcerchat.ChatData;
import com.nottingham.messenger.recylcerchat.ConversationRecyclerView;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SecureConnectionHelper;
import com.nottingham.messenger.security.SettingsManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class ConversationActivity extends PasswordProtectedActivity {

    public static final SimpleDateFormat HOURS_AND_MINUTES_DATE_FORMAT = new SimpleDateFormat("HH:mm"); // yyyy-MM-dd

    private RecyclerView mRecyclerView;
    private ConversationRecyclerView mAdapter;
    private EditText etMessage;
    private Button btnSend;
    private String phoneNumber;
    private SharedPreferences.OnSharedPreferenceChangeListener secureConnectionEstablishmentListenerForUpdatingLockMenuItem;
    private MenuItem lockMenuItem;

    private final BroadcastReceiver smsReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Map<String, String> addressToMessagesMap = SMSHelper.retrieveReceivedSMSMessages(intent);
            if (addressToMessagesMap.containsKey(phoneNumber)) {

                updateStatusAndSecureConnectionEstablishmentIcon();

                String encryptedMessageWithIVInPrefixAsHex = addressToMessagesMap.get(phoneNumber);
                String messageInPlainText = SMSHelper.getTextFromSecureSMS(getApplicationContext(), phoneNumber, encryptedMessageWithIVInPrefixAsHex);
                if (messageInPlainText != null) {
                    ChatData chatData = new ChatData("1", messageInPlainText);
                    mAdapter.addItem(chatData);
                    scrollToConversationBottom();
                }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        phoneNumber = getIntent().getExtras().getString("address");

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_conversation);

        setupToolbar();
        setupRecyclerView();
        setupMessageField();
        setupSendButton();
        registerReceiver(smsReceiver, new IntentFilter("android.provider.Telephony.SMS_RECEIVED"));
        registerSecureConnectionEstablishmentListenerForUpdatingLockMenuItem();
    }

    @Override
    public void onResume() {
        super.onResume();
        updateStatusAndSecureConnectionEstablishmentIcon();
    }

    private void registerSecureConnectionEstablishmentListenerForUpdatingLockMenuItem() {
        secureConnectionEstablishmentListenerForUpdatingLockMenuItem = new SharedPreferences.OnSharedPreferenceChangeListener() {
            public void onSharedPreferenceChanged(SharedPreferences prefs, String key) {
                if (!lockMenuItem.getIcon().equals(getResources().getDrawable(R.drawable.lock_locked))
                        && SecureConnectionHelper.established(getApplicationContext(), phoneNumber))

                    lockMenuItem.setIcon(getResources().getDrawable(R.drawable.lock_locked));
            }
        };

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        preferences.registerOnSharedPreferenceChangeListener(secureConnectionEstablishmentListenerForUpdatingLockMenuItem);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
//        unregisterReceiver(smsReceiver);
    }

    private void setupSendButton() {
        btnSend = (Button) findViewById(R.id.bt_send);
        btnSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                updateStatusAndSecureConnectionEstablishmentIcon();

                if (SecureConnectionHelper.established(getApplicationContext(), phoneNumber)) {

                    String message = etMessage.getText().toString();
                    if (!message.equals("")) {
                        mAdapter.addItem(new ChatData("2", message));
                        etMessage.setText("");
                        scrollToConversationBottom();

                        try {
                            SMSHelper.sendSecureSMS(getApplicationContext(), phoneNumber, message);
                        } catch (Exception ex) {
                            Toast.makeText(getApplicationContext(), ex.getMessage().toString(), Toast.LENGTH_LONG).show();
                            ex.printStackTrace();
                        }

                    }
                } else {
                    Toast.makeText(getApplicationContext(), "Establish secure connection first!", Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private void updateStatusAndSecureConnectionEstablishmentIcon() {
        SecureConnectionHelper.checkAndUpdateStatusAndKeyBasedOnAesKeyExpirationDate(getApplicationContext(), phoneNumber);
        setSecureConnectionEstablishmentIcon();
    }

    private void scrollToConversationBottom() {
        if (mRecyclerView.getAdapter().getItemCount() > 0)
            mRecyclerView.smoothScrollToPosition(mRecyclerView.getAdapter().getItemCount() - 1);
    }

    private void setupMessageField() {
        etMessage = (EditText) findViewById(R.id.et_message);
        etMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mRecyclerView.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        scrollToConversationBottom();
                    }
                }, 100);
            }
        });
    }

    private void setupRecyclerView() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        mAdapter = new ConversationRecyclerView(this, getAllMessages());
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.postDelayed(new Runnable() {
            @Override
            public void run() {
                scrollToConversationBottom();
            }
        }, 100);
    }

    private void refreshRecycleView() {
        mAdapter.setItems(getAllMessages());
        scrollToConversationBottom();
    }

    private void setupToolbar() {
        if (getIntent().getExtras().containsKey("name")) {
            setupToolbarWithUpNav(R.id.toolbar, getIntent().getExtras().getString("name"), R.drawable.ic_action_back);
        } else {
            setupToolbarWithUpNav(R.id.toolbar, phoneNumber, R.drawable.ic_action_back);
        }
    }

    private void establishSecureConnection(String phoneNumber) {
        Toast.makeText(getApplicationContext(), "Wait until secure connection is established", Toast.LENGTH_LONG).show();
        try {
            if (SecureConnectionHelper.establishmentNotStarted(getApplicationContext(), phoneNumber)) {
                SecureConnectionHelper.sendPublicRsaKey(getApplicationContext(), phoneNumber);
                SecureConnectionHelper.setEstablishmentStarted(getApplicationContext(), phoneNumber);
            }

            if (SecureConnectionHelper.rsaKeysExchanged(getApplicationContext(), phoneNumber)) {
                KeyStoreHelper.regenerateECDHKeyPairToStore(getApplicationContext(), phoneNumber, SettingsManager.enteredPassword);
                SecureConnectionHelper.sendEncryptedECDHKey(getApplicationContext(), phoneNumber);

            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getApplicationContext(), "Problem establishing connection..." + e.toString(), Toast.LENGTH_LONG).show();
        }
    }

    public List<ChatData> getAllMessages() {
        List<ChatData> data = new ArrayList<>();

        ContentResolver contentResolver = getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms"), null, "address='" + phoneNumber + "'", null, "date asc");
        String[] projection = new String[]{"_id", "phoneNumber", "person", "body", "date", "type"};

        int bodyIndex = smsInboxCursor.getColumnIndex("body");
        int dateIndex = smsInboxCursor.getColumnIndex("date");
        int typeIndex = smsInboxCursor.getColumnIndex("type");

        smsInboxCursor.moveToFirst();

        decryptMessagesAndFillChatData(phoneNumber, data, smsInboxCursor, bodyIndex, dateIndex, typeIndex);

        return data;
    }

    private void decryptMessagesAndFillChatData(String address, List<ChatData> data, Cursor smsInboxCursor, int bodyIndex, int dateIndex, int typeIndex) {
        if (!SecureConnectionHelper.established(getApplicationContext(), phoneNumber)) return;

        if (!smsInboxCursor.moveToFirst()) return;

        do {
            String encryptedMessageWithIVInPrefixAsHex = smsInboxCursor.getString(bodyIndex);
            String messageInPlainText = SMSHelper.getTextFromSecureSMS(this, address, encryptedMessageWithIVInPrefixAsHex);

            if (messageInPlainText != null) {
                ChatData item = new ChatData();
                item.setType(smsInboxCursor.getString(typeIndex));
                item.setText(messageInPlainText);
                item.setTime(HOURS_AND_MINUTES_DATE_FORMAT.format(new Date(smsInboxCursor.getLong(dateIndex))));
                data.add(item);
            }

        } while (smsInboxCursor.moveToNext());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_userphoto, menu);

        lockMenuItem = menu.getItem(0);

        setSecureConnectionEstablishmentIcon();

        return true;
    }

    private void setSecureConnectionEstablishmentIcon() {
        if (lockMenuItem == null) return;

        if (SecureConnectionHelper.established(getApplicationContext(), phoneNumber)) {
            lockMenuItem.setIcon(getResources().getDrawable(R.drawable.lock_locked));

            lockMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    Toast.makeText(getApplicationContext(), "Connection is secure!", Toast.LENGTH_LONG).show();
                    return true;
                }
            });
        } else {
            lockMenuItem.setIcon(getResources().getDrawable(R.drawable.lock_unlocked));

            lockMenuItem.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
                @Override
                public boolean onMenuItemClick(MenuItem item) {
                    if (!SecureConnectionHelper.established(getApplicationContext(), phoneNumber)) {
                        establishSecureConnection(phoneNumber);
                    } else {
                        Toast.makeText(getApplicationContext(), "Connection is secure!", Toast.LENGTH_LONG).show();
                    }
                    return true;
                }
            });
        }
    }
}
