package com.example.myapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class PlaceModel {
    @DocumentId
    private String id;
    private String name;
    private Double latitude;
    private Double longitude;
    private String type;
    private String notes;
    private Timestamp createdAt;
    private Timestamp updatedAt;

    public PlaceModel() {
    }

    public PlaceModel(String id,
                      String name,
                      Double latitude,
                      Double longitude,
                      String type,
                      String notes,
                      Timestamp createdAt,
                      Timestamp updatedAt) {
        this.id = id;
        this.name = name;
        this.latitude = latitude;
        this.longitude = longitude;
        this.type = type;
        this.notes = notes;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Timestamp updatedAt) { this.updatedAt = updatedAt; }
}
