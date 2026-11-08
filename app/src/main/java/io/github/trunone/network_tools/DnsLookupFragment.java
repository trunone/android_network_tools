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

public class DnsLookupFragment extends Fragment {

    private EditText etHost;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dns_lookup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_dns_host);
        tvOutput = view.findViewById(R.id.tv_dns_output);
        Button btnRun = view.findViewById(R.id.btn_run_dns);

        btnRun.setOnClickListener(v -> runDnsLookup());
    }

    private void runDnsLookup() {
        String host = etHost.getText().toString();
        tvOutput.setText("Resolving " + host + "...");

        new Thread(() -> {
            String result = NetworkUtils.dnsLookup(host);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
