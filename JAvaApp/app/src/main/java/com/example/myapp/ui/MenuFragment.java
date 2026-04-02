package com.example.myapp.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.myapp.R;

public class MenuFragment extends Fragment {

    private static final String[] MENU_ITEMS = {"History", "Settings", "Help"};

    public MenuFragment() {
        super(R.layout.fragment_menu);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_menu, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ListView menuList = view.findViewById(R.id.menu_list);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_list_item_1,
                MENU_ITEMS
        );
        menuList.setAdapter(adapter);
        menuList.setOnItemClickListener((parent, itemView, position, id) -> {
            String item = MENU_ITEMS[position];
            if ("History".equals(item)) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToHistory();
                }
                return;
            }
            if ("Settings".equals(item)) {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).navigateToSettings();
                }
                return;
            }
            Toast.makeText(requireContext(), item, Toast.LENGTH_SHORT).show();
        });
    }
}
