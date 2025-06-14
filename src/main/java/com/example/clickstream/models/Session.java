package com.example.clickstream.models;

import lombok.Data;
import java.time.Instant;

@Data
public class Session {
    // Core identifiers
    private String sessionId;
    private String userId;

    // Timestamps
    private Instant sessionStart;
    private Instant sessionEnd;

    // User/device context
    private String appId;
    private String platform;

    // Device
    private String deviceCategory;
}
