package com.nottingham.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;

import com.nottingham.messenger.R;


public class NewConversationActivity extends PasswordProtectedActivity {

    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_conversation);

        setupToolbar();
        setOnClickListenerForContinueButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setOnClickListenerForContinueButton() {
        btnContinue = (Button) findViewById(R.id.bt_continue);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), ConversationActivity.class);
                EditText phoneNumber = (EditText) findViewById(R.id.et_phone);
                String address = phoneNumber.getText().toString();

                address = address.replace(" ", "");
                if (address.startsWith("07")) address = "+44" + address.substring(1);

                intent.putExtra("address", address);
                startActivity(intent);
            }
        });
    }

    private void setupToolbar() {
        setupToolbarWithUpNav(R.id.toolbar, "", R.drawable.ic_action_back);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
