package com.example.networktools;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnOpenPing = findViewById(R.id.btn_open_ping);
        Button btnOpenTraceroute = findViewById(R.id.btn_open_traceroute);
        Button btnOpenArp = findViewById(R.id.btn_open_arp);

        btnOpenPing.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, PingActivity.class);
            startActivity(intent);
        });

        btnOpenTraceroute.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, TracerouteActivity.class);
            startActivity(intent);
        });

        btnOpenArp.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, ArpActivity.class);
            startActivity(intent);
        });
    }
}
