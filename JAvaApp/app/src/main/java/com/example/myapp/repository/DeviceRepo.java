package com.example.myapp.repository;

import com.example.myapp.database.FirebaseManager;
import com.example.myapp.model.DeviceModel;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentSnapshot;

public class DeviceRepo {
    private final FirebaseManager firebaseManager;

    public DeviceRepo() {
        firebaseManager = FirebaseManager.getInstance();
    }

    public Task<Void> saveDevice(DeviceModel device) {
        return firebaseManager.upsertDevice(device);
    }

    public Task<DeviceModel> loadDevice(String deviceId) {
        return firebaseManager.getDevicesRef()
                .document(deviceId)
                .get()
                .continueWith(task -> {
                    if (!task.isSuccessful()) {
                        throw task.getException();
                    }
                    DocumentSnapshot snapshot = task.getResult();
                    if (snapshot == null || !snapshot.exists()) {
                        return null;
                    }
                    DeviceModel device = snapshot.toObject(DeviceModel.class);
                    if (device != null && (device.getId() == null || device.getId().trim().isEmpty())) {
                        device.setId(snapshot.getId());
                    }
                    return device;
                });
    }
}
