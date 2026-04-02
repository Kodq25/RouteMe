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
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.R;
import com.example.myapp.database.FirestoreSeeder;
import com.example.myapp.viewmodel.MapViewModel;
import com.example.myapp.viewmodel.MapViewModelFactory;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity{


    private MapViewModel viewModel;
    private ActivityResultLauncher<String> requestPermissionLauncher;

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
        requestLocationPermission();

        if (savedInstanceState == null) {
            BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
            switchTo(new SearchFragment());
            MenuItem homeItem = bottomNavigation.getMenu().findItem(R.id.nav_home);
            if (homeItem != null) {
                homeItem.setChecked(true);
            }
        }
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        bottomNavigation.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();
            if (itemId == R.id.nav_home) {
                switchTo(new SearchFragment());
                return true;
            }
            if (itemId == R.id.nav_route) {
                switchTo(new MapFragment());
                return true;
            }
            if (itemId == R.id.nav_menu) {
                switchTo(new MenuFragment());
                return true;
            }
            return false;
        });
    }

    public void navigateToRoute() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_route);
            return;
        }
        switchTo(new MapFragment());
    }

    public void navigateToHome() {
        BottomNavigationView bottomNavigation = findViewById(R.id.bottom_navigation);
        if (bottomNavigation != null) {
            bottomNavigation.setSelectedItemId(R.id.nav_home);
            return;
        }
        switchTo(new SearchFragment());
    }

    public void navigateToHistory() {
        switchTo(new HistoryFragment());
    }

    public void navigateToSettings() {
        switchTo(new SettingsFragment());
    }

    private void switchTo(@NonNull androidx.fragment.app.Fragment fragment) {
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragment_container, fragment)
                .commit();
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
