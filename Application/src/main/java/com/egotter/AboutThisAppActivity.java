package com.egotter;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class AboutThisAppActivity extends AppCompatActivity {

    private TextView versionText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about_this_app);

        versionText = findViewById(R.id.version);
        versionText.setText(getString(R.string.versionNameFormat, BuildConfig.VERSION_NAME));

    }
}
