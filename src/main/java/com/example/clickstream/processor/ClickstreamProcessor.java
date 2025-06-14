package com.example.clickstream.processor;

import com.example.clickstream.config.AppConfig;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;
import static org.apache.spark.sql.functions.to_timestamp;

public class ClickstreamProcessor {
    private final SparkSession sparkSession;
    private final AppConfig appConfig;

    public ClickstreamProcessor(
            SparkSession sparkSession,
            AppConfig appConfig) {
        this.sparkSession = sparkSession;
        this.appConfig = appConfig;
    }

    public void startProcessing() throws StreamingQueryException, TimeoutException {
        StructType schema = createSchema();

        Dataset<Row> kafkaDf = sparkSession.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", appConfig.getKafkaBootstrapServers())
                .option("subscribe", appConfig.getKafkaTopic())
                .option("startingOffsets", "earliest")
                .load();

        Dataset<Row> eventDf = kafkaDf
                .select(
                        from_json(col("value").cast("string"), schema).alias("event_data"),
                        col("timestamp").alias("kafka_timestamp")
                )
                .withColumn("event_id", col("event_data.eventId"))
                .withColumn("event_name", col("event_data.eventName"))
                .withColumn("event_time", date_format(to_timestamp(col("event_data.eventParams.event_time")), "yyyy-MM-dd HH:mm:ss").cast("string"))
                .withColumn("user_id", col("event_data.userId"))
                .withColumn("session_id", col("event_data.sessionId"))
//                .withColumn("app_id", col("event_data.appId"))
                .withColumn("platform", col("event_data.platform"))
                .withColumn("page_url", col("event_data.eventParams.page_url"))
                .drop("event_data", "kafka_timestamp");

        StreamingQuery query = eventDf.writeStream()
                .foreachBatch((batchDf, batchId) -> {
                    batchDf.write()
                            .format("jdbc")
                            .option("url", appConfig.getClickhouseUrl())
                            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
                            .option("dbtable", "events")
                            .option("user", appConfig.getClickhouseUser())
                            .option("password", appConfig.getClickhousePassword())
                            .mode("append")
                            .save();
                })
                .outputMode("append")
                .start();

        query.awaitTermination();
    }

    private StructType createSchema() {
        return new StructType()
                .add("eventId", DataTypes.StringType)
                .add("eventName", DataTypes.StringType)
                .add("eventTime", DataTypes.StringType)
                .add("userId", DataTypes.StringType)
                .add("sessionId", DataTypes.StringType)
                .add("appId", DataTypes.StringType)
                .add("platform", DataTypes.StringType)
                .add("eventParams", new StructType()
                        .add("event_id", DataTypes.StringType)
                        .add("event_name", DataTypes.StringType)
                        .add("event_time", DataTypes.StringType)
                        .add("user_id", DataTypes.StringType)
                        .add("element_text", DataTypes.StringType)
                        .add("track", DataTypes.StringType)
                        .add("app_id", DataTypes.StringType)
                        .add("platform", DataTypes.StringType)
                        .add("page_url", DataTypes.StringType)
                        .add("page_referrer", DataTypes.StringType)
                        .add("screen_resolution", DataTypes.StringType)
                        .add("viewport_size", DataTypes.StringType)
                        .add("element_type", DataTypes.StringType)
                        .add("element_class", DataTypes.StringType)
                        .add("language", DataTypes.StringType)
                        .add("user_agent", DataTypes.StringType)
                        .add("page_path", DataTypes.StringType)
                        .add("productId", DataTypes.StringType)
                        .add("sessionId", DataTypes.StringType)
                );
    }
}