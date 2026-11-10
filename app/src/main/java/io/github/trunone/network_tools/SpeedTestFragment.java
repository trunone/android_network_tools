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

public class SpeedTestFragment extends Fragment {

    private TextView tvStatus;
    private TextView tvOutput;
    private Button btnRun;
    private Thread testThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_speed_test, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvStatus = view.findViewById(R.id.tv_speed_status);
        tvOutput = view.findViewById(R.id.tv_speed_output);
        btnRun = view.findViewById(R.id.btn_run_speed_test);

        btnRun.setOnClickListener(v -> startSpeedTest());
    }

    private void startSpeedTest() {
        btnRun.setEnabled(false);
        tvStatus.setText("Test in progress...");
        tvOutput.setText("Initializing speed test...\n");

        testThread = new Thread(() -> {
            String result = NetworkUtils.runSpeedTest(message -> {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> tvStatus.setText(message));
                }
            });

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    tvOutput.setText(result);
                    tvStatus.setText("Speed test finished.");
                    btnRun.setEnabled(true);
                });
            }
        });
        testThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (testThread != null && testThread.isAlive()) {
            testThread.interrupt();
        }
    }
}
