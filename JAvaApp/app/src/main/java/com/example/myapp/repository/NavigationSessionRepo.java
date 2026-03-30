package com.example.myapp.repository;

import com.example.myapp.database.FirebaseManager;
import com.example.myapp.model.NavigationSessionModel;
import com.google.android.gms.tasks.Task;

import java.util.Map;

public class NavigationSessionRepo {
    private final FirebaseManager firebaseManager;

    public NavigationSessionRepo() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public Task<Void> saveSession(NavigationSessionModel session) {
        return firebaseManager.upsertNavigationSession(session);
    }

    public Task<Void> updateSessionFields(String sessionId, Map<String, Object> fields) {
        return firebaseManager.updateNavigationSession(sessionId, fields);
    }
}
