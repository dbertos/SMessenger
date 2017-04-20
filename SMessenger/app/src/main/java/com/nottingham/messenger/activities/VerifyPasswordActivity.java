package com.nottingham.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.nottingham.messenger.R;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SettingsManager;


public class VerifyPasswordActivity extends BaseActivity {

    private Button btnContinue;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_verify_password);

        setupToolbar();
        setOnClickListenerForContinueButton();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setOnClickListenerForContinueButton() {
        btnContinue = (Button) findViewById(R.id.bt_continue_after_password_verify);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText passwordText = (EditText) findViewById(R.id.et_current_password);

                int length = passwordText.length();
                char[] password = new char[length];
                passwordText.getText().getChars(0, length, password, 0);

                if (KeyStoreHelper.verifyPassword(getApplicationContext(), password)) {
                    SettingsManager.enteredPassword = password;
                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                    finish();
                } else {
                    passwordText.getText().clear();
                    Toast.makeText(getApplicationContext(), "Incorrect password! Please try again!", Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupToolbar() {
        setupToolbar(R.id.toolbar, "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
