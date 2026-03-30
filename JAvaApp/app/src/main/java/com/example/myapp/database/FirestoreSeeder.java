package com.example.myapp.database;

import com.example.myapp.model.BoundsModel;
import com.example.myapp.model.DeviceModel;
import com.example.myapp.model.LocationUpdateModel;
import com.example.myapp.model.NavigationSessionModel;
import com.example.myapp.model.RouteLegModel;
import com.example.myapp.model.RouteManeuverModel;
import com.example.myapp.model.RouteModel;
import com.example.myapp.model.RouteStopModel;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.GeoPoint;

import java.util.Collections;
import java.util.concurrent.TimeUnit;

public final class FirestoreSeeder {
    private static final String SAMPLE_ANON_ID = "anon_123";

    private FirestoreSeeder() {
    }

    public static Task<Void> seedSampleData() {
        FirebaseManager manager = FirebaseManager.getInstance();
        return manager.getDevicesRef()
                .limit(1)
                .get()
                .continueWithTask(task -> {
                    if (!task.isSuccessful()) {
                        Exception failure = task.getException();
                        return Tasks.forException(
                                failure != null ? failure : new IllegalStateException("Seed check failed"));
                    }
                    if (task.getResult() != null && !task.getResult().isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    return createSampleDocuments(manager);
                });
    }

    private static Task<Void> createSampleDocuments(FirebaseManager manager) {
        String deviceId = manager.getDevicesRef().document().getId();
        String routeId = manager.getRoutesRef().document().getId();
        String sessionId = manager.getNavigationSessionsRef().document().getId();

        Timestamp now = Timestamp.now();
        long expiresSeconds = now.getSeconds() + TimeUnit.DAYS.toSeconds(1);
        Timestamp expiresAt = new Timestamp(expiresSeconds, 0);

        DeviceModel device = new DeviceModel();
        device.setId(deviceId);
        device.setAnonId(SAMPLE_ANON_ID);
        device.setPlatform("android");
        device.setAppVersion("1.0");
        device.setCreatedAt(now);
        device.setLastSeenAt(now);
        device.setActiveSessionId(sessionId);

        RouteStopModel origin = new RouteStopModel(37.7749, -122.4194, "Start");
        RouteStopModel destination = new RouteStopModel(37.7849, -122.4094, "End");
        BoundsModel bounds = new BoundsModel(37.7700, -122.4250, 37.7900, -122.4050);

        RouteModel route = new RouteModel();
        route.setId(routeId);
        route.setAnonId(SAMPLE_ANON_ID);
        route.setOrigin(origin);
        route.setDestination(destination);
        route.setWaypoints(Collections.emptyList());
        route.setDistanceMeters(1200.5);
        route.setDurationSeconds(540L);
        route.setPolyline("abcd1234");
        route.setOverviewBounds(bounds);
        route.setLegs(Collections.singletonList(new RouteLegModel(1200.5, 540L)));
        route.setManeuvers(Collections.singletonList(
                new RouteManeuverModel("Head north", 200.0, 60L, 37.7750, -122.4190, 0.0, "depart")));
        route.setCreatedAt(now);
        route.setExpiresAt(expiresAt);

        NavigationSessionModel session = new NavigationSessionModel();
        session.setId(sessionId);
        session.setAnonId(SAMPLE_ANON_ID);
        session.setDeviceId(deviceId);
        session.setRouteId(routeId);
        session.setStatus("active");
        session.setStartedAt(now);
        session.setEndedAt(null);
        session.setLastLocationAt(now);
        session.setLastProgressAt(now);
        session.setCurrentLegIndex(0);
        session.setCurrentStepIndex(0);
        session.setEtaSeconds(540L);
        session.setRemainingMeters(1200.5);
        session.setOffRoute(false);
        session.setRecalcCount(0);

        CollectionReference updatesRef = manager.getNavigationSessionsRef()
                .document(sessionId)
                .collection("locationUpdates");
        String updateId = updatesRef.document().getId();

        LocationUpdateModel update = new LocationUpdateModel();
        update.setId(updateId);
        update.setTs(now);
        update.setGeo(new GeoPoint(37.7751, -122.4189));
        update.setSpeedMps(1.8);
        update.setBearing(5.0);
        update.setAccuracyM(6.0);

        Task<Void> deviceTask = manager.upsertDevice(device);
        Task<Void> routeTask = manager.upsertRoute(route);
        Task<Void> sessionTask = manager.upsertNavigationSession(session);
        Task<Void> updateTask = manager.upsertLocationUpdate(sessionId, update);

        return Tasks.whenAll(deviceTask, routeTask, sessionTask, updateTask);
    }
}
