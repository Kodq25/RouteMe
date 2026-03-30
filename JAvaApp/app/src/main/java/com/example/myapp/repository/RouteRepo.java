package com.example.myapp.repository;

import com.example.myapp.database.FirebaseManager;
import com.example.myapp.model.RouteModel;
import com.google.android.gms.tasks.Task;

public class RouteRepo {
    private final FirebaseManager firebaseManager;

    public RouteRepo() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public Task<Void> saveRoute(RouteModel route){
        return firebaseManager.upsertRoute(route);
    }
}
