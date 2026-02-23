package com.example.networktools;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private EditText etHost;
    private TextView tvOutput;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        etHost = findViewById(R.id.et_host);
        tvOutput = findViewById(R.id.tv_output);

        Button btnPing = findViewById(R.id.btn_ping);
        Button btnTraceroute = findViewById(R.id.btn_traceroute);
        Button btnArp = findViewById(R.id.btn_arp);

        btnPing.setOnClickListener(v -> runTool("ping"));
        btnTraceroute.setOnClickListener(v -> runTool("traceroute"));
        btnArp.setOnClickListener(v -> runTool("arp"));
    }

    private void runTool(String tool) {
        String host = etHost.getText().toString();
        tvOutput.setText("Running " + tool + "...");

        new Thread(() -> {
            String result = "";
            switch (tool) {
                case "ping":
                    result = NetworkUtils.ping(host);
                    break;
                case "traceroute":
                    result = NetworkUtils.traceroute(host);
                    break;
                case "arp":
                    result = NetworkUtils.getArpTable();
                    break;
            }
            final String finalResult = result;
            runOnUiThread(() -> tvOutput.setText(finalResult));
        }).start();
    }
}
