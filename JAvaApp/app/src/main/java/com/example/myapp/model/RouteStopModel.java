package com.example.myapp.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class RouteStopModel {
    private Double latitude;
    private Double longitude;
    private String label;

    public RouteStopModel() {
    }

    public RouteStopModel(Double latitude, Double longitude, String label) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.label = label;
    }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
}
