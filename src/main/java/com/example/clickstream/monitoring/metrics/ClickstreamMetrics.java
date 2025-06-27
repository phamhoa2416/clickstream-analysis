package com.example.clickstream.monitoring.metrics;

import io.micrometer.core.instrument.*;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class ClickstreamMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicLong lagGauge;

    private static final String EVENTS_PROCESSED = "events_processed";
    private static final String PROCESSING_TIME = "processing_time";
    private static final String EVENT_LAG = "event_lag";
    private static final String BATCH_SIZE = "batch_size";
    private static final String ERRORS = "errors";

    private final Map<String, Counter> eventCounters = new ConcurrentHashMap<>();

    public ClickstreamMetrics(
            MeterRegistry meterRegistry
    ) {
        this.meterRegistry = meterRegistry;
        this.lagGauge = new AtomicLong(0);

        Gauge.builder(EVENT_LAG, lagGauge, AtomicLong::get)
                .description("Event processing lag in milliseconds")
                .register(meterRegistry);
    }

    public void recordProcessedEvent(String eventType) {
        eventCounters.computeIfAbsent(eventType, type ->
             Counter.builder(EVENTS_PROCESSED)
                     .tag("event_type", type)
                     .description("Total events by type")
                    .register(this.meterRegistry)
        ).increment();
    }

    public void recordProcessingTime(long milliseconds) {
        Timer.builder(PROCESSING_TIME)
                .description("Processing time in milliseconds")
                .register(meterRegistry)
                .record(milliseconds, TimeUnit.MILLISECONDS);
    }

    public void recordBatchMetrics(Dataset<Row> batch) {
        DistributionSummary.builder(BATCH_SIZE)
                .description("Events per batch")
                .register(meterRegistry)
                .record(batch.count());
    }

    public void recordError(String errorType) {
        Counter.builder(ERRORS)
                .tag("error_type", errorType)
                .description("Total errors by type")
                .register(meterRegistry)
                .increment();
    }

    public void updateLag(long lagMilliseconds) {
        lagGauge.set(lagMilliseconds);
    }
}
