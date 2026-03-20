package io.github.trunone.network_tools;

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

    private EditText etHost;
    private EditText etCount;
    private EditText etTimeout;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ping, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_ping_host);
        etCount = view.findViewById(R.id.et_ping_count);
        etTimeout = view.findViewById(R.id.et_ping_timeout);
        tvOutput = view.findViewById(R.id.tv_ping_output);
        Button btnRun = view.findViewById(R.id.btn_run_ping);

        btnRun.setOnClickListener(v -> runPing());
    }

    private void runPing() {
        String host = etHost.getText().toString();
        String countStr = etCount.getText().toString();
        String timeoutStr = etTimeout.getText().toString();

        int count = 4;
        try {
            count = Integer.parseInt(countStr);
        } catch (NumberFormatException e) {
            // default
        }

        int timeout = 1;
        try {
            timeout = Integer.parseInt(timeoutStr);
        } catch (NumberFormatException e) {
            // default
        }

        tvOutput.setText("Pinging " + host + "...");
        final int finalCount = count;
        final int finalTimeout = timeout;

        new Thread(() -> {
            String result = NetworkUtils.ping(host, finalCount, finalTimeout);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
