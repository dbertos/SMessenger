package com.nottingham.messenger;

import android.content.ContentResolver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.provider.ContactsContract;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.nottingham.messenger.activities.ConversationActivity;
import com.nottingham.messenger.activities.MainActivity;
import com.nottingham.messenger.recyclerview.Chat;
import com.nottingham.messenger.recyclerview.ChatAdapter;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SecureConnectionHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;


public class FragmentHome extends Fragment implements ChatAdapter.ViewHolder.ClickListener {
    private RecyclerView mRecyclerView;
    private ChatAdapter mAdapter;
    private TextView tv_selection;

    public FragmentHome() {
        setHasOptionsMenu(true);
    }

    public void onCreate(Bundle a) {
        super.onCreate(a);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, null, false);

        getActivity().supportInvalidateOptionsMenu();
        ((MainActivity) getActivity()).changeTitle(R.id.toolbar, "Messages");

        tv_selection = (TextView) view.findViewById(R.id.tv_selection);
        mRecyclerView = (RecyclerView) view.findViewById(R.id.recyclerView);
        mRecyclerView.setHasFixedSize(true);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mAdapter = new ChatAdapter(getContext(), getChats(), this);
        mRecyclerView.setAdapter(mAdapter);
    }

    public List<Chat> getChats() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(getContext());
        Map<String, ?> keyToValueMap = preferences.getAll();

        List<Chat> chats = new ArrayList<>();

        for (String key : keyToValueMap.keySet()) {
            if (key.startsWith(KeyStoreHelper.KEY_EXPIRATION_DATE)) {
                String phoneNumber = key.replace(KeyStoreHelper.KEY_EXPIRATION_DATE, "");

                SecureConnectionHelper.checkAndUpdateStatusAndKeyBasedOnAesKeyExpirationDate(getContext(), phoneNumber);

                if (SecureConnectionHelper.established(getContext(), phoneNumber)) {
                    Pair<String, String> lastSecureMessageWithTime = getLastSecureMessageWithTime(phoneNumber);

                    if (lastSecureMessageWithTime != null) {
                        String message = lastSecureMessageWithTime.first;
                        String time = lastSecureMessageWithTime.second;

                        createNewChatAndAddToChats(chats, phoneNumber, message, time);
                    } else {
                        createNewChatAndAddToChats(chats, phoneNumber, "No messages yet...", "");
                    }
                }

                if (SecureConnectionHelper.establishmentStarted(getContext(), phoneNumber) || SecureConnectionHelper.rsaKeysExchanged(getContext(), phoneNumber)) {
                    createNewChatAndAddToChats(chats, phoneNumber, "Establishing secure connection...", "");
                }
            }
        }

        return chats;
    }

    private void createNewChatAndAddToChats(List<Chat> data, String phoneNumber, String message, String time) {
        Chat chat = new Chat();
        chat.setTime(time);
        chat.setAddress(phoneNumber);
        chat.setImage(R.drawable.userpic);
        chat.setOnline(false);
        chat.setLastChat(message);
        chat.setDisplayName(getContactName(getContext().getApplicationContext().getContentResolver(), phoneNumber));
        data.add(chat);
    }

    private Pair<String, String> getLastSecureMessageWithTime(String phoneNumber) {
        ContentResolver contentResolver = getContext().getApplicationContext().getContentResolver();
        Cursor smsInboxCursor = contentResolver.query(Uri.parse("content://sms"), null, "address='" + phoneNumber + "'", null, "date desc");
        smsInboxCursor.moveToFirst();

        int bodyIndex = smsInboxCursor.getColumnIndex("body");
        int dateIndex = smsInboxCursor.getColumnIndex("date");

        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("HH:mm");

        do {
            String message = smsInboxCursor.getString(bodyIndex);
            String time = simpleDateFormat.format(new Date(smsInboxCursor.getLong(dateIndex)));
            String decryptedMessage = SMSHelper.getTextFromSecureSMS(getContext(), phoneNumber, message);

            if (decryptedMessage != null) return new Pair<String, String>(decryptedMessage, time);

        } while (smsInboxCursor.moveToNext());

        return null;
    }

    public String getContactName(ContentResolver cr, String phoneNumber) {
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(phoneNumber));
        Cursor cursor = cr.query(uri, new String[]{ContactsContract.PhoneLookup.DISPLAY_NAME}, null, null, null);
        if (cursor == null) return null;

        String contactName = null;
        if (cursor.moveToFirst()) {
            contactName = cursor.getString(cursor.getColumnIndex(ContactsContract.PhoneLookup.DISPLAY_NAME));
        }
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return contactName;
    }

    @Override
    public void onItemClicked(int position) {
        Intent intent = new Intent(getActivity(), ConversationActivity.class);
        String address = mAdapter.getItem(position).getAddress();
        intent.putExtra("address", address);
        if (mAdapter.getItem(position).hasDisplayName())
            intent.putExtra("name", mAdapter.getItem(position).getDisplayName());
        startActivity(intent);
    }

    @Override
    public boolean onItemLongClicked(int position) {
        toggleSelection(position);
        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return false;
    }

    private void toggleSelection(int position) {
        mAdapter.toggleSelection(position);
        if (mAdapter.getSelectedItemCount() > 0) {
            tv_selection.setVisibility(View.VISIBLE);
        } else
            tv_selection.setVisibility(View.GONE);


        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                tv_selection.setText("Delete (" + mAdapter.getSelectedItemCount() + ")");
            }
        });

    }

    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        menu.clear();
        inflater.inflate(R.menu.menu_edit, menu);
    }
}
