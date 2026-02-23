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

public class PingFragment extends Fragment {

    private EditText etHost, etCount, etTimeout;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ping, container, false);

        etHost = view.findViewById(R.id.et_host);
        etCount = view.findViewById(R.id.et_count);
        etTimeout = view.findViewById(R.id.et_timeout);
        tvOutput = view.findViewById(R.id.tv_output);
        Button btnRun = view.findViewById(R.id.btn_run);

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
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvOutput.setText(result));
                }
            }).start();
        });

        return view;
    }
}
