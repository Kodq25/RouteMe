package com.example.myapp.model;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.IgnoreExtraProperties;

@IgnoreExtraProperties
public class DeviceModel {
    @DocumentId
    private String id;
    private String anonId;
    private String platform;
    private String appVersion;
    private Timestamp createdAt;
    private Timestamp lastSeenAt;
    private String activeSessionId;

    public DeviceModel() {
    }

    public DeviceModel(String id,
                       String anonId,
                       String platform,
                       String appVersion,
                       Timestamp createdAt,
                       Timestamp lastSeenAt,
                       String activeSessionId) {
        this.id = id;
        this.anonId = anonId;
        this.platform = platform;
        this.appVersion = appVersion;
        this.createdAt = createdAt;
        this.lastSeenAt = lastSeenAt;
        this.activeSessionId = activeSessionId;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAnonId() { return anonId; }
    public void setAnonId(String anonId) { this.anonId = anonId; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getAppVersion() { return appVersion; }
    public void setAppVersion(String appVersion) { this.appVersion = appVersion; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getLastSeenAt() { return lastSeenAt; }
    public void setLastSeenAt(Timestamp lastSeenAt) { this.lastSeenAt = lastSeenAt; }

    public String getActiveSessionId() { return activeSessionId; }
    public void setActiveSessionId(String activeSessionId) { this.activeSessionId = activeSessionId; }
}
