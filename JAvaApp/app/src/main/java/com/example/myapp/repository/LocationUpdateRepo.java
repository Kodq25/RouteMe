package com.example.myapp.repository;

import com.example.myapp.database.FirebaseManager;
import com.example.myapp.model.LocationUpdateModel;
import com.google.android.gms.tasks.Task;

public class LocationUpdateRepo {
    private final FirebaseManager firebaseManager;

    public LocationUpdateRepo() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public Task<Void> addLocationUpdate(String sessionId, LocationUpdateModel update) {
        return firebaseManager.upsertLocationUpdate(sessionId, update);
    }
}
