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

public class WhoisFragment extends Fragment {

    private EditText etQuery;
    private TextView tvOutput;
    private Thread workerThread;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_whois, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etQuery = view.findViewById(R.id.et_whois_query);
        tvOutput = view.findViewById(R.id.tv_whois_output);
        Button btnRun = view.findViewById(R.id.btn_run_whois);

        btnRun.setOnClickListener(v -> runQuery());
    }

    private void runQuery() {
        if (workerThread != null && workerThread.isAlive()) {
            workerThread.interrupt();
        }

        String query = etQuery.getText().toString().trim();
        tvOutput.setText(R.string.loading);

        workerThread = new Thread(() -> {
            String result = NetworkUtils.whois(query);
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
