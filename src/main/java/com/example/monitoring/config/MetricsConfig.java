package com.example.monitoring.config;

import com.example.monitoring.metrics.ClickstreamMetrics;
import com.example.monitoring.metrics.ProducerMetrics;
import io.micrometer.core.aop.TimedAspect;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;

public class MetricsConfig {
    @Bean
    public TimedAspect timedAspect(MeterRegistry meterRegistry) {
        return new TimedAspect(meterRegistry);
    }

    @Bean
    public ProducerMetrics producerMetrics(MeterRegistry meterRegistry) {
        return new ProducerMetrics(meterRegistry);
    }

    @Bean
    public ClickstreamMetrics clickstreamMetrics(MeterRegistry meterRegistry) {
        return new ClickstreamMetrics(meterRegistry);
    }
}
