package com.example.myapp.viewmodel;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Looper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.example.myapp.BuildConfig;
import com.example.myapp.model.DeviceModel;
import com.example.myapp.model.LocationUpdateModel;
import com.example.myapp.model.NavigationSessionModel;
import com.example.myapp.model.PlaceModel;
import com.example.myapp.model.RouteModel;
import com.example.myapp.model.RouteStopModel;
import com.example.myapp.model.BoundsModel;
import com.example.myapp.repository.DeviceRepo;
import com.example.myapp.repository.LocationUpdateRepo;
import com.example.myapp.repository.NavigationSessionRepo;
import com.example.myapp.repository.PlaceRepo;
import com.example.myapp.repository.RouteRepo;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.Task;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.ListenerRegistration;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.osmdroid.util.GeoPoint;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class MapViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "device_prefs";
    private static final String KEY_DEVICE_ID = "device_id";
    private static final String KEY_ANON_ID = "anon_id";
    private static final String PLATFORM_ANDROID = "android";
    private static final String DEFAULT_ORIGIN_LABEL = "Origin";
    private static final String DEFAULT_DESTINATION_LABEL = "Destination";

    private final Context appContext;
    private final FusedLocationProviderClient fusedLocationClient;
    private final MutableLiveData<Location> currentLocation = new MutableLiveData<>();
    private final MutableLiveData<List<GeoPoint>> routePoints = new MutableLiveData<>();
    private final MutableLiveData<List<PlaceModel>> sharedPlaces =
            new MutableLiveData<>(Collections.emptyList());
    private final MutableLiveData<DeviceModel> currentDevice = new MutableLiveData<>();
    private final LocationCallback locationCallback;
    private final ExecutorService routingExecutor = Executors.newSingleThreadExecutor();
    private final DeviceRepo deviceRepo;
    private final RouteRepo routeRepo;
    private final NavigationSessionRepo navigationSessionRepo;
    private final LocationUpdateRepo locationUpdateRepo;
    private final PlaceRepo placeRepo;
    private String activeSessionId;
    private ListenerRegistration sharedPlacesListener;
    private List<GeoPoint> fullRoutePoints = Collections.emptyList();

    public MapViewModel(@NonNull Application application) {
        super(application);
        this.appContext = application.getApplicationContext();
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(appContext);
        this.deviceRepo = new DeviceRepo();
        this.routeRepo = new RouteRepo();
        this.navigationSessionRepo = new NavigationSessionRepo();
        this.locationUpdateRepo = new LocationUpdateRepo();
        this.placeRepo = new PlaceRepo();
        this.locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {
                if (locationResult.getLastLocation() != null) {
                    Location latest = locationResult.getLastLocation();
                    currentLocation.postValue(latest);
                    persistLocationUpdateIfActive(latest);
                    updateRemainingRoutePoints(latest);
                }
            }
        };
    }

    public LiveData<Location> getCurrentLocation() {
        return currentLocation;
    }

    public LiveData<List<GeoPoint>> getRoutePoints() {
        return routePoints;
    }

    public LiveData<DeviceModel> getCurrentDevice() {
        return currentDevice;
    }

    public LiveData<List<PlaceModel>> getSharedPlaces() {
        return sharedPlaces;
    }

    public void initDevice() {
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        String deviceId = prefs.getString(KEY_DEVICE_ID, null);
        String anonId = prefs.getString(KEY_ANON_ID, null);

        if (deviceId == null || deviceId.trim().isEmpty()
                || anonId == null || anonId.trim().isEmpty()) {
            createNewDevice(prefs);
            return;
        }

        deviceRepo.loadDevice(deviceId)
                .addOnSuccessListener(device -> {
                    if (device == null) {
                        createNewDevice(prefs);
                        return;
                    }
                    if (device.getId() == null || device.getId().trim().isEmpty()) {
                        device.setId(deviceId);
                    }
                    if (device.getAnonId() == null || device.getAnonId().trim().isEmpty()) {
                        device.setAnonId(anonId);
                    }
                    updateLastSeen(device);
                    activeSessionId = device.getActiveSessionId();
                    currentDevice.postValue(device);
                })
                .addOnFailureListener(error -> {
                    createNewDevice(prefs);
                });
    }

    public void setActiveSessionId(String sessionId) {
        this.activeSessionId = sessionId;
    }

    public Task<Void> saveDevice(@NonNull DeviceModel device) {
        return deviceRepo.saveDevice(device);
    }

    public Task<Void> saveRoute(@NonNull RouteModel route) {
        return routeRepo.saveRoute(route);
    }

    public Task<Void> saveSession(@NonNull NavigationSessionModel session) {
        Task<Void> task = navigationSessionRepo.saveSession(session);
        if (session.getId() != null && !session.getId().trim().isEmpty()) {
            activeSessionId = session.getId();
        }
        return task;
    }

    public Task<Void> saveSharedPlace(@NonNull PlaceModel place) {
        Timestamp now = Timestamp.now();
        if (place.getCreatedAt() == null) {
            place.setCreatedAt(now);
        }
        place.setUpdatedAt(now);
        return placeRepo.savePlace(place);
    }

    public void startSharedPlacesListener() {
        if (sharedPlacesListener != null) {
            return;
        }
        sharedPlacesListener = placeRepo.listenToPlaces(new PlaceRepo.PlacesListener() {
            @Override
            public void onChanged(@NonNull List<PlaceModel> places) {
                sharedPlaces.postValue(places);
            }

            @Override
            public void onError(@NonNull Exception error) {
                sharedPlaces.postValue(Collections.emptyList());
            }
        });
    }

    public void stopSharedPlacesListener() {
        if (sharedPlacesListener != null) {
            sharedPlacesListener.remove();
            sharedPlacesListener = null;
        }
    }

    @SuppressLint("MissingPermission")
    public void startLocationUpdates() {
        if (!hasLocationPermission() || !isGpsEnabled()) {
            return;
        }

        LocationRequest request = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, 5000L)
                .setMinUpdateIntervalMillis(2000L)
                .build();

        fusedLocationClient.requestLocationUpdates(request, locationCallback, Looper.getMainLooper());
        fusedLocationClient.getLastLocation().addOnSuccessListener(location -> {
            if (location != null) {
                currentLocation.setValue(location);
                persistLocationUpdateIfActive(location);
                updateRemainingRoutePoints(location);
            }
        });
    }

    public void stopLocationUpdates() {
        fusedLocationClient.removeLocationUpdates(locationCallback);
        endActiveSession("inactive");
    }

    public void requestRoute(@NonNull GeoPoint origin, @NonNull GeoPoint destination) {
        routingExecutor.execute(() -> {
            try {
                RouteResult result = fetchRouteResult(origin, destination);
                fullRoutePoints = new ArrayList<>(result.points);
                updateRemainingRoutePoints(currentLocation.getValue());
                if (!result.points.isEmpty()) {
                    saveRouteAndSession(origin, destination, result);
                }
            } catch (IOException | JSONException ignored) {
                routePoints.postValue(new ArrayList<>());
            }
        });
    }

    public boolean isGpsEnabled() {
        LocationManager locationManager =
                (LocationManager) appContext.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            return false;
        }
        boolean gps = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean network = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        return gps || network;
    }

    private boolean hasLocationPermission() {
        return ContextCompat.checkSelfPermission(appContext, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void persistLocationUpdateIfActive(@NonNull Location location) {
        String sessionId = activeSessionId;
        if (sessionId == null || sessionId.trim().isEmpty()) {
            return;
        }
        LocationUpdateModel update = new LocationUpdateModel();
        update.setTs(Timestamp.now());
        update.setGeo(new com.google.firebase.firestore.GeoPoint(
                location.getLatitude(),
                location.getLongitude()));
        update.setSpeedMps(location.hasSpeed() ? (double) location.getSpeed() : null);
        update.setBearing(location.hasBearing() ? (double) location.getBearing() : null);
        update.setAccuracyM(location.hasAccuracy() ? (double) location.getAccuracy() : null);
        locationUpdateRepo.addLocationUpdate(sessionId, update);

        Map<String, Object> fields = new HashMap<>();
        fields.put("lastLocationAt", update.getTs());
        fields.put("lastProgressAt", update.getTs());
        navigationSessionRepo.updateSessionFields(sessionId, fields);
    }

    private RouteResult fetchRouteResult(@NonNull GeoPoint origin, @NonNull GeoPoint destination)
            throws IOException, JSONException {
        String url = String.format(
                Locale.US,
                "https://router.project-osrm.org/route/v1/driving/%f,%f;%f,%f?overview=full&geometries=geojson",
                origin.getLongitude(),
                origin.getLatitude(),
                destination.getLongitude(),
                destination.getLatitude()
        );

        HttpURLConnection connection = (HttpURLConnection) new URL(url).openConnection();
        connection.setConnectTimeout(10000);
        connection.setReadTimeout(10000);
        connection.setRequestMethod("GET");

        try (InputStream inputStream = connection.getInputStream()) {
            String json = readStream(inputStream);
            JSONObject root = new JSONObject(json);
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) {
                return new RouteResult(Collections.emptyList(), null, null, null);
            }
            JSONObject route = routes.getJSONObject(0);
            JSONObject geometry = route.getJSONObject("geometry");
            JSONArray coordinates = geometry.getJSONArray("coordinates");
            List<GeoPoint> points = new ArrayList<>(coordinates.length());
            for (int i = 0; i < coordinates.length(); i++) {
                JSONArray coord = coordinates.getJSONArray(i);
                double lon = coord.getDouble(0);
                double lat = coord.getDouble(1);
                points.add(new GeoPoint(lat, lon));
            }
            Double distanceMeters = route.has("distance") ? route.getDouble("distance") : null;
            Long durationSeconds = route.has("duration") ? Math.round(route.getDouble("duration")) : null;
            String geometryJson = geometry.toString();
            return new RouteResult(points, distanceMeters, durationSeconds, geometryJson);
        } finally {
            connection.disconnect();
        }
    }

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

    @Override
    protected void onCleared() {
        super.onCleared();
        stopLocationUpdates();
        stopSharedPlacesListener();
        routingExecutor.shutdownNow();
    }

    private void createNewDevice(@NonNull SharedPreferences prefs) {
        DeviceModel device = new DeviceModel();
        device.setAnonId("anon_" + UUID.randomUUID().toString().replace("-", ""));
        device.setPlatform(PLATFORM_ANDROID);
        device.setAppVersion(BuildConfig.VERSION_NAME);
        Timestamp now = Timestamp.now();
        device.setCreatedAt(now);
        device.setLastSeenAt(now);
        saveDevice(device);
        String deviceId = device.getId();
        if (deviceId != null && !deviceId.trim().isEmpty()) {
            prefs.edit()
                    .putString(KEY_DEVICE_ID, deviceId)
                    .putString(KEY_ANON_ID, device.getAnonId())
                    .apply();
        }
        currentDevice.postValue(device);
    }

    private void updateLastSeen(@NonNull DeviceModel device) {
        device.setLastSeenAt(Timestamp.now());
        device.setAppVersion(BuildConfig.VERSION_NAME);
        device.setPlatform(PLATFORM_ANDROID);
        saveDevice(device);
    }

    private void updateRemainingRoutePoints(@Nullable Location location) {
        List<GeoPoint> points = fullRoutePoints;
        if (points.isEmpty()) {
            routePoints.postValue(Collections.emptyList());
            return;
        }
        if (location == null) {
            routePoints.postValue(new ArrayList<>(points));
            return;
        }
        int closestIndex = findClosestPointIndex(location, points);
        if (closestIndex < 0 || closestIndex >= points.size()) {
            routePoints.postValue(new ArrayList<>(points));
            return;
        }
        routePoints.postValue(new ArrayList<>(points.subList(closestIndex, points.size())));
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

    private void saveRouteAndSession(@NonNull GeoPoint origin,
                                     @NonNull GeoPoint destination,
                                     @NonNull RouteResult result) {
        Timestamp now = Timestamp.now();
        RouteModel route = new RouteModel();
        route.setAnonId(getCurrentAnonId());
        route.setOrigin(new RouteStopModel(origin.getLatitude(), origin.getLongitude(), DEFAULT_ORIGIN_LABEL));
        route.setDestination(new RouteStopModel(
                destination.getLatitude(),
                destination.getLongitude(),
                DEFAULT_DESTINATION_LABEL));
        route.setWaypoints(Collections.emptyList());
        route.setDistanceMeters(result.distanceMeters);
        route.setDurationSeconds(result.durationSeconds);
        route.setPolyline(result.geometryJson);
        route.setOverviewBounds(buildBounds(result.points, origin, destination));
        route.setLegs(Collections.emptyList());
        route.setManeuvers(Collections.emptyList());
        route.setCreatedAt(now);
        route.setExpiresAt(new Timestamp(now.getSeconds() + TimeUnit.DAYS.toSeconds(1), 0));
        Task<Void> routeTask = saveRoute(route);

        DeviceModel device = currentDevice.getValue();
        if (device == null || device.getId() == null || device.getId().trim().isEmpty()) {
            return;
        }

        if (activeSessionId == null || activeSessionId.trim().isEmpty()) {
            NavigationSessionModel session = new NavigationSessionModel();
            session.setAnonId(device.getAnonId());
            session.setDeviceId(device.getId());
            session.setRouteId(route.getId());
            session.setStatus("active");
            session.setStartedAt(now);
            session.setLastLocationAt(now);
            session.setLastProgressAt(now);
            session.setEtaSeconds(result.durationSeconds);
            session.setRemainingMeters(result.distanceMeters);
            session.setCurrentLegIndex(0);
            session.setCurrentStepIndex(0);
            session.setOffRoute(false);
            session.setRecalcCount(0);
            Task<Void> sessionTask = saveSession(session);
            if (session.getId() != null && !session.getId().trim().isEmpty()) {
                device.setActiveSessionId(session.getId());
                activeSessionId = session.getId();
            }
            device.setLastSeenAt(now);
            Task<Void> deviceTask = saveDevice(device);
            currentDevice.postValue(device);
            com.google.android.gms.tasks.Tasks.whenAll(routeTask, sessionTask, deviceTask);
        } else {
            Map<String, Object> fields = new HashMap<>();
            fields.put("routeId", route.getId());
            fields.put("status", "active");
            fields.put("lastProgressAt", now);
            if (result.durationSeconds != null) {
                fields.put("etaSeconds", result.durationSeconds);
            }
            if (result.distanceMeters != null) {
                fields.put("remainingMeters", result.distanceMeters);
            }
            com.google.android.gms.tasks.Tasks.whenAll(
                    routeTask,
                    navigationSessionRepo.updateSessionFields(activeSessionId, fields));
        }
    }

    public Task<Void> endActiveSession(@NonNull String status) {
        if (activeSessionId == null || activeSessionId.trim().isEmpty()) {
            return com.google.android.gms.tasks.Tasks.forResult(null);
        }
        String sessionId = activeSessionId;
        activeSessionId = null;
        Timestamp now = Timestamp.now();
        Map<String, Object> fields = new HashMap<>();
        fields.put("status", status);
        fields.put("endedAt", now);
        fields.put("lastProgressAt", now);
        Task<Void> sessionTask = navigationSessionRepo.updateSessionFields(sessionId, fields);

        DeviceModel device = currentDevice.getValue();
        if (device != null && sessionId.equals(device.getActiveSessionId())) {
            device.setActiveSessionId(null);
            device.setLastSeenAt(now);
            Task<Void> deviceTask = saveDevice(device);
            currentDevice.postValue(device);
            return com.google.android.gms.tasks.Tasks.whenAll(sessionTask, deviceTask);
        }
        return sessionTask;
    }

    private BoundsModel buildBounds(@NonNull List<GeoPoint> points,
                                    @NonNull GeoPoint origin,
                                    @NonNull GeoPoint destination) {
        double minLat = Math.min(origin.getLatitude(), destination.getLatitude());
        double maxLat = Math.max(origin.getLatitude(), destination.getLatitude());
        double minLon = Math.min(origin.getLongitude(), destination.getLongitude());
        double maxLon = Math.max(origin.getLongitude(), destination.getLongitude());
        for (GeoPoint point : points) {
            minLat = Math.min(minLat, point.getLatitude());
            maxLat = Math.max(maxLat, point.getLatitude());
            minLon = Math.min(minLon, point.getLongitude());
            maxLon = Math.max(maxLon, point.getLongitude());
        }
        return new BoundsModel(minLat, minLon, maxLat, maxLon);
    }

    private String getCurrentAnonId() {
        DeviceModel device = currentDevice.getValue();
        if (device != null && device.getAnonId() != null && !device.getAnonId().trim().isEmpty()) {
            return device.getAnonId();
        }
        SharedPreferences prefs = appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_ANON_ID, null);
    }

    private static final class RouteResult {
        private final List<GeoPoint> points;
        private final Double distanceMeters;
        private final Long durationSeconds;
        private final String geometryJson;

        private RouteResult(@NonNull List<GeoPoint> points,
                            Double distanceMeters,
                            Long durationSeconds,
                            String geometryJson) {
            this.points = points;
            this.distanceMeters = distanceMeters;
            this.durationSeconds = durationSeconds;
            this.geometryJson = geometryJson;
        }
    }
}
