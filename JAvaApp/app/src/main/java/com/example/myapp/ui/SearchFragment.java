package com.example.myapp.ui;

import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.viewmodel.MapViewModel;
import com.example.myapp.R;
import com.example.myapp.storage.SearchHistoryStore;

import org.osmdroid.util.GeoPoint;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SearchFragment extends Fragment {

    private static final long ADDRESS_RESOLVE_INTERVAL_MS = 15000L;
    private static final float ADDRESS_RESOLVE_DISTANCE_METERS = 50f;

    private MapViewModel viewModel;
    private EditText currentLocationInput;
    private EditText destinationInput;
    private Location lastLocation;
    private Location lastAddressLocation;
    private long lastAddressResolveMs;
    private final ExecutorService geocodeExecutor = Executors.newSingleThreadExecutor();

    public SearchFragment() {
        super(R.layout.fragment_search);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        currentLocationInput = view.findViewById(R.id.input_current_location);
        destinationInput = view.findViewById(R.id.input_destination);
        ImageButton currentLocationButton = view.findViewById(R.id.button_current_location);
        Button searchButton = view.findViewById(R.id.button_search);

        viewModel.getCurrentLocation().observe(getViewLifecycleOwner(), location -> {
            if (location == null) {
                return;
            }
            lastLocation = location;
            boolean isCurrentLocationEmpty = currentLocationInput == null
                    || currentLocationInput.getText().toString().trim().isEmpty();
            if (isCurrentLocationEmpty || shouldResolveAddress(location)) {
                resolveAddress(location, this::updateCurrentLocationText);
            }
        });

        viewModel.getPendingDestinationQuery().observe(getViewLifecycleOwner(), query -> {
            if (query == null || query.trim().isEmpty()) {
                return;
            }
            if (destinationInput != null) {
                destinationInput.setText(query);
            }
            viewModel.clearPendingDestinationQuery();
        });

        currentLocationButton.setOnClickListener(v -> viewModel.startLocationUpdates());
        searchButton.setOnClickListener(v -> {
            requestRouteForDestination();
            if (getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).navigateToRoute();
            }
        });
    }

    @SuppressWarnings("deprecation")
    private void resolveAddress(@NonNull Location location, @NonNull Consumer<String> callback) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        double latitude = location.getLatitude();
        double longitude = location.getLongitude();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocation(latitude, longitude, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    String address = extractAddress(addresses);
                    if (address != null) {
                        callback.accept(address);
                    } else {
                        callback.accept(getString(R.string.current_location_unavailable));
                    }
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    callback.accept(getString(R.string.current_location_unavailable));
                }
            });
            return;
        }

        geocodeExecutor.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                String address = extractAddress(addresses);
                if (address != null) {
                    callback.accept(address);
                } else {
                    callback.accept(getString(R.string.current_location_unavailable));
                }
            } catch (IOException ignored) {
                callback.accept(getString(R.string.current_location_unavailable));
            }
        });
    }

    @Nullable
    private String extractAddress(@Nullable List<Address> addresses) {
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);

            String line = address.getAddressLine(0);
            if (line != null && !line.isEmpty()) {
                return line;
            }

            if (address.getLocality() != null) {
                return address.getLocality();
            }

            if (address.getAdminArea() != null) {
                return address.getAdminArea();
            }

            if (address.getCountryName() != null) {
                return address.getCountryName();
            }
        }
        return null;
    }

    private void requestRouteForDestination() {
        if (destinationInput == null) {
            return;
        }

        String destinationText = destinationInput.getText().toString().trim();
        if (destinationText.isEmpty()) {
            showToast("Enter a destination.");
            return;
        }

        if (lastLocation == null) {
            showToast("Current location is not available yet.");
            return;
        }

        SearchHistoryStore.recordQuery(requireContext(), destinationText);

        GeoPoint origin = new GeoPoint(lastLocation.getLatitude(), lastLocation.getLongitude());
        GeoPoint directPoint = parseLatLon(destinationText);
        if (directPoint != null) {
            viewModel.requestRoute(origin, directPoint);
            return;
        }

        resolveDestination(destinationText, destinationPoint -> {
            viewModel.requestRoute(origin, destinationPoint);
        });
    }

    @SuppressWarnings("deprecation")
    private void resolveDestination(@NonNull String query, @NonNull Consumer<GeoPoint> callback) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            geocoder.getFromLocationName(query, 1, new Geocoder.GeocodeListener() {
                @Override
                public void onGeocode(@NonNull List<Address> addresses) {
                    GeoPoint point = extractGeoPoint(addresses);
                    if (point == null) {
                        resolveDestinationWithFallback(query, callback);
                        return;
                    }
                    callback.accept(point);
                }

                @Override
                public void onError(@NonNull String errorMessage) {
                    resolveDestinationWithFallback(query, callback);
                }
            });
            return;
        }

        geocodeExecutor.execute(() -> {
            try {
                List<Address> addresses = geocoder.getFromLocationName(query, 1);
                GeoPoint point = extractGeoPoint(addresses);
                if (point != null) {
                    callback.accept(point);
                    return;
                }
                resolveDestinationWithFallback(query, callback);
            } catch (IOException ignored) {
                resolveDestinationWithFallback(query, callback);
            }
        });
    }

    private void resolveDestinationWithFallback(@NonNull String query, @NonNull Consumer<GeoPoint> callback) {
        geocodeExecutor.execute(() -> {
            try {
                GeoPoint point = fetchGeoPointFromNominatim(query);
                if (point != null) {
                    callback.accept(point);
                    return;
                }
                showToast("Destination not found.");
            } catch (IOException | JSONException ignored) {
                showToast("Destination not found.");
            }
        });
    }

    @Nullable
    private GeoPoint fetchGeoPointFromNominatim(@NonNull String query) throws IOException, JSONException {
        String encoded = encodeQuery(query);
        String url = "https://nominatim.openstreetmap.org/search?q=" + encoded + "&format=json&limit=1";
        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");
        connection.setRequestProperty("User-Agent", "MyApp/1.0 (android)");

        try (InputStream inputStream = connection.getInputStream()) {
            String json = readStream(inputStream);
            JSONArray results = new JSONArray(json);
            if (results.length() == 0) {
                return null;
            }
            JSONObject first = results.getJSONObject(0);
            double lat = Double.parseDouble(first.getString("lat"));
            double lon = Double.parseDouble(first.getString("lon"));
            return new GeoPoint(lat, lon);
        } finally {
            connection.disconnect();
        }
    }

    @NonNull
    private String encodeQuery(@NonNull String query) throws IOException {
        try {
            return URLEncoder.encode(query, "UTF-8");
        } catch (java.io.UnsupportedEncodingException e) {
            throw new IOException("UTF-8 encoding not supported", e);
        }
    }

    @NonNull
    private String readStream(@NonNull InputStream inputStream) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                builder.append(line);
            }
        }
        return builder.toString();
    }

    @Nullable
    private GeoPoint extractGeoPoint(@Nullable List<Address> addresses) {
        if (addresses != null && !addresses.isEmpty()) {
            Address address = addresses.get(0);
            return new GeoPoint(address.getLatitude(), address.getLongitude());
        }
        return null;
    }

    @Nullable
    private GeoPoint parseLatLon(@NonNull String value) {
        String normalized = value.trim().replaceAll("\\s+", " ");
        String[] parts = normalized.split(",");
        if (parts.length != 2) {
            parts = normalized.split(" ");
            if (parts.length != 2) {
                return null;
            }
        }

        try {
            double lat = Double.parseDouble(parts[0].trim());
            double lon = Double.parseDouble(parts[1].trim());
            if (lat < -90.0 || lat > 90.0 || lon < -180.0 || lon > 180.0) {
                return null;
            }
            return new GeoPoint(lat, lon);
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private void showToast(@NonNull String message) {
        new Handler(Looper.getMainLooper()).post(() -> {
            if (getContext() == null) {
                return;
            }
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    private void updateCurrentLocationText(@NonNull String text) {
        if (currentLocationInput == null) {
            return;
        }
        currentLocationInput.post(() -> {
            if (currentLocationInput != null) {
                currentLocationInput.setText(text);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        currentLocationInput = null;
        destinationInput = null;
        lastLocation = null;
        lastAddressLocation = null;
        lastAddressResolveMs = 0L;
    }

    private boolean shouldResolveAddress(@NonNull Location location) {
        long now = System.currentTimeMillis();
        if (lastAddressLocation == null) {
            lastAddressLocation = new Location(location);
            lastAddressResolveMs = now;
            return true;
        }
        float distance = location.distanceTo(lastAddressLocation);
        if (distance < ADDRESS_RESOLVE_DISTANCE_METERS
                && (now - lastAddressResolveMs) < ADDRESS_RESOLVE_INTERVAL_MS) {
            return false;
        }
        lastAddressLocation.set(location);
        lastAddressResolveMs = now;
        return true;
    }
}
