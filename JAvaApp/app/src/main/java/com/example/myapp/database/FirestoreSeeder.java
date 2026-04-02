package com.example.myapp.database;

import com.example.myapp.model.BoundsModel;
import com.example.myapp.model.DeviceModel;
import com.example.myapp.model.LocationUpdateModel;
import com.example.myapp.model.NavigationSessionModel;
import com.example.myapp.model.PlaceModel;
import com.example.myapp.model.RouteLegModel;
import com.example.myapp.model.RouteManeuverModel;
import com.example.myapp.model.RouteModel;
import com.example.myapp.model.RouteStopModel;
import com.example.myapp.BuildConfig;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class FirestoreSeeder {
    private static final String SAMPLE_ANON_ID = "anon_123";

    private FirestoreSeeder() {
    }

    public static Task<Void> seedSampleData() {
        if (!BuildConfig.DEBUG) {
            return Tasks.forResult(null);
        }
        FirebaseManager manager = FirebaseManager.getInstance();
        Task<QuerySnapshot> devicesTask = manager.getDevicesRef().limit(1).get();
        Task<QuerySnapshot> placesTask = manager.getPlacesRef().limit(1).get();

        return Tasks.whenAllComplete(devicesTask, placesTask)
                .continueWithTask(task -> {
                    Exception failure = null;
                    if (!devicesTask.isSuccessful()) {
                        failure = devicesTask.getException();
                    } else if (!placesTask.isSuccessful()) {
                        failure = placesTask.getException();
                    }
                    if (failure != null) {
                        return Tasks.forException(
                                failure != null ? failure : new IllegalStateException("Seed check failed"));
                    }

                    List<Task<Void>> seedTasks = new ArrayList<>();
                    if (devicesTask.getResult() == null || devicesTask.getResult().isEmpty()) {
                        seedTasks.add(createSampleDocuments(manager));
                    }
                    if (placesTask.getResult() == null || placesTask.getResult().isEmpty()) {
                        seedTasks.add(createPlaceDocuments(manager));
                    }
                    if (seedTasks.isEmpty()) {
                        return Tasks.forResult(null);
                    }
                    return Tasks.whenAll(seedTasks);
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

    private static Task<Void> createPlaceDocuments(FirebaseManager manager) {
        Timestamp now = Timestamp.now();
        List<PlaceModel> places = new ArrayList<>();

        places.add(buildPlace("Takoradi Harbour", 4.8845, -1.7554, "Port", "Major seaport", now));
        places.add(buildPlace("Sekondi Harbour", 4.9340, -1.7040, "Port", "Fishing harbour", now));
        places.add(buildPlace("Market Circle", 4.8977, -1.7550, "Market", "Main business district", now));
        places.add(buildPlace("Paa Grant Roundabout", 4.9015, -1.7558, "Landmark", "Major roundabout", now));
        places.add(buildPlace("Takoradi Airport", 4.8961, -1.7748, "Transport", "Domestic airport", now));
        places.add(buildPlace("Anaji Estate", 4.9050, -1.7300, "Residential", "Estate area", now));
        places.add(buildPlace("Effiakuma", 4.9105, -1.7402, "Residential", "Busy suburb", now));
        places.add(buildPlace("Tanokrom", 4.9058, -1.7705, "Town", "Commercial area", now));
        places.add(buildPlace("Kojokrom", 4.8935, -1.7690, "Town", "Railway town", now));
        places.add(buildPlace("Kwesimintsim", 4.9260, -1.7700, "Town", "Suburban town", now));

        places.add(buildPlace("Apremdo", 4.9200, -1.7800, "Town", "Residential", now));
        places.add(buildPlace("Essikado", 4.9050, -1.7100, "Town", "Industrial area", now));
        places.add(buildPlace("Effia", 4.9150, -1.7350, "Town", "Suburban area", now));
        places.add(buildPlace("Mpintsin", 4.9400, -1.7200, "Town", "Nearby town", now));
        places.add(buildPlace("Ntankoful", 4.9405, -1.7500, "Town", "Growing area", now));
        places.add(buildPlace("Kansaworodo", 4.9300, -1.7450, "Town", "Residential", now));
        places.add(buildPlace("Whindo", 4.9155, -1.7600, "Town", "Suburb", now));
        places.add(buildPlace("Apowa", 4.8600, -1.8000, "Town", "Junction area", now));
        places.add(buildPlace("Inchaban", 4.9500, -1.7300, "Town", "Mining area", now));
        places.add(buildPlace("Shama Junction", 5.0200, -1.6500, "Transport", "Major junction", now));

        places.add(buildPlace("Takoradi Technical University", 4.8963, -1.7515, "Education", "Technical university", now));
        places.add(buildPlace("Takoradi SHS", 4.9000, -1.7500, "Education", "Senior high school", now));
        places.add(buildPlace("St Mary's Boys SHS", 4.9100, -1.7600, "Education", "Boys school", now));
        places.add(buildPlace("Archbishop Porter Girls SHS", 4.9050, -1.7450, "Education", "Girls school", now));
        places.add(buildPlace("Fijai SHS", 4.9300, -1.7300, "Education", "Secondary school", now));

        places.add(buildPlace("Effia Nkwanta Hospital", 4.9012, -1.7578, "Hospital", "Regional hospital", now));
        places.add(buildPlace("Railway Hospital", 4.8950, -1.7550, "Hospital", "Public hospital", now));
        places.add(buildPlace("Essikado Hospital", 4.9055, -1.7105, "Hospital", "Local hospital", now));
        places.add(buildPlace("Trust Hospital", 4.8980, -1.7600, "Hospital", "Private hospital", now));

        places.add(buildPlace("Takoradi Mall", 4.9025, -1.7603, "Shopping", "Shopping mall", now));
        places.add(buildPlace("Anaji Market", 4.9055, -1.7320, "Market", "Local market", now));
        places.add(buildPlace("Effiakuma Market", 4.9100, -1.7400, "Market", "Busy market", now));
        places.add(buildPlace("Kwesimintsim Market", 4.9255, -1.7705, "Market", "Community market", now));

        places.add(buildPlace("Essipong Stadium", 4.9255, -1.7765, "Sports", "Football stadium", now));
        places.add(buildPlace("Sekondi Stadium", 4.9340, -1.7100, "Sports", "Sports stadium", now));
        places.add(buildPlace("Gyandu Park", 4.9320, -1.7150, "Sports", "Training park", now));

        places.add(buildPlace("Vienna City Beach", 4.8900, -1.7600, "Beach", "Popular beach", now));
        places.add(buildPlace("Aboadze Beach", 4.8000, -1.9000, "Beach", "Coastal beach", now));
        places.add(buildPlace("Busua Beach", 4.8060, -1.9400, "Beach", "Tourist beach", now));
        places.add(buildPlace("Akwidaa Beach", 4.7600, -2.0000, "Beach", "Quiet beach", now));

        places.add(buildPlace("Chapel Hill", 4.9350, -1.7050, "Landmark", "Historic hill", now));
        places.add(buildPlace("Fort Orange", 4.9345, -1.7055, "Historical", "Colonial fort", now));

        places.add(buildPlace("Oil and Gas Hub", 4.8700, -1.7800, "Industry", "Energy sector", now));
        places.add(buildPlace("Fishing Harbour", 4.8900, -1.7605, "Industry", "Fishing area", now));

        places.add(buildPlace("Apremdo Market", 4.9205, -1.7805, "Market", "Local trade", now));
        places.add(buildPlace("Kojokrom Market", 4.8938, -1.7695, "Market", "Community market", now));

        places.add(buildPlace("Sekondi Naval Base", 4.9355, -1.7020, "Military", "Naval base", now));

        places.add(buildPlace("Diabene", 4.8705, -1.7900, "Town", "Suburb", now));
        places.add(buildPlace("Eshiem", 4.9550, -1.7350, "Town", "Residential", now));

        List<Task<Void>> tasks = new ArrayList<>();
        for (PlaceModel place : places) {
            tasks.add(manager.upsertPlace(place));
        }
        return Tasks.whenAll(tasks);
    }

    private static PlaceModel buildPlace(String name,
                                         double latitude,
                                         double longitude,
                                         String type,
                                         String notes,
                                         Timestamp now) {
        PlaceModel place = new PlaceModel();
        place.setName(name);
        place.setLatitude(latitude);
        place.setLongitude(longitude);
        place.setType(type);
        place.setNotes(notes);
        place.setCreatedAt(now);
        place.setUpdatedAt(now);
        return place;
    }
}
