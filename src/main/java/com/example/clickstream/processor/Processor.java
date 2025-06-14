package com.example.clickstream.processor;

import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class Processor {
    private static final String KAFKA_BOOTSTRAP_SERVERS = "localhost:9092";
    private static final String CLICKHOUSE_URL = "jdbc:clickhouse://localhost:8123/clickstream_1k";

    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        SparkSession spark = SparkSession.builder()
                .appName("Processor")
                .master("local[4]")
                .getOrCreate();

        StructType schema = new StructType()
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

        Dataset<Row> kafkaDf = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", KAFKA_BOOTSTRAP_SERVERS)
                .option("subscribe", "clickstream-events")
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
                .withColumn("page_url", col("event_data.eventParams.page_url"));

        Dataset<Row> validatedDf = eventDf.drop("event_data", "kafka_timestamp");

        validatedDf.printSchema();

        // Write to ClickHouse
        StreamingQuery query = validatedDf.writeStream()
                .foreachBatch((batchDf, batchId) -> {
                    batchDf.write()
                            .format("jdbc")
                            .option("url", CLICKHOUSE_URL)
                            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
                            .option("dbtable", "events")
                            .option("user", "phamviethoa")
                            .option("password", "clickstream_application")
                            .mode("append")
                            .save();
                })
                .outputMode("append")
                .start();

        // Wait for the streaming query to terminate
        query.awaitTermination();
    }
}
