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

public class SslCertFragment extends Fragment {

    private EditText etHost;
    private EditText etPort;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssl_cert, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_ssl_host);
        etPort = view.findViewById(R.id.et_ssl_port);
        tvOutput = view.findViewById(R.id.tv_ssl_output);
        Button btnRun = view.findViewById(R.id.btn_run_ssl);

        btnRun.setOnClickListener(v -> runSslInspect());
    }

    private void runSslInspect() {
        String host = etHost.getText().toString();
        int port = 443;
        try {
            port = Integer.parseInt(etPort.getText().toString().trim());
        } catch (Exception ignored) {
        }
        final int targetPort = port;
        tvOutput.setText("Inspecting SSL/TLS certificate for " + host + ":" + targetPort + "...");

        new Thread(() -> {
            String result = NetworkUtils.getSslCertificateInfo(host, targetPort, 10000);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
