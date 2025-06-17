package com.example.clickstream.service;

import com.example.clickstream.models.Event;
import com.example.clickstream.producer.ClickstreamProducer;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.validation.Validator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClickstreamService {
    private final ClickstreamProducer producer;
    private final Validator validator;

    public ClickstreamService(
            ClickstreamProducer producer,
            ObjectMapper objectMapper,
            Validator validator
    )  {
        this.producer = producer;
        this.validator = validator;
    }

    public void processEvents(List<Map<String, Object>> rawEvents) {
        List<Event> events = rawEvents.stream()
                .map(this::convertToEvent)
                .filter(event -> {
                    var violations = validator.validate(event);
                    if (!violations.isEmpty()) {
                        log.error("Invalid event: {}", violations);
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        producer.sendEvents(events);
    }

    private Event convertToEvent(Map<String, Object> rawEvent) {
        try {
            Event event = new Event();

            // === Core Identifiers ===
            event.setEventId((String) rawEvent.get("event_id"));
            event.setEventName((String) rawEvent.get("event_name"));
            event.setEventTimestamp(Instant.parse((String) rawEvent.get("event_time")));

            // === User Context ===
            event.setUserId((String) rawEvent.get("user_id"));
            event.setAnonymousId((String) rawEvent.get("anonymous_id"));

            // === Session Context ===
            event.setSessionId((String) rawEvent.get("session_id"));
            event.setSessionStartTime(rawEvent.get("session_start_time") != null ?
                    Instant.parse((String) rawEvent.get("session_start_time")) : null);
            event.setSessionSequence(rawEvent.get("session_sequence") != null ?
                    Integer.parseInt(rawEvent.get("session_sequence").toString()) : null);

            // === Application Context ===
            event.setAppId((String) rawEvent.get("app_id"));
            event.setSchemaVersion((String) rawEvent.get("schema_version"));

            // === Device Context ===
            event.setPlatform((String) rawEvent.get("platform"));
            event.setDeviceType((String) rawEvent.get("device_type"));
            event.setDeviceBrand((String) rawEvent.get("device_brand"));
            event.setOsName((String) rawEvent.get("os_name"));
            event.setBrowserName((String) rawEvent.get("browser_name"));
            event.setBrowserVersion((String) rawEvent.get("browser_version"));
            event.setScreenResolution((String) rawEvent.get("screen_resolution"));
            event.setUserAgent((String) rawEvent.get("user_agent"));
            event.setLanguage((String) rawEvent.get("language"));

            // === Page Context ===
            event.setPageUrl((String) rawEvent.get("page_url"));
            event.setPagePath((String) rawEvent.get("page_path"));
            event.setPageReferrer((String) rawEvent.get("page_referrer"));

            // === Marketing Context (UTM Parameters) ===
            event.setUtmSource((String) rawEvent.get("utm_source"));
            event.setUtmMedium((String) rawEvent.get("utm_medium"));
            event.setUtmCampaign((String) rawEvent.get("utm_campaign"));

            // === Performance Metrics ===
            event.setPageLoadTime(rawEvent.get("page_load_time") != null ?
                    Float.parseFloat(rawEvent.get("page_load_time").toString()) : null);
            event.setTimeToInteractive(rawEvent.get("time_to_interactive") != null ?
                    Float.parseFloat(rawEvent.get("time_to_interactive").toString()) : null);

            // === E-commerce Context ===
            event.setProductId((String) rawEvent.get("product_id"));
            event.setProductCategory((String) rawEvent.get("product_category"));
            event.setOrderId((String) rawEvent.get("order_id"));
            event.setRevenue(rawEvent.get("revenue") != null ?
                    Double.parseDouble(rawEvent.get("revenue").toString()) : null);
            event.setCurrency((String) rawEvent.get("currency"));

            Map<String, String> eventParams = rawEvent.entrySet().stream()
                    .filter(entry -> entry.getValue() != null)
                    .collect(Collectors.toMap(
                            Map.Entry::getKey,
                            entry -> entry.getValue().toString()
                    ));
            event.setEventParams(eventParams);

            return event;
        } catch (Exception e) {
            log.error("Failed to convert event: {}", rawEvent, e);
            return null;
        }
    }
}
