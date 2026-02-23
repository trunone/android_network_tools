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

        Button btnLocalNetwork = findViewById(R.id.btn_nav_local_network);
        Button btnPing = findViewById(R.id.btn_nav_ping);
        Button btnTraceroute = findViewById(R.id.btn_nav_traceroute);
        Button btnArp = findViewById(R.id.btn_nav_arp);

        btnLocalNetwork.setOnClickListener(v -> loadFragment(new LocalNetworkFragment()));
        btnPing.setOnClickListener(v -> loadFragment(new PingFragment()));
        btnTraceroute.setOnClickListener(v -> loadFragment(new TracerouteFragment()));
        btnArp.setOnClickListener(v -> loadFragment(new ArpFragment()));

        // Default fragment
        if (savedInstanceState == null) {
            loadFragment(new LocalNetworkFragment());
        }
    }

    private void loadFragment(Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
    }
}
