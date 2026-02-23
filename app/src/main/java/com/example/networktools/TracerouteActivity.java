package com.example.networktools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class TracerouteActivity extends AppCompatActivity {

    private EditText etHost, etMaxHops, etTimeout;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_traceroute);

        etHost = findViewById(R.id.et_host);
        etMaxHops = findViewById(R.id.et_max_hops);
        etTimeout = findViewById(R.id.et_timeout);
        tvOutput = findViewById(R.id.tv_output);
        Button btnRun = findViewById(R.id.btn_run);

        btnRun.setOnClickListener(v -> {
            String host = etHost.getText().toString();
            int maxHops = 30;
            int timeout = 1;

            try {
                String hopsStr = etMaxHops.getText().toString();
                if (!hopsStr.isEmpty()) maxHops = Integer.parseInt(hopsStr);
            } catch (NumberFormatException e) {}

            try {
                String timeoutStr = etTimeout.getText().toString();
                if (!timeoutStr.isEmpty()) timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {}

            tvOutput.setText("Running Traceroute...");
            final int finalMaxHops = maxHops;
            final int finalTimeout = timeout;

            new Thread(() -> {
                String result = NetworkUtils.traceroute(host, finalMaxHops, finalTimeout);
                runOnUiThread(() -> tvOutput.setText(result));
            }).start();
        });
    }
}
