package com.example.clickstream.service;

import com.example.clickstream.exception.ProducerException;
import com.example.clickstream.models.Event;
import com.example.clickstream.producer.ClickstreamProducer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ClickstreamService {
    private final ClickstreamProducer producer;
    private final Validator validator;

    public ClickstreamService(
            ClickstreamProducer producer,
            Validator validator
    )  {
        this.producer = producer;
        this.validator = validator;
    }

    public void processEvents(List<Map<String, Object>> rawEvents) {
        if (rawEvents == null || rawEvents.isEmpty()) {
            log.warn("Receive empty or null events list");
            return;
        }

        log.info("Processing {} events", rawEvents.size());

        List<Event> events = rawEvents.stream()
                .map(this::convertToEvent)
                .filter(event -> {
                    if (event == null) {
                        log.warn("Skipping null event after conversion");
                        return false;
                    }

                    Set<ConstraintViolation<Event>> violations = validator.validate(event);
                    if (!violations.isEmpty()) {
                        log.error("Invalid event: {}: {}", event.getEventId(),
                                violations.stream()
                                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                                        .collect(Collectors.joining(", ")));
                        return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());

        if (events.isEmpty()) {
            log.warn("No valid events found");
            return;
        }

        log.info("Sending {} valid events to Kafka", events.size());
        try {
            producer.sendEvents(events);
            log.info("Successfully sent {} valid events to Kafka", events.size());
        } catch (ProducerException e) {
            log.error("Failed to send events to Kafka", e);
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error occurred while sending events to Kafka", e);
            throw new RuntimeException("Failed to process events to Kafka", e);
        }
    }

    private Event convertToEvent(Map<String, Object> rawEvent) {
        try {
            Event event = new Event();

            // === Core Identifiers ===
            event.setEventId(getStringValue(rawEvent, "event_id"));
            event.setEventName(getStringValue(rawEvent, "event_name"));
            event.setEventTimestamp(parseInstant(rawEvent, "event_time"));

            // === User Context ===
            event.setUserId(getStringValue(rawEvent, "user_id"));
            event.setAnonymousId(getStringValue(rawEvent, "anonymous_id"));

            // === Session Context ===
            event.setSessionId(getStringValue(rawEvent, "session_id"));
            event.setSessionStartTime(parseInstant(rawEvent, "session_start_time"));
            event.setSessionSequence(parseInteger(rawEvent, "session_sequence"));

            // === Application Context ===
            event.setAppId(getStringValue(rawEvent, "app_id"));
            event.setSchemaVersion(getStringValue(rawEvent, "schema_version"));

            // === Device Context ===
            event.setPlatform(getStringValue(rawEvent, "platform"));
            event.setDeviceType(getStringValue(rawEvent, "device_type"));
            event.setDeviceBrand(getStringValue(rawEvent, "device_brand"));
            event.setOsName(getStringValue(rawEvent, "os_name"));
            event.setBrowserName(getStringValue(rawEvent, "browser_name"));
            event.setBrowserVersion(getStringValue(rawEvent, "browser_version"));
            event.setScreenResolution(getStringValue(rawEvent, "screen_resolution"));
            event.setUserAgent(getStringValue(rawEvent, "user_agent"));
            event.setLanguage(getStringValue(rawEvent, "language"));

            // === Page Context ===
            event.setPageUrl(getStringValue(rawEvent, "page_url"));
            event.setPagePath(getStringValue(rawEvent, "page_path"));
            event.setPageReferrer(getStringValue(rawEvent, "page_referrer"));

            // === Marketing Context (UTM Parameters) ===
            event.setUtmSource(getStringValue(rawEvent, "utm_source"));
            event.setUtmMedium(getStringValue(rawEvent, "utm_medium"));
            event.setUtmCampaign(getStringValue(rawEvent, "utm_campaign"));

            // === Performance Metrics ===
            event.setPageLoadTime(parseFloat(rawEvent, "page_load_time"));
            event.setTimeToInteractive(parseFloat(rawEvent, "time_to_interactive"));

            // === E-commerce Context ===
            event.setProductId(getStringValue(rawEvent, "product_id"));
            event.setProductCategory(getStringValue(rawEvent, "product_category"));
            event.setOrderId(getStringValue(rawEvent, "order_id"));
            event.setRevenue(parseDouble(rawEvent, "revenue"));
            event.setCurrency(getStringValue(rawEvent, "currency"));

            // === Customer event parameters ===
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

    private String getStringValue(Map<String, Object> rawEvent, String key) {
        Object value = rawEvent.get(key);
        return value != null ? value.toString() : null;
    }

    private Instant parseInstant(Map<String, Object> rawEvent, String key) {
        Object value = rawEvent.get(key);
        if (value == null) return null;

        try {
            return Instant.parse(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse instant for key {}: {}", key, value);
            return null;
        }
    }

    private Integer parseInteger(Map<String, Object> rawEvent, String key) {
        Object value = rawEvent.get(key);
        if (value == null) return null;

        try {
            return Integer.parseInt(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse integer for key {}: {}", key, value);
            return null;
        }
    }

    private Float parseFloat(Map<String, Object> rawEvent, String key) {
        Object value = rawEvent.get(key);
        if (value == null) return null;

        try {
            return Float.parseFloat(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse float for key {}: {}", key, value);
            return null;
        }
    }

    private Double parseDouble(Map<String, Object> rawEvent, String key) {
        Object value = rawEvent.get(key);
        if (value == null) return null;

        try {
            return Double.parseDouble(value.toString());
        } catch (Exception e) {
            log.warn("Failed to parse double for key {}: {}", key, value);
            return null;
        }
    }
}
