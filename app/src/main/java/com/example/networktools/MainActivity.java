package com.example.networktools;

import android.os.Bundle;
import android.widget.Button;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnPing = findViewById(R.id.btn_menu_ping);
        Button btnTraceroute = findViewById(R.id.btn_menu_traceroute);
        Button btnArp = findViewById(R.id.btn_menu_arp);

        btnPing.setOnClickListener(v -> loadFragment(new PingFragment()));
        btnTraceroute.setOnClickListener(v -> loadFragment(new TracerouteFragment()));
        btnArp.setOnClickListener(v -> loadFragment(new ArpFragment()));

        // Load default fragment
        if (savedInstanceState == null) {
            loadFragment(new PingFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
