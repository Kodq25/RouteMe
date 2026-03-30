package com.example.myapp.database;

import com.example.myapp.model.DeviceModel;
import com.example.myapp.model.LocationUpdateModel;
import com.example.myapp.model.NavigationSessionModel;
import com.example.myapp.model.PlaceModel;
import com.example.myapp.model.RouteModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.Map;

public class FirebaseManager {
    private static FirebaseManager instance;
    private final FirebaseFirestore db;

    private static final String COLLECTION_DEVICES = "devices";
    private static final String COLLECTION_ROUTES = "routes";
    private static final String COLLECTION_NAV_SESSIONS = "navigationSessions";
    private static final String COLLECTION_PLACES = "places";
    private static final String SUBCOLLECTION_LOCATION_UPDATES = "locationUpdates";

    private final CollectionReference devicesRef;
    private final CollectionReference routesRef;
    private final CollectionReference navigationSessionsRef;
    private final CollectionReference placesRef;

    private FirebaseManager() {

        db = FirebaseFirestore.getInstance();

        devicesRef = db.collection(COLLECTION_DEVICES);
        routesRef = db.collection(COLLECTION_ROUTES);
        navigationSessionsRef = db.collection(COLLECTION_NAV_SESSIONS);
        placesRef = db.collection(COLLECTION_PLACES);
    }

    public static FirebaseManager getInstance() {
        if (instance == null) {
            instance = new FirebaseManager();
        }
        return instance;
    }

    public CollectionReference getDevicesRef() { return devicesRef; }

    public CollectionReference getRoutesRef() { return routesRef; }

    public CollectionReference getNavigationSessionsRef() { return navigationSessionsRef; }

    public CollectionReference getPlacesRef() { return placesRef; }

    public Task<Void> upsertDevice(DeviceModel device) {
        DocumentReference doc = resolveDoc(devicesRef, device.getId(), device::setId);
        return doc.set(device, SetOptions.merge());
    }

    public Task<Void> upsertRoute(RouteModel route) {
        DocumentReference doc = resolveDoc(routesRef, route.getId(), route::setId);
        return doc.set(route, SetOptions.merge());
    }

    public Task<Void> upsertNavigationSession(NavigationSessionModel session) {
        DocumentReference doc = resolveDoc(navigationSessionsRef, session.getId(), session::setId);
        return doc.set(session, SetOptions.merge());
    }

    public Task<Void> updateNavigationSession(String sessionId, Map<String, Object> fields) {
        DocumentReference doc = navigationSessionsRef.document(sessionId);
        return doc.set(fields, SetOptions.merge());
    }

    public Task<Void> upsertPlace(PlaceModel place) {
        DocumentReference doc = resolveDoc(placesRef, place.getId(), place::setId);
        return doc.set(place, SetOptions.merge());
    }

    public Task<Void> upsertLocationUpdate(String sessionId, LocationUpdateModel update) {
        CollectionReference updatesRef = navigationSessionsRef.document(sessionId)
                .collection(SUBCOLLECTION_LOCATION_UPDATES);
        DocumentReference doc = resolveDoc(updatesRef, update.getId(), update::setId);
        return doc.set(update, SetOptions.merge());
    }

    private DocumentReference resolveDoc(CollectionReference ref, String currentId, IdSetter setter) {
        if (currentId == null || currentId.trim().isEmpty()) {
            DocumentReference doc = ref.document();
            setter.setId(doc.getId());
            return doc;
        }
        return ref.document(currentId);
    }

    private interface IdSetter {
        void setId(String id);
    }
}
