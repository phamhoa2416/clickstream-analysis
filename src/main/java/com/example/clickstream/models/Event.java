package com.example.clickstream.models;

import lombok.Data;

import jakarta.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
public class Event implements Serializable {
    // === Core Identifiers ===
    @NotNull private String eventId;
    @NotNull private String eventName;
    @NotNull private Instant eventTimestamp;

    // === User Context ===
    private String userId;
    private String anonymousId;

    // === Session Context ===
    private String sessionId;
    private Instant sessionStartTime;
    private Integer sessionSequence;

    // === Application Context ===
    private String appId;
    private String schemaVersion;

    // === Device Context ===
    private String platform;
    private String deviceType;
    private String deviceBrand;
    private String osName;
    private String browserName;
    private String browserVersion;
    private String screenResolution;
    private String userAgent;
    private String language;

    // === Page Context ===
    private String pageUrl;
    private String pagePath;
    private String pageReferrer;

    // === Marketing Context (UTM Parameters) ===
    private String utmSource;
    private String utmMedium;
    private String utmCampaign;

    // === Performance Metrics ===
    private Float pageLoadTime;
    private Float timeToInteractive;

    // === E-commerce Context ===
    private String productId;
    private String productCategory;
    private String orderId;
    private Double revenue;
    private String currency;

    // === Custom Event Parameters ===
    private Map<String, String> eventParams;
}