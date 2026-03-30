package com.example.myapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class LocationUpdateModel {
    @DocumentId
    private String id;
    private Timestamp ts;
    private GeoPoint geo;
    private Double speedMps;
    private Double bearing;
    private Double accuracyM;

    public LocationUpdateModel() {
    }

    public LocationUpdateModel(String id,
                               Timestamp ts,
                               GeoPoint geo,
                               Double speedMps,
                               Double bearing,
                               Double accuracyM) {
        this.id = id;
        this.ts = ts;
        this.geo = geo;
        this.speedMps = speedMps;
        this.bearing = bearing;
        this.accuracyM = accuracyM;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Timestamp getTs() { return ts; }
    public void setTs(Timestamp ts) { this.ts = ts; }

    public GeoPoint getGeo() { return geo; }
    public void setGeo(GeoPoint geo) { this.geo = geo; }

    public Double getSpeedMps() { return speedMps; }
    public void setSpeedMps(Double speedMps) { this.speedMps = speedMps; }

    public Double getBearing() { return bearing; }
    public void setBearing(Double bearing) { this.bearing = bearing; }

    public Double getAccuracyM() { return accuracyM; }
    public void setAccuracyM(Double accuracyM) { this.accuracyM = accuracyM; }
}
