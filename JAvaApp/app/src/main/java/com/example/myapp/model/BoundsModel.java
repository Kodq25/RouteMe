package com.example.myapp.model;

import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class BoundsModel {
    private Double swLat;
    private Double swLng;
    private Double neLat;
    private Double neLng;

    public BoundsModel() {
    }

    public BoundsModel(Double swLat, Double swLng, Double neLat, Double neLng) {
        this.swLat = swLat;
        this.swLng = swLng;
        this.neLat = neLat;
        this.neLng = neLng;
    }

    public Double getSwLat() { return swLat; }
    public void setSwLat(Double swLat) { this.swLat = swLat; }

    public Double getSwLng() { return swLng; }
    public void setSwLng(Double swLng) { this.swLng = swLng; }

    public Double getNeLat() { return neLat; }
    public void setNeLat(Double neLat) { this.neLat = neLat; }

    public Double getNeLng() { return neLng; }
    public void setNeLng(Double neLng) { this.neLng = neLng; }
}
