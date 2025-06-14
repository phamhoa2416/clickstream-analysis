package com.example.clickstream.models;

import lombok.Data;

import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
public class Event implements Serializable {
    // Core identifiers
    private String eventId;
    private String eventName;

    // Timestamps
    private Instant eventTimestamp;

    // User/device context
    private String userId;
    private String sessionId;

    // Platform context
    private String appId;
    private String platform;
    private String pageUrl;

    // Custom parameters
    private Map<String, String> eventParams;
}
