package com.example.myapp.ui;

import android.content.Intent;
import android.graphics.Color;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.myapp.BuildConfig;
import com.example.myapp.viewmodel.MapViewModel;
import com.example.myapp.R;

import org.osmdroid.config.Configuration;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.views.MapView;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;

import java.util.List;

public class MapFragment extends Fragment {

    private MapViewModel viewModel;
    private MapView mapView;
    private Marker currentMarker;
    private Marker destinationMarker;
    private Polyline routeLine;
    private boolean followLocation = true;
    private TextView routeDebug;

    public MapFragment() {
        super(R.layout.fragment_map);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_map, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        viewModel = new ViewModelProvider(requireActivity()).get(MapViewModel.class);

        Configuration.getInstance().setUserAgentValue(BuildConfig.APPLICATION_ID);

        if (!viewModel.isGpsEnabled()) {
            showGpsDisabledDialog();
        }

        mapView = view.findViewById(R.id.map_view);
        routeDebug = view.findViewById(R.id.route_debug);
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setMultiTouchControls(true);
        mapView.getController().setZoom(15.0);
        mapView.addMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                followLocation = false;
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                followLocation = false;
                return false;
            }
        });

        viewModel.getCurrentLocation().observe(getViewLifecycleOwner(), this::updateLocationOnMap);
        viewModel.getRoutePoints().observe(getViewLifecycleOwner(), this::updateRouteOnMap);
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
    }

    private void updateLocationOnMap(@Nullable Location location) {
        if (mapView == null || location == null) {
            return;
        }

        GeoPoint point = new GeoPoint(location.getLatitude(), location.getLongitude());

        if (currentMarker == null) {
            currentMarker = new Marker(mapView);
            currentMarker.setTitle(getString(R.string.current_location));
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
            mapView.getOverlays().add(currentMarker);
        }

        currentMarker.setPosition(point);
        if (followLocation) {
            mapView.getController().animateTo(point);
        }
        mapView.invalidate();
    }

    private void updateRouteOnMap(@Nullable List<GeoPoint> points) {
        if (mapView == null) {
            return;
        }

        updateRouteDebug(points);

        if (routeLine != null) {
            mapView.getOverlays().remove(routeLine);
            routeLine = null;
        }
        if (destinationMarker != null) {
            mapView.getOverlays().remove(destinationMarker);
            destinationMarker = null;
        }

        if (points == null || points.size() < 2) {
            mapView.invalidate();
            return;
        }

        routeLine = new Polyline();
        routeLine.setPoints(points);
        routeLine.setColor(Color.parseColor("#00C853"));
        routeLine.setWidth(10f);
        mapView.getOverlays().add(0, routeLine);

        GeoPoint destination = points.get(points.size() - 1);
        destinationMarker = new Marker(mapView);
        destinationMarker.setTitle("Destination");
        destinationMarker.setPosition(destination);
        destinationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        destinationMarker.setIcon(ContextCompat.getDrawable(requireContext(), android.R.drawable.ic_menu_compass));
        mapView.getOverlays().add(destinationMarker);

        BoundingBox box = buildBoundingBox(points);
        if (box != null) {
            mapView.zoomToBoundingBox(box, true, 64);
        }

        mapView.invalidate();
    }

    @Nullable
    private BoundingBox buildBoundingBox(@NonNull List<GeoPoint> points) {
        if (points.isEmpty()) {
            return null;
        }

        double minLat = Double.MAX_VALUE;
        double maxLat = -Double.MAX_VALUE;
        double minLon = Double.MAX_VALUE;
        double maxLon = -Double.MAX_VALUE;

        for (GeoPoint point : points) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }

        return new BoundingBox(maxLat, maxLon, minLat, minLon);
    }

    private void updateRouteDebug(@Nullable List<GeoPoint> points) {
        if (routeDebug == null) {
            return;
        }
        int count = points == null ? 0 : points.size();
        routeDebug.setText("Route points: " + count);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mapView != null) {
            mapView.onDetach();
            mapView = null;
            currentMarker = null;
            destinationMarker = null;
            routeLine = null;
            followLocation = true;
        }
        routeDebug = null;
    }

    private void showGpsDisabledDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("GPS is disabled")
                .setMessage("Please enable GPS to use this feature.")
                .setCancelable(false)
                .setPositiveButton("Enable GPS", (dialog, which) ->
                        startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)))
                .setNegativeButton("Dismiss", (dialog, which) -> dialog.dismiss())
                .show();
    }
}
