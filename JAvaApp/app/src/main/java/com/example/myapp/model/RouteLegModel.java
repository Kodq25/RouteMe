package com.example.myapp.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RouteLegModel {
    private Double distanceMeters;
    private Long durationSeconds;

    public RouteLegModel() {
    }

    public RouteLegModel(Double distanceMeters, Long durationSeconds) {
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
    }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }
}
