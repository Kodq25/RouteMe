package com.example.myapp.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RouteManeuverModel {
    private String instruction;
    private Double distanceMeters;
    private Long durationSeconds;
    private Double latitude;
    private Double longitude;
    private Double bearing;
    private String type;

    public RouteManeuverModel() {
    }

    public RouteManeuverModel(String instruction,
                              Double distanceMeters,
                              Long durationSeconds,
                              Double latitude,
                              Double longitude,
                              Double bearing,
                              String type) {
        this.instruction = instruction;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.latitude = latitude;
        this.longitude = longitude;
        this.bearing = bearing;
        this.type = type;
    }

    public String getInstruction() { return instruction; }
    public void setInstruction(String instruction) { this.instruction = instruction; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public Double getBearing() { return bearing; }
    public void setBearing(Double bearing) { this.bearing = bearing; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
}
