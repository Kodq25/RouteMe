package com.example.myapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.widget.SwitchCompat;
import androidx.fragment.app.Fragment;

import com.example.myapp.BuildConfig;
import com.example.myapp.R;
import com.example.myapp.storage.SearchHistoryStore;
import com.example.myapp.storage.SettingsStore;

public class SettingsFragment extends Fragment {

    public SettingsFragment() {
        super(R.layout.fragment_settings);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        SwitchCompat autoRerouteSwitch = view.findViewById(R.id.settings_auto_reroute);
        Button clearHistoryButton = view.findViewById(R.id.settings_clear_history);
        TextView versionView = view.findViewById(R.id.settings_app_version);

        autoRerouteSwitch.setChecked(SettingsStore.isAutoRerouteEnabled(requireContext()));
        autoRerouteSwitch.setOnCheckedChangeListener(
                (buttonView, isChecked) ->
                        SettingsStore.setAutoRerouteEnabled(requireContext(), isChecked));

        clearHistoryButton.setOnClickListener(v -> confirmClearHistory());

        versionView.setText(getString(R.string.settings_app_version, BuildConfig.VERSION_NAME));
    }

    private void confirmClearHistory() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_clear_confirm_title)
                .setMessage(R.string.history_clear_confirm_message)
                .setPositiveButton(R.string.history_clear_all, (dialog, which) -> {
                    SearchHistoryStore.clearHistory(requireContext());
                    Toast.makeText(requireContext(), R.string.history_cleared_toast, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
