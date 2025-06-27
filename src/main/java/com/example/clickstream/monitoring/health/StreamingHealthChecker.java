package com.example.clickstream.monitoring.health;

import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class StreamingHealthChecker implements HealthIndicator {
    private final SparkSession sparkSession;

    public StreamingHealthChecker(SparkSession sparkSession) {
        this.sparkSession = sparkSession;
    }

    @Override
    public Health health() {
        try {
            StreamingQuery[] queries = sparkSession.streams().active();
            if (queries.length == 0) {
                return Health.down().withDetail("reason", "No active streaming queries").build();
            }

            Health.Builder builder = Health.up();
            for (StreamingQuery query : queries) {
                builder.withDetail(query.id().toString(), query.status());
            }

            return builder.build();
        } catch (Exception e) {
            return Health.down().withDetail("reason", e.getMessage()).build();
        }
    }
}
