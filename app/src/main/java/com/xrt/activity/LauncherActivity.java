package com.xrt.activity;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import com.xrt.R;

import androidx.appcompat.app.AppCompatActivity;

//引导页
public class LauncherActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_launcher);
        jump(500);
    }
    private void jump(int milliSeconds){
        new Handler().postDelayed(() -> {
            Intent intent = new Intent(LauncherActivity.this, MainActivity.class);
            startActivity(intent);
            finish();
        }, milliSeconds);
    }
}
