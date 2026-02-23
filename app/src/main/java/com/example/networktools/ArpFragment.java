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
        View view = inflater.inflate(R.layout.fragment_arp, container, false);

        tvOutput = view.findViewById(R.id.tv_output);
        Button btnRun = view.findViewById(R.id.btn_run);

        btnRun.setOnClickListener(v -> {
            tvOutput.setText("Fetching ARP Table...");
            new Thread(() -> {
                String result = NetworkUtils.getArpTable();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvOutput.setText(result));
                }
            }).start();
        });

        return view;
    }
}
