package com.example.clickstream.models;

import lombok.Data;

import java.time.Instant;

@Data
public class User {
    // Core identifiers
    private String userId;
    private String appId;

    // Timestamps
    private Instant firstTouchTime;
    private Instant lastTouchTime;

    // User attribution
    private String firstTrafficSource;
    private String lastTrafficSource;
}
