package com.example.clickstream.config;

import org.apache.spark.SparkConf;
import org.apache.spark.sql.SparkSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SparkConfig {
    private final AppConfig appConfig;

    @Autowired
    public SparkConfig(AppConfig appConfig) {
        this.appConfig = appConfig;
    }

    @Bean
    public SparkConf sparkConf() {
        return new SparkConf()
                .setAppName(appConfig.getSpark().getAppName())
                .setMaster(appConfig.getSpark().getMaster())
                .set("spark.serializer", "org.apache.spark.serializer.KryoSerializer")
                .set("spark.sql.shuffle.partitions", "8")
                .set("spark.executor.memory", "2g")
                .set("spark.driver.memory", "2g")
                .set("spark.ui.enabled", "true");
    }

    @Bean
    public SparkSession sparkSession() {
        return SparkSession.builder()
                .config(sparkConf())
                .config("spark.jars.packages", "org.apache.spark:spark-sql-kafka-0-10_2.12:3.3.0")
                .getOrCreate();
    }
}
