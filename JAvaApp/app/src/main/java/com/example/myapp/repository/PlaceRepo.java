package com.example.myapp.repository;

import androidx.annotation.NonNull;

import com.example.myapp.database.FirebaseManager;
import com.example.myapp.model.PlaceModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.ListenerRegistration;

import java.util.Collections;
import java.util.List;

public class PlaceRepo {
    private final FirebaseManager firebaseManager;

    public PlaceRepo() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public Task<Void> savePlace(@NonNull PlaceModel place) {
        return firebaseManager.upsertPlace(place);
    }

    public ListenerRegistration listenToPlaces(@NonNull PlacesListener listener) {
        return firebaseManager.getPlacesRef()
                .orderBy("updatedAt")
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        listener.onError(error);
                        return;
                    }
                    if (snapshot == null) {
                        listener.onChanged(Collections.emptyList());
                        return;
                    }
                    List<PlaceModel> places = snapshot.toObjects(PlaceModel.class);
                    listener.onChanged(places);
                });
    }

    public interface PlacesListener {
        void onChanged(@NonNull List<PlaceModel> places);
        void onError(@NonNull Exception error);
    }
}
