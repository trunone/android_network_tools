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

public class WakeOnLanFragment extends Fragment {

    private EditText etMac;
    private EditText etBroadcast;
    private EditText etPort;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_wake_on_lan, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etMac = view.findViewById(R.id.et_wol_mac);
        etBroadcast = view.findViewById(R.id.et_wol_broadcast);
        etPort = view.findViewById(R.id.et_wol_port);
        tvOutput = view.findViewById(R.id.tv_wol_output);
        Button btnSend = view.findViewById(R.id.btn_send_wol);

        btnSend.setOnClickListener(v -> sendWol());
    }

    private void sendWol() {
        String mac = etMac.getText().toString();
        String broadcast = etBroadcast.getText().toString();
        int port = 9;
        try {
            port = Integer.parseInt(etPort.getText().toString());
        } catch (NumberFormatException e) {
            // default
        }

        tvOutput.setText("Sending Wake-on-LAN magic packet...");
        final int finalPort = port;

        new Thread(() -> {
            String result = NetworkUtils.sendWakeOnLan(mac, broadcast, finalPort);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
