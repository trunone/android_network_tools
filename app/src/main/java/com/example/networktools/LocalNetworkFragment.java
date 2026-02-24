package com.example.networktools;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class LocalNetworkFragment extends Fragment {

    private TextView tvNetworkInfo;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_local_network, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        tvNetworkInfo = view.findViewById(R.id.tv_network_info);
        Button btnRefresh = view.findViewById(R.id.btn_refresh);

        btnRefresh.setOnClickListener(v -> refreshInfo());

        refreshInfo();
    }

    private void refreshInfo() {
        tvNetworkInfo.setText("Loading...");
        Context context = getContext();
        new Thread(() -> {
            String info = NetworkUtils.getLocalNetworkInfo(context);
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> tvNetworkInfo.setText(info));
            }
        }).start();
    }
}
