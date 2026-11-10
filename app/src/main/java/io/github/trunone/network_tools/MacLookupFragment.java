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

public class MacLookupFragment extends Fragment {

    private EditText etQuery;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_mac_lookup, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etQuery = view.findViewById(R.id.et_mac_query);
        tvOutput = view.findViewById(R.id.tv_mac_output);
        Button btnRun = view.findViewById(R.id.btn_run_mac_lookup);

        btnRun.setOnClickListener(v -> runMacLookup());
    }

    private void runMacLookup() {
        String query = etQuery.getText().toString();
        tvOutput.setText("Looking up hardware manufacturer for " + query + "...");

        new Thread(() -> {
            String result = NetworkUtils.lookupMacVendor(query);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvOutput.setText(result));
            }
        }).start();
    }
}
