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

public class PortScannerFragment extends Fragment {

    private EditText etHost;
    private EditText etStartPort;
    private EditText etEndPort;
    private EditText etTimeout;
    private TextView tvOutput;
    private Thread scanThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_port_scanner, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_port_host);
        etStartPort = view.findViewById(R.id.et_port_start);
        etEndPort = view.findViewById(R.id.et_port_end);
        etTimeout = view.findViewById(R.id.et_port_timeout);
        tvOutput = view.findViewById(R.id.tv_port_output);
        Button btnRun = view.findViewById(R.id.btn_run_port_scan);

        btnRun.setOnClickListener(v -> runPortScan());
    }

    private void runPortScan() {
        if (scanThread != null && scanThread.isAlive()) {
            scanThread.interrupt();
        }

        String host = etHost.getText().toString();
        int startPort = 1;
        try {
            startPort = Integer.parseInt(etStartPort.getText().toString());
        } catch (NumberFormatException e) {
            // default
        }

        int endPort = 1024;
        try {
            endPort = Integer.parseInt(etEndPort.getText().toString());
        } catch (NumberFormatException e) {
            // default
        }

        int timeout = 200;
        try {
            timeout = Integer.parseInt(etTimeout.getText().toString());
        } catch (NumberFormatException e) {
            // default
        }

        tvOutput.setText("Scanning " + host + " (ports " + startPort + "-" + endPort + ")...");
        final int finalStart = startPort;
        final int finalEnd = endPort;
        final int finalTimeout = timeout;

        scanThread = new Thread(() -> {
            String result = NetworkUtils.scanPorts(host, finalStart, finalEnd, finalTimeout);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        });
        scanThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (scanThread != null && scanThread.isAlive()) {
            scanThread.interrupt();
        }
    }
}
