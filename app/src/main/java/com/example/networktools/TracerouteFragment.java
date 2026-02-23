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

    private EditText etHost;
    private EditText etMaxHops;
    private EditText etTimeout;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_traceroute, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_trace_host);
        etMaxHops = view.findViewById(R.id.et_trace_hops);
        etTimeout = view.findViewById(R.id.et_trace_timeout);
        tvOutput = view.findViewById(R.id.tv_trace_output);
        Button btnRun = view.findViewById(R.id.btn_run_trace);

        btnRun.setOnClickListener(v -> runTraceroute());
    }

    private void runTraceroute() {
        String host = etHost.getText().toString();
        String hopsStr = etMaxHops.getText().toString();
        String timeoutStr = etTimeout.getText().toString();

        int maxHops = 30;
        try {
            maxHops = Integer.parseInt(hopsStr);
        } catch (NumberFormatException e) {
            // default
        }

        int timeout = 1;
        try {
            timeout = Integer.parseInt(timeoutStr);
        } catch (NumberFormatException e) {
            // default
        }

        tvOutput.setText("Tracing route to " + host + "...");
        final int finalMaxHops = maxHops;
        final int finalTimeout = timeout;

        new Thread(() -> {
            String result = NetworkUtils.traceroute(host, finalMaxHops, finalTimeout);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
