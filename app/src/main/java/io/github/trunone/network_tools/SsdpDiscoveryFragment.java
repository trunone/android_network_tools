package io.github.trunone.network_tools;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class SsdpDiscoveryFragment extends Fragment {

    private TextView tvOutput;
    private Button btnRun;
    private Thread discoveryThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_ssdp_discovery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvOutput = view.findViewById(R.id.tv_ssdp_output);
        btnRun = view.findViewById(R.id.btn_run_ssdp);

        btnRun.setOnClickListener(v -> startDiscovery());
    }

    private void startDiscovery() {
        btnRun.setEnabled(false);
        tvOutput.setText("Broadcasting SSDP M-SEARCH packet to 239.255.255.250:1900...\nWaiting for device responses...\n");

        discoveryThread = new Thread(() -> {
            String result = NetworkUtils.discoverSsdpDevices(3500);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvOutput.setText(result);
                    btnRun.setEnabled(true);
                });
            }
        });
        discoveryThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (discoveryThread != null && discoveryThread.isAlive()) {
            discoveryThread.interrupt();
        }
    }
}
