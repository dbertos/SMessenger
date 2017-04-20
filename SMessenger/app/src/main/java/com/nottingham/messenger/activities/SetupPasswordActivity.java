package com.nottingham.messenger.activities;

import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import com.nottingham.messenger.MyUtil;
import com.nottingham.messenger.R;
import com.nottingham.messenger.security.KeyStoreHelper;
import com.nottingham.messenger.security.SettingsManager;


public class SetupPasswordActivity extends BaseActivity {

    private Button btnContinue;
    private static final String[] spinnerOptions = {"1 Day", "3 Days", "7 Days"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup_password);

        setupToolbar();
        setOnClickListenerForContinueButton();

        Spinner spinner = (Spinner) findViewById(R.id.expiration_days_spinner);
        ArrayAdapter<String> adapter = new ArrayAdapter<String>(SetupPasswordActivity.this, android.R.layout.simple_spinner_item, spinnerOptions);

        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        spinner.setSelection(1);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }

    private void setOnClickListenerForContinueButton() {
        btnContinue = (Button) findViewById(R.id.bt_continue_after_password_setup);
        btnContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                EditText passwordText = (EditText) findViewById(R.id.et_new_password);

                int length = passwordText.length();
                char[] password = new char[length];
                passwordText.getText().getChars(0, length, password, 0);

                try {
                    Toast.makeText(getApplicationContext(), "Setting up, please wait...", Toast.LENGTH_LONG).show();

                    KeyStoreHelper.generateRSAKeyPairAndStore(getApplicationContext(), password);

                    SettingsManager.enteredPassword = password;
                    SettingsManager.numberOfDaysAesKeyIsValidFor = getSelectedOptionOfExpirationDays();

                    MyUtil.setSetupCompleted(getApplicationContext());

                    startActivity(new Intent(getApplicationContext(), MainActivity.class));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(getApplicationContext(), "Something went wrong" + e.toString(), Toast.LENGTH_LONG).show();
                }
            }
        });
    }

    private int getSelectedOptionOfExpirationDays() {
        Spinner spinner = (Spinner) findViewById(R.id.expiration_days_spinner);

        if (spinner.getSelectedItem().toString().equals(spinnerOptions[0])) return 1;
        if (spinner.getSelectedItem().toString().equals(spinnerOptions[1])) return 3;
        if (spinner.getSelectedItem().toString().equals(spinnerOptions[2])) return 7;

        return 0;
    }

    private void setupToolbar() {
        setupToolbar(R.id.toolbar, "");
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        return true;
    }
}
