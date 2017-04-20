package com.nottingham.messenger.activities;

import android.content.Intent;
import android.os.Bundle;

import com.nottingham.messenger.security.SettingsManager;

/**
 * Created by user on 14/03/2017.
 */

public class PasswordProtectedActivity extends BaseActivity {
    public long lastPause;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        lastPause = System.currentTimeMillis();
    }

    @Override
    public void onPause() {
        super.onPause();
        lastPause = System.currentTimeMillis();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (System.currentTimeMillis() - lastPause > 10000) {
            SettingsManager.removePasswordFromMemory();
            startActivity(new Intent(getApplicationContext(), VerifyPasswordActivity.class));
        }
    }

    @Override
    public void onBackPressed() {
        //do nothing
    }
}
