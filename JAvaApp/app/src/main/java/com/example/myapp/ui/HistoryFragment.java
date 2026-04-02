package com.example.myapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.storage.SearchHistoryStore;
import com.example.myapp.viewmodel.MapViewModel;

import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class HistoryFragment extends Fragment {

    private final List<String> rows = new ArrayList<>();
    private final List<SearchHistoryStore.Entry> entries = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private ListView historyList;
    private TextView emptyView;
    private Button clearButton;
    private MapViewModel viewModel;

    public HistoryFragment() {
        super(R.layout.fragment_history);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_history, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);
        historyList = view.findViewById(R.id.history_list);
        emptyView = view.findViewById(R.id.history_empty);
        clearButton = view.findViewById(R.id.history_clear_button);
        adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                rows
        );
        historyList.setAdapter(adapter);
        historyList.setOnItemClickListener((parent, itemView, position, id) -> {
            if (position < 0 || position >= entries.size()) {
                return;
            }
            String query = entries.get(position).getQuery();
            viewModel.setPendingDestinationQuery(query);
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToHome();
            }
        });
        historyList.setOnItemLongClickListener((parent, itemView, position, id) -> {
            if (position < 0 || position >= entries.size()) {
                return false;
            }
            confirmDeleteEntry(entries.get(position));
            return true;
        });
        if (clearButton != null) {
            clearButton.setOnClickListener(v -> confirmClearAll());
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        reloadHistory();
    }

    private void reloadHistory() {
        List<SearchHistoryStore.Entry> loaded =
                SearchHistoryStore.loadHistory(requireContext());
        rows.clear();
        entries.clear();
        entries.addAll(loaded);
        DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.SHORT, DateFormat.SHORT);
        for (SearchHistoryStore.Entry entry : loaded) {
            String line = entry.getQuery();
            if (entry.getTimestampMs() > 0) {
                line = line + " - " + formatter.format(new Date(entry.getTimestampMs()));
            }
            rows.add(line);
        }
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
        boolean isEmpty = rows.isEmpty();
        if (emptyView != null) {
            emptyView.setVisibility(isEmpty ? View.VISIBLE : View.GONE);
        }
        if (historyList != null) {
            historyList.setVisibility(isEmpty ? View.GONE : View.VISIBLE);
        }
        if (clearButton != null) {
            clearButton.setEnabled(!isEmpty);
        }
    }

    private void confirmClearAll() {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_clear_confirm_title)
                .setMessage(R.string.history_clear_confirm_message)
                .setPositiveButton(R.string.history_clear_all, (dialog, which) -> {
                    SearchHistoryStore.clearHistory(requireContext());
                    reloadHistory();
                    Toast.makeText(requireContext(), R.string.history_cleared_toast, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }

    private void confirmDeleteEntry(@NonNull SearchHistoryStore.Entry entry) {
        new AlertDialog.Builder(requireContext())
                .setTitle(R.string.history_delete_title)
                .setMessage(R.string.history_delete_message)
                .setPositiveButton(R.string.history_delete_action, (dialog, which) -> {
                    SearchHistoryStore.deleteQuery(requireContext(), entry.getQuery());
                    reloadHistory();
                    Toast.makeText(requireContext(), R.string.history_deleted_toast, Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(android.R.string.cancel, (dialog, which) -> dialog.dismiss())
                .show();
    }
}
