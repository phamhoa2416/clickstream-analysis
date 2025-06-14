package com.example.clickstream.config;

import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

@Getter
@Configuration
public class AppConfig {
    // Kafka configuration
    @Value("${kafka.bootstrap.servers}")
    private String kafkaBootstrapServers;

    @Value("${kafka.topic}")
    private String kafkaTopic;

    @Value("${kafka.consumer.group.id}")
    private String kafkaConsumerGroupId;

    @Value("${kafka.partitions:2}")
    private int kafkaPartitions;

    @Value("${kafka.replication.factor:1}")
    private short kafkaReplicationFactor;

    // ClickHouse configuration
    @Value("${clickhouse.url}")
    private String clickhouseUrl;

    @Value("${clickhouse.user}")
    private String clickhouseUser;

    @Value("${clickhouse.password}")
    private String clickhousePassword;

    @Value("${clickhouse.database}")
    private String clickhouseDatabase;

    // Spark configuration
    @Value("${spark.app.name}")
    private String sparkAppName;

    @Value("${spark.master}")
    private String sparkMaster;

    @Value("${spark.checkpoint.location}")
    private String sparkCheckpointLocation;
}
