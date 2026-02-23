package com.example.networktools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class ArpActivity extends AppCompatActivity {

    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_arp);

        tvOutput = findViewById(R.id.tv_output);
        Button btnRun = findViewById(R.id.btn_run);

        btnRun.setOnClickListener(v -> {
            tvOutput.setText("Fetching ARP Table...");
            new Thread(() -> {
                String result = NetworkUtils.getArpTable();
                runOnUiThread(() -> tvOutput.setText(result));
            }).start();
        });
    }
}
