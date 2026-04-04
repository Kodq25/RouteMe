package com.example.myapp.ui;


import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.database.FirestoreSeeder;
import com.example.myapp.viewmodel.MapViewModel;
import com.example.myapp.viewmodel.MapViewModelFactory;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity{

    private static final String KEY_SELECTED_BOTTOM_ITEM = "selected_bottom_item";

    private MapViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private MaterialToolbar topAppBar;

    private void initPermissionLauncher() {
        requestPermissionLauncher =
                registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
                    if (isGranted && viewModel != null) {
                        viewModel.startLocationUpdates();
                    } else {
                        Toast.makeText(this, "Location permission denied.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        initPermissionLauncher();

        viewModel = new ViewModelProvider(this, new MapViewModelFactory(getApplication()))
                .get(MapViewModel.class);

        viewModel.initDevice();

        FirestoreSeeder.seedSampleData();

        setupBottomNavigation();
        setupTopAppBarNavigation();
        requestLocationPermission();

        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (savedInstanceState == null) {
            switchTo(new SearchFragment(), false);
            MenuItem homeItem = bottomNavigation.getMenu().findItem(R.id.nav_home);
            if (homeItem != null) {
                homeItem.setChecked(true);
            }
            return;
        }

        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null) {
            syncBottomNavSelection(current);
            return;
        }
        int savedSelectedItemId = savedInstanceState.getInt(KEY_SELECTED_BOTTOM_ITEM, R.id.nav_home);
        ensureVisibleRootFragment(savedSelectedItemId);
        Fragment fallback = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (fallback != null) {
            syncBottomNavSelection(fallback);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            outState.putInt(KEY_SELECTED_BOTTOM_ITEM, bottomNavigation.getSelectedItemId());
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                clearBackStack();
                switchTo(new SearchFragment(), false);
                return true;
            }
            if (itemId == R.id.nav_route) {
                clearBackStack();
                switchTo(new MapFragment(), false);
                return true;
            }
            if (itemId == R.id.nav_menu) {
                clearBackStack();
                switchTo(new MenuFragment(), false);
                return true;
            }
            return false;
        });
        bottomNavigation.setOnItemReselectedListener(item -> {
            if (item.getItemId() == R.id.nav_menu) {
                clearBackStack();
                switchTo(new MenuFragment(), false);
            }
        });
    }

    private void setupTopAppBarNavigation() {
        topAppBar = findViewById(R.id.top_app_bar);
        if (topAppBar == null) {
            return;
        }
        topAppBar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        getSupportFragmentManager().addOnBackStackChangedListener(this::updateTopAppBarNavigation);
        updateTopAppBarNavigation();
    }

    private void updateTopAppBarNavigation() {
        if (topAppBar == null) {
            return;
        }
        boolean canGoBack = getSupportFragmentManager().getBackStackEntryCount() > 0;
        if (!canGoBack) {
            topAppBar.setNavigationIcon(null);
            return;
        }
        topAppBar.setNavigationIcon(androidx.appcompat.R.drawable.abc_ic_ab_back_material);
        topAppBar.setNavigationContentDescription(getString(R.string.back));
    }

    public void navigateToRoute() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_route);
            return;
        }
        clearBackStack();
        switchTo(new MapFragment(), false);
    }

    public void navigateToHome() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            return;
        }
        clearBackStack();
        switchTo(new SearchFragment(), false);
    }

    public void navigateToHistory() {
        switchTo(new HistoryFragment(), true);
    }

    public void navigateToSettings() {
        switchTo(new SettingsFragment(), true);
    }

    private void ensureVisibleRootFragment(int selectedBottomItemId) {
        Fragment current = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
        if (current != null) {
            return;
        }
        if (selectedBottomItemId == R.id.nav_route) {
            switchTo(new MapFragment(), false);
            return;
        }
        if (selectedBottomItemId == R.id.nav_menu) {
            switchTo(new MenuFragment(), false);
            return;
        }
        switchTo(new SearchFragment(), false);
    }

    private void syncBottomNavSelection(@NonNull Fragment currentFragment) {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation == null) {
            return;
        }
        int selectedId = mapBottomItemForFragment(currentFragment);
        MenuItem item = bottomNavigation.getMenu().findItem(selectedId);
        if (item != null) {
            item.setChecked(true);
        }
    }

    private int mapBottomItemForFragment(@NonNull Fragment fragment) {
        if (fragment instanceof MapFragment) {
            return R.id.nav_route;
        }
        if (fragment instanceof MenuFragment
                || fragment instanceof HistoryFragment
                || fragment instanceof SettingsFragment) {
            return R.id.nav_menu;
        }
        return R.id.nav_home;
    }

    private void switchTo(@NonNull Fragment fragment, boolean addToBackStack) {
        FragmentManager fragmentManager = getSupportFragmentManager();
        if (fragmentManager.isStateSaved()) {
            return;
        }
        Fragment current = fragmentManager.findFragmentById(R.id.fragment_container);
        if (current != null && current.getClass().equals(fragment.getClass())) {
            return;
        }
        androidx.fragment.app.FragmentTransaction transaction = fragmentManager
                .beginTransaction()
                .setReorderingAllowed(true)
                .replace(R.id.fragment_container, fragment);
        if (addToBackStack) {
            transaction.addToBackStack(fragment.getClass().getSimpleName());
            transaction.commit();
            return;
        }
        transaction.commitNow();
    }

    private void clearBackStack() {
        FragmentManager fragmentManager = getSupportFragmentManager();
        while (fragmentManager.getBackStackEntryCount() > 0) {
            fragmentManager.popBackStackImmediate();
        }
    }

    private void requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocationUpdates();
        } else {
            requestPermissionLauncher.launch(Manifest.permission.ACCESS_FINE_LOCATION);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (viewModel != null
                && ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            viewModel.startLocationUpdates();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (viewModel != null) {
            viewModel.stopLocationUpdates();
        }
    }
}
