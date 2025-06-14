package com.example.monitoring.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class ProducerMetrics {
    private final MeterRegistry meterRegistry;
    private final AtomicLong queueSizeGauge;

    private static final String MESSAGES_SENT = "messages_sent";
    private static final String MESSAGES_SIZE =  "messages_size";
    private static final String QUEUE_SIZE = "queue_size";
    private static final String ERRORS = "errors";

    @Autowired
    public ProducerMetrics(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.queueSizeGauge = new AtomicLong(0);

        Gauge.builder(QUEUE_SIZE, queueSizeGauge, AtomicLong::get)
                .description("Current producer queue size")
                .register(meterRegistry);
    }

    public void incrementSuccess() {
        Counter.builder(MESSAGES_SENT)
                .description("Total successfully sent messages")
                .register(meterRegistry)
                .increment();
    }

    public void incrementFailure() {
        Counter.builder(ERRORS)
                .description("Total failed sent messages")
                .register(meterRegistry)
                .increment();
    }

    public void recordMessageSize(int bytes) {
        DistributionSummary.builder(MESSAGES_SIZE)
                .baseUnit("bytes")
                .description("Current producer message size")
                .register(meterRegistry)
                .record(bytes);
    }

    public void updateQueueSize(long size) {
        queueSizeGauge.set(size);
    }
}
