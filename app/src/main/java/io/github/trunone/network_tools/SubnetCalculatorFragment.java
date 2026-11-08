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

public class SubnetCalculatorFragment extends Fragment {

    private EditText etIp;
    private EditText etPrefix;
    private TextView tvOutput;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_subnet_calculator, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        etIp = view.findViewById(R.id.et_subnet_ip);
        etPrefix = view.findViewById(R.id.et_subnet_prefix);
        tvOutput = view.findViewById(R.id.tv_subnet_output);
        Button btnCalculate = view.findViewById(R.id.btn_calculate_subnet);

        btnCalculate.setOnClickListener(v -> calculateSubnet());

        // Calculate on load
        calculateSubnet();
    }

    private void calculateSubnet() {
        String ip = etIp.getText().toString();
        int prefix = 24;
        try {
            prefix = Integer.parseInt(etPrefix.getText().toString());
        } catch (NumberFormatException e) {
            // default
        }

        String result = NetworkUtils.calculateSubnet(ip, prefix);
        tvOutput.setText(result);
    }
}
