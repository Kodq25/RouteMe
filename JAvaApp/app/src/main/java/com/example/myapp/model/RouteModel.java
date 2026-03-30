package com.example.myapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

import java.util.List;

@IgnoreExtraProperties
public class RouteModel {
    @DocumentId
    private String id;
    private String anonId;
    private RouteStopModel origin;
    private RouteStopModel destination;
    private List<RouteStopModel> waypoints;
    private Double distanceMeters;
    private Long durationSeconds;
    private String polyline;
    private BoundsModel overviewBounds;
    private List<RouteLegModel> legs;
    private List<RouteManeuverModel> maneuvers;
    private Timestamp createdAt;
    private Timestamp expiresAt;

    public RouteModel() {
    }

    public RouteModel(String id,
                      String anonId,
                      RouteStopModel origin,
                      RouteStopModel destination,
                      List<RouteStopModel> waypoints,
                      Double distanceMeters,
                      Long durationSeconds,
                      String polyline,
                      BoundsModel overviewBounds,
                      List<RouteLegModel> legs,
                      List<RouteManeuverModel> maneuvers,
                      Timestamp createdAt,
                      Timestamp expiresAt) {
        this.id = id;
        this.anonId = anonId;
        this.origin = origin;
        this.destination = destination;
        this.waypoints = waypoints;
        this.distanceMeters = distanceMeters;
        this.durationSeconds = durationSeconds;
        this.polyline = polyline;
        this.overviewBounds = overviewBounds;
        this.legs = legs;
        this.maneuvers = maneuvers;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnonId() { return anonId; }
    public void setAnonId(String anonId) { this.anonId = anonId; }

    public RouteStopModel getOrigin() { return origin; }
    public void setOrigin(RouteStopModel origin) { this.origin = origin; }

    public RouteStopModel getDestination() { return destination; }
    public void setDestination(RouteStopModel destination) { this.destination = destination; }

    public List<RouteStopModel> getWaypoints() { return waypoints; }
    public void setWaypoints(List<RouteStopModel> waypoints) { this.waypoints = waypoints; }

    public Double getDistanceMeters() { return distanceMeters; }
    public void setDistanceMeters(Double distanceMeters) { this.distanceMeters = distanceMeters; }

    public Long getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(Long durationSeconds) { this.durationSeconds = durationSeconds; }

    public String getPolyline() { return polyline; }
    public void setPolyline(String polyline) { this.polyline = polyline; }

    public BoundsModel getOverviewBounds() { return overviewBounds; }
    public void setOverviewBounds(BoundsModel overviewBounds) { this.overviewBounds = overviewBounds; }

    public List<RouteLegModel> getLegs() { return legs; }
    public void setLegs(List<RouteLegModel> legs) { this.legs = legs; }

    public List<RouteManeuverModel> getManeuvers() { return maneuvers; }
    public void setManeuvers(List<RouteManeuverModel> maneuvers) { this.maneuvers = maneuvers; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Timestamp expiresAt) { this.expiresAt = expiresAt; }
}
