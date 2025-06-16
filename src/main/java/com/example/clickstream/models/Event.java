package com.example.clickstream.models;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.time.Instant;
import java.util.Map;

@Data
public class Event implements Serializable {
    // Core identifiers
    @NotNull
    private String eventId;
    @NotNull
    private String eventName;

    // Timestamps
    @NotNull
    private Instant eventTimestamp;

    // User/device context
    private String userId;
    private String sessionId;

    // Platform context
    private String platform;
    private String pageUrl;

    // Custom parameters
    private Map<String, String> eventParams;
}
