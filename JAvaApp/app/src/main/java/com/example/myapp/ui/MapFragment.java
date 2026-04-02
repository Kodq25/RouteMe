package com.example.myapp.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.os.Bundle;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.Surface;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MapFragment extends Fragment {

    private static final float DIRECTION_ICON_OFFSET_DEGREES = 0f;
    private static final float AZIMUTH_DEAD_BAND_DEGREES = 2f;
    private static final float AZIMUTH_SMOOTHING_FACTOR = 0.35f;

    private MapViewModel viewModel;
    private MapView mapView;
    private Marker currentMarker;
    private Marker destinationMarker;
    private Polyline routeLine;
    private boolean followLocation = true;
    private boolean allowAutoFit = true;
    private boolean autoFitDone = false;
    private GeoPoint lastRouteDestination;
    private List<GeoPoint> lastRoutePoints = Collections.emptyList();
    private Float lastMarkerBearing;
    private Float lastMapBearing;
    private TextView routeDebug;
    private SensorManager sensorManager;
    private Sensor rotationSensor;
    private Sensor accelSensor;
    private Sensor magnetSensor;
    private float[] lastAccel;
    private float[] lastMagnet;
    private Float deviceAzimuth;
    private final SensorEventListener orientationListener = new SensorEventListener() {
        @Override
        public void onSensorChanged(SensorEvent event) {
            if (event.sensor == null) {
                return;
            }
            int sensorType = event.sensor.getType();
            if (sensorType == Sensor.TYPE_ROTATION_VECTOR) {
                updateFromRotationVector(event.values);
                return;
            }
            if (sensorType == Sensor.TYPE_ACCELEROMETER) {
                lastAccel = event.values.clone();
            } else if (sensorType == Sensor.TYPE_MAGNETIC_FIELD) {
                lastMagnet = event.values.clone();
            }
            if (lastAccel != null && lastMagnet != null) {
                updateFromAccelMag(lastAccel, lastMagnet);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
            // No-op.
        }
    };

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
                allowAutoFit = false;
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                followLocation = false;
                allowAutoFit = false;
                return false;
            }
        });

        viewModel.getCurrentLocation().observe(getViewLifecycleOwner(), this::updateLocationOnMap);
        viewModel.getRoutePoints().observe(getViewLifecycleOwner(), this::updateRouteOnMap);

        sensorManager = (SensorManager) requireContext().getSystemService(Context.SENSOR_SERVICE);
        if (sensorManager != null) {
            rotationSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR);
            if (rotationSensor == null) {
                accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
                magnetSensor = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mapView != null) {
            mapView.onResume();
        }
        if (sensorManager != null && rotationSensor != null) {
            sensorManager.registerListener(
                    orientationListener,
                    rotationSensor,
                    SensorManager.SENSOR_DELAY_UI);
        } else if (sensorManager != null && accelSensor != null && magnetSensor != null) {
            sensorManager.registerListener(
                    orientationListener,
                    accelSensor,
                    SensorManager.SENSOR_DELAY_UI);
            sensorManager.registerListener(
                    orientationListener,
                    magnetSensor,
                    SensorManager.SENSOR_DELAY_UI);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mapView != null) {
            mapView.onPause();
        }
        if (sensorManager != null) {
            sensorManager.unregisterListener(orientationListener);
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
            currentMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_CENTER);
            currentMarker.setIcon(ContextCompat.getDrawable(requireContext(), R.drawable.ic_direction_arrow));
            mapView.getOverlays().add(currentMarker);
        }

        currentMarker.setPosition(point);
        Float routeBearing = getRouteBearing(location);
        Float deviceHeading = deviceAzimuth;
        float markerTarget = deviceHeading != null
                ? normalizeBearing(deviceHeading
                - (mapView != null ? mapView.getMapOrientation() : 0f))
                : (location.hasBearing()
                        ? location.getBearing()
                        : (routeBearing != null ? routeBearing : 0f));
        markerTarget = normalizeBearing(markerTarget + DIRECTION_ICON_OFFSET_DEGREES);
        float markerBearing = deviceHeading != null
                ? markerTarget
                : smoothBearing(lastMarkerBearing, markerTarget);
        currentMarker.setRotation(markerBearing);
        lastMarkerBearing = markerBearing;
        if (followLocation && routeBearing != null) {
            float mapBearing = smoothBearing(lastMapBearing, routeBearing);
            mapView.setMapOrientation(mapBearing);
            lastMapBearing = mapBearing;
        }
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
            lastRoutePoints = Collections.emptyList();
            allowAutoFit = true;
            autoFitDone = false;
            lastRouteDestination = null;
            mapView.invalidate();
            return;
        }
        lastRoutePoints = new ArrayList<>(points);

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

        if (isNewRoute(points)) {
            allowAutoFit = true;
            autoFitDone = false;
        }
        if (allowAutoFit && !autoFitDone) {
            BoundingBox box = buildBoundingBox(points);
            if (box != null) {
                mapView.zoomToBoundingBox(box, true, 64);
                autoFitDone = true;
            }
        }
        lastRouteDestination = destination;

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

    @Nullable
    private Float getRouteBearing(@Nullable Location location) {
        if (lastRoutePoints == null || lastRoutePoints.size() < 2) {
            return null;
        }
        int startIndex = 0;
        if (location != null) {
            startIndex = findClosestPointIndex(location, lastRoutePoints);
            if (startIndex < 0) {
                startIndex = 0;
            }
            if (startIndex >= lastRoutePoints.size() - 1) {
                startIndex = lastRoutePoints.size() - 2;
            }
        }
        GeoPoint start = lastRoutePoints.get(startIndex);
        GeoPoint end = lastRoutePoints.get(startIndex + 1);
        float[] results = new float[2];
        Location.distanceBetween(
                start.getLatitude(),
                start.getLongitude(),
                end.getLatitude(),
                end.getLongitude(),
                results);
        return normalizeBearing(results[1]);
    }

    private int findClosestPointIndex(@NonNull Location location, @NonNull List<GeoPoint> points) {
        double bestDistance = Double.MAX_VALUE;
        int bestIndex = -1;
        float[] results = new float[1];
        double lat = location.getLatitude();
        double lon = location.getLongitude();
        for (int i = 0; i < points.size(); i++) {
            GeoPoint point = points.get(i);
            Location.distanceBetween(
                    lat,
                    lon,
                    point.getLatitude(),
                    point.getLongitude(),
                    results);
            float distance = results[0];
            if (distance < bestDistance) {
                bestDistance = distance;
                bestIndex = i;
            }
        }
        return bestIndex;
    }

    private float smoothBearing(@Nullable Float previous, float target) {
        float normalizedTarget = normalizeBearing(target);
        if (previous == null) {
            return normalizedTarget;
        }
        float normalizedPrevious = normalizeBearing(previous);
        float delta = ((normalizedTarget - normalizedPrevious + 540f) % 360f) - 180f;
        if (Math.abs(delta) < 1f) {
            return normalizedTarget;
        }
        return normalizeBearing(normalizedPrevious + (delta * 0.2f));
    }

    private float normalizeBearing(float bearing) {
        float normalized = bearing % 360f;
        if (normalized < 0f) {
            normalized += 360f;
        }
        return normalized;
    }

    private boolean updateDeviceAzimuth(float rawAzimuth) {
        float normalized = normalizeBearing(rawAzimuth);
        if (deviceAzimuth == null) {
            deviceAzimuth = normalized;
            return true;
        }
        float delta = ((normalized - deviceAzimuth + 540f) % 360f) - 180f;
        if (Math.abs(delta) < AZIMUTH_DEAD_BAND_DEGREES) {
            return false;
        }
        deviceAzimuth = normalizeBearing(deviceAzimuth + (delta * AZIMUTH_SMOOTHING_FACTOR));
        return true;
    }

    private boolean isNewRoute(@NonNull List<GeoPoint> points) {
        if (points.isEmpty()) {
            return false;
        }
        GeoPoint destination = points.get(points.size() - 1);
        if (lastRouteDestination == null) {
            return true;
        }
        double latDelta = Math.abs(destination.getLatitude() - lastRouteDestination.getLatitude());
        double lonDelta = Math.abs(destination.getLongitude() - lastRouteDestination.getLongitude());
        return latDelta > 1e-5 || lonDelta > 1e-5;
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
            allowAutoFit = true;
            autoFitDone = false;
            lastRouteDestination = null;
            lastRoutePoints = Collections.emptyList();
            lastMarkerBearing = null;
            lastMapBearing = null;
            deviceAzimuth = null;
            sensorManager = null;
            rotationSensor = null;
            accelSensor = null;
            magnetSensor = null;
            lastAccel = null;
            lastMagnet = null;
        }
        routeDebug = null;
    }

    private int getDisplayRotation() {
        WindowManager windowManager =
                (WindowManager) requireContext().getSystemService(Context.WINDOW_SERVICE);
        if (windowManager == null) {
            return Surface.ROTATION_0;
        }
        return windowManager.getDefaultDisplay().getRotation();
    }

    private void updateFromRotationVector(float[] values) {
        float[] rotationMatrix = new float[9];
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values);
        updateFromRotationMatrix(rotationMatrix);
    }

    private void updateFromAccelMag(float[] accel, float[] magnet) {
        float[] rotationMatrix = new float[9];
        boolean success = SensorManager.getRotationMatrix(rotationMatrix, null, accel, magnet);
        if (!success) {
            return;
        }
        updateFromRotationMatrix(rotationMatrix);
    }

    private void updateFromRotationMatrix(float[] rotationMatrix) {
        float[] adjustedMatrix = new float[9];
        int rotation = getDisplayRotation();
        switch (rotation) {
            case Surface.ROTATION_90:
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_Y,
                        SensorManager.AXIS_MINUS_X,
                        adjustedMatrix);
                break;
            case Surface.ROTATION_180:
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_X,
                        SensorManager.AXIS_MINUS_Y,
                        adjustedMatrix);
                break;
            case Surface.ROTATION_270:
                SensorManager.remapCoordinateSystem(
                        rotationMatrix,
                        SensorManager.AXIS_MINUS_Y,
                        SensorManager.AXIS_X,
                        adjustedMatrix);
                break;
            case Surface.ROTATION_0:
            default:
                System.arraycopy(rotationMatrix, 0, adjustedMatrix, 0, rotationMatrix.length);
                break;
        }
        float[] orientations = new float[3];
        SensorManager.getOrientation(adjustedMatrix, orientations);
        float azimuth = (float) Math.toDegrees(orientations[0]);
        if (!updateDeviceAzimuth(azimuth)) {
            return;
        }
        if (currentMarker != null && deviceAzimuth != null) {
            float markerBearing = normalizeBearing(deviceAzimuth
                    - (mapView != null ? mapView.getMapOrientation() : 0f));
            currentMarker.setRotation(markerBearing);
            lastMarkerBearing = markerBearing;
            if (mapView != null) {
                mapView.invalidate();
            }
        }
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
