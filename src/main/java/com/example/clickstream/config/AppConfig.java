package com.example.clickstream.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
@ConfigurationProperties(prefix = "app")
public class AppConfig {
    private final Kafka kafka = new Kafka();
    private final ClickHouse clickhouse = new ClickHouse();
    private final Spark spark = new Spark();

    @Getter
    @Setter
    public static class Kafka {
        private String bootstrapServers;
        private String topic;
        private String consumerGroupId;
        private int partitions = 2;
        private short replicationFactor = 1;
    }

    @Getter
    @Setter
    public static class ClickHouse {
        private String url;
        private String user;
        private String password;
        private String database;
    }

    @Getter
    @Setter
    public static class Spark {
        private String appName;
        private String master;
        private String checkpointLocation;
    }
}
