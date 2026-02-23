package com.example.networktools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class ArpFragment extends Fragment {

    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_arp, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOutput = view.findViewById(R.id.tv_arp_output);
        Button btnRefresh = view.findViewById(R.id.btn_refresh_arp);

        btnRefresh.setOnClickListener(v -> loadArpTable());

        // Auto load on first view
        loadArpTable();
    }

    private void loadArpTable() {
        tvOutput.setText("Loading ARP table...");
        new Thread(() -> {
            String result = NetworkUtils.getArpTable();
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
