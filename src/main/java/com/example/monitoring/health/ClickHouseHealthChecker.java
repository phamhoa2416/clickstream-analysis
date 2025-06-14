package com.example.monitoring.health;

import com.clickhouse.jdbc.ClickHouseConnection;
import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.HealthIndicator;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.SQLException;

@Component
public class ClickHouseHealthChecker implements HealthIndicator {
    private final DataSource clickHouseDataSource;

    public ClickHouseHealthChecker(DataSource clickHouseDataSource) {
        this.clickHouseDataSource = clickHouseDataSource;
    }

    @Override
    public Health health() {
        try (ClickHouseConnection connection = (ClickHouseConnection) clickHouseDataSource.getConnection()) {
            boolean connected = connection.isValid(5);

            if (!connected) {
                return Health.down().withDetail("reason", "Connection is not valid").build();
            }

            return Health.up()
                    .withDetail("version", connection.getServerVersion())
                    .build();
        } catch (SQLException e) {
            return Health.down().withDetail("reason", e.getMessage()).build();
        }
    }
}
