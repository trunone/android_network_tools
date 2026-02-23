package com.example.networktools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class TracerouteFragment extends Fragment {

    private EditText etHost, etMaxHops, etTimeout;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_traceroute, container, false);

        etHost = view.findViewById(R.id.et_host);
        etMaxHops = view.findViewById(R.id.et_max_hops);
        etTimeout = view.findViewById(R.id.et_timeout);
        tvOutput = view.findViewById(R.id.tv_output);
        Button btnRun = view.findViewById(R.id.btn_run);

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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvOutput.setText(result));
                }
            }).start();
        });

        return view;
    }
}
