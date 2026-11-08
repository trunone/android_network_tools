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

public class HttpHeaderFragment extends Fragment {

    private EditText etUrl;
    private TextView tvOutput;
    private Thread workerThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_http_header, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etUrl = view.findViewById(R.id.et_http_url);
        tvOutput = view.findViewById(R.id.tv_http_output);
        Button btnRun = view.findViewById(R.id.btn_run_http_inspect);

        btnRun.setOnClickListener(v -> runInspect());
    }

    private void runInspect() {
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        String url = etUrl.getText().toString().trim();
        tvOutput.setText(R.string.loading);

        workerThread = new Thread(() -> {
            String result = NetworkUtils.httpInspect(url);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        });
        workerThread.start();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }
    }
}
