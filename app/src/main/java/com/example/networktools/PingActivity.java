package com.example.networktools;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class PingActivity extends AppCompatActivity {

    private EditText etHost, etCount, etTimeout;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_ping);

        etHost = findViewById(R.id.et_host);
        etCount = findViewById(R.id.et_count);
        etTimeout = findViewById(R.id.et_timeout);
        tvOutput = findViewById(R.id.tv_output);
        Button btnRun = findViewById(R.id.btn_run);

        btnRun.setOnClickListener(v -> {
            String host = etHost.getText().toString();
            int count = 4;
            int timeout = 1;

            try {
                String countStr = etCount.getText().toString();
                if (!countStr.isEmpty()) count = Integer.parseInt(countStr);
            } catch (NumberFormatException e) {}

            try {
                String timeoutStr = etTimeout.getText().toString();
                if (!timeoutStr.isEmpty()) timeout = Integer.parseInt(timeoutStr);
            } catch (NumberFormatException e) {}

            tvOutput.setText("Running Ping...");
            final int finalCount = count;
            final int finalTimeout = timeout;

            new Thread(() -> {
                String result = NetworkUtils.ping(host, finalCount, finalTimeout);
                runOnUiThread(() -> tvOutput.setText(result));
            }).start();
        });
    }
}
