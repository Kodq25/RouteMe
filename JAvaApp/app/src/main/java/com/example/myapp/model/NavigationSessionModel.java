package com.example.myapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class NavigationSessionModel {
    @DocumentId
    private String id;
    private String anonId;
    private String deviceId;
    private String routeId;
    private String status;
    private Timestamp startedAt;
    private Timestamp endedAt;
    private Timestamp lastLocationAt;
    private Timestamp lastProgressAt;
    private Integer currentLegIndex;
    private Integer currentStepIndex;
    private Long etaSeconds;
    private Double remainingMeters;
    private Boolean offRoute;
    private Integer recalcCount;

    public NavigationSessionModel() {
    }

    public NavigationSessionModel(String id,
                                  String anonId,
                                  String deviceId,
                                  String routeId,
                                  String status,
                                  Timestamp startedAt,
                                  Timestamp endedAt,
                                  Timestamp lastLocationAt,
                                  Timestamp lastProgressAt,
                                  Integer currentLegIndex,
                                  Integer currentStepIndex,
                                  Long etaSeconds,
                                  Double remainingMeters,
                                  Boolean offRoute,
                                  Integer recalcCount) {
        this.id = id;
        this.anonId = anonId;
        this.deviceId = deviceId;
        this.routeId = routeId;
        this.status = status;
        this.startedAt = startedAt;
        this.endedAt = endedAt;
        this.lastLocationAt = lastLocationAt;
        this.lastProgressAt = lastProgressAt;
        this.currentLegIndex = currentLegIndex;
        this.currentStepIndex = currentStepIndex;
        this.etaSeconds = etaSeconds;
        this.remainingMeters = remainingMeters;
        this.offRoute = offRoute;
        this.recalcCount = recalcCount;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnonId() { return anonId; }
    public void setAnonId(String anonId) { this.anonId = anonId; }

    public String getDeviceId() { return deviceId; }
    public void setDeviceId(String deviceId) { this.deviceId = deviceId; }

    public String getRouteId() { return routeId; }
    public void setRouteId(String routeId) { this.routeId = routeId; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Timestamp getStartedAt() { return startedAt; }
    public void setStartedAt(Timestamp startedAt) { this.startedAt = startedAt; }

    public Timestamp getEndedAt() { return endedAt; }
    public void setEndedAt(Timestamp endedAt) { this.endedAt = endedAt; }

    public Timestamp getLastLocationAt() { return lastLocationAt; }
    public void setLastLocationAt(Timestamp lastLocationAt) { this.lastLocationAt = lastLocationAt; }

    public Timestamp getLastProgressAt() { return lastProgressAt; }
    public void setLastProgressAt(Timestamp lastProgressAt) { this.lastProgressAt = lastProgressAt; }

    public Integer getCurrentLegIndex() { return currentLegIndex; }
    public void setCurrentLegIndex(Integer currentLegIndex) { this.currentLegIndex = currentLegIndex; }

    public Integer getCurrentStepIndex() { return currentStepIndex; }
    public void setCurrentStepIndex(Integer currentStepIndex) { this.currentStepIndex = currentStepIndex; }

    public Long getEtaSeconds() { return etaSeconds; }
    public void setEtaSeconds(Long etaSeconds) { this.etaSeconds = etaSeconds; }

    public Double getRemainingMeters() { return remainingMeters; }
    public void setRemainingMeters(Double remainingMeters) { this.remainingMeters = remainingMeters; }

    public Boolean getOffRoute() { return offRoute; }
    public void setOffRoute(Boolean offRoute) { this.offRoute = offRoute; }

    public Integer getRecalcCount() { return recalcCount; }
    public void setRecalcCount(Integer recalcCount) { this.recalcCount = recalcCount; }
}
