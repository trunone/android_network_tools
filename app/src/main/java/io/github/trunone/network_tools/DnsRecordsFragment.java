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

public class DnsRecordsFragment extends Fragment {

    private EditText etHost;
    private EditText etType;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dns_records, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etHost = view.findViewById(R.id.et_doh_host);
        etType = view.findViewById(R.id.et_doh_type);
        tvOutput = view.findViewById(R.id.tv_doh_output);
        Button btnRun = view.findViewById(R.id.btn_run_doh);

        btnRun.setOnClickListener(v -> runDohQuery());
    }

    private void runDohQuery() {
        String host = etHost.getText().toString();
        String type = etType.getText().toString().trim();
        tvOutput.setText("Querying " + (type.isEmpty() ? "A" : type) + " records for " + host + " via DoH...");

        new Thread(() -> {
            String result = NetworkUtils.queryDnsRecords(host, type);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
