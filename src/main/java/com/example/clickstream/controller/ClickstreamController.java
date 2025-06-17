package com.example.clickstream.controller;

import com.example.clickstream.models.dto.EventPayload;
import com.example.clickstream.service.ClickstreamService;
import com.example.monitoring.metrics.ClickstreamMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.time.Instant;
import java.util.List;
import java.util.Map;

@RestController
@CrossOrigin(origins = "*")
public class ClickstreamController {
    private static final Logger log = LoggerFactory.getLogger(ClickstreamController.class);

    private final ClickstreamService service;
    private final ClickstreamMetrics metrics;

    public ClickstreamController(
            ClickstreamService service,
            ClickstreamMetrics metrics
    ) {
        this.service = service;
        this.metrics = metrics;
    }

    @PostMapping({"/events/", "/events"})
    public ResponseEntity<Map<String, String>> trackEvents(
            @Valid @RequestBody EventPayload payload,
            @RequestHeader(value="X-Request-ID", required = false) String requestId
    ) {
        long start = System.currentTimeMillis();
        log.info("Received events batch [Request ID: {}]", requestId);

        try {
            log.info("Received payload: {}", payload);
            List<Map<String, Object>> events = payload.getEvents();
            if (events == null || events.isEmpty()) {
                log.warn("Received empty events list [Request ID: {}]", requestId);
                metrics.recordError("Empty events payload received");
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Events list cannot be empty"));
            }

            log.info("Received {} events [Request ID: {}]", events.size(), requestId);
            events.forEach(event -> {
                String eventType = (String) event.getOrDefault("event_name", "unknown");
                metrics.recordProcessedEvent(eventType);
            });
            service.processEvents(events);
            metrics.recordProcessingTime(System.currentTimeMillis() - start);
            log.info("Successfully processed {} events [Request ID: {}]", events.size(), requestId);

            return ResponseEntity.ok(Map.of(
                    "status", "success",
                    "events_processed", String.valueOf(events.size())
            ));
        } catch (ClassCastException e) {
            log.error("Invalid payload format [Request ID: {}]", requestId, e);
            metrics.recordError("invalid_payload");
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "Invalid payload format: " + e.getMessage()));
        } catch (Exception e) {
            log.error("Error processing events [Request ID: {}]", requestId, e);
            metrics.recordError("processing_error_" + e.getClass().getSimpleName());
            return ResponseEntity.internalServerError()
                    .body(Map.of("error", "Server error: " + e.getMessage()));
        }
    }

    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> healthCheck() {
        return ResponseEntity.ok(Map.of(
                "status", "healthy",
                "timestamp", Instant.now().toString()
        ));
    }
}
