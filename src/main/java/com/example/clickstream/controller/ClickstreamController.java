package com.example.clickstream.controller;

import com.example.clickstream.service.ClickstreamService;
import com.example.monitoring.metrics.ClickstreamMetrics;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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
    public ResponseEntity<Void> trackEvents(
            @RequestBody Map<String, List<Map<String, Object>>> payload,
            @RequestHeader(value="X-Request-ID", required = false) String requestId
    ) {
        long start = System.currentTimeMillis();
        log.info("Received events batch [Request ID: {}]", requestId);

        try {
            log.info("Received payload: {}", payload);
            List<Map<String, Object>> events = payload.get("events");
            if (events == null || events.isEmpty()) {
                log.warn("Received empty events list");
                metrics.recordError("Empty events payload received");
                return ResponseEntity.badRequest().build();
            }

            log.info("Received {} events", events.size());
            service.processEvents(events);
            metrics.recordProcessedEvent("streaming");
            metrics.recordProcessingTime(System.currentTimeMillis() - start);
            log.info("Successfully processed {} events", events.size());

            return ResponseEntity.ok().build();
        } catch (ClassCastException e) {
            log.error("Invalid payload received", e);
            metrics.recordError("Invalid payload received");
            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("Error processing events", e);
            metrics.recordError("Error processing events: " + e.getMessage());
            return ResponseEntity.internalServerError().build();
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
