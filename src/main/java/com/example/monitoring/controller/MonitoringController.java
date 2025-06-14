package com.example.monitoring.controller;

import com.example.monitoring.metrics.ClickstreamMetrics;
import com.example.monitoring.metrics.ProducerMetrics;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/monitoring")
public class MonitoringController {
    public MonitoringController(
            ClickstreamMetrics clickstreamMetrics,
            ProducerMetrics producerMetrics) {
    }

    @GetMapping("/metrics")
    public Map<String, String> getMetrics() {
        return Map.of(
                "status", "Metrics collection active",
                "clickstream", "Tracking event processing metrics",
                "producer", "Tracking Kafka producer metrics"
        );
    }
}
