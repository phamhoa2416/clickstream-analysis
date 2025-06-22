package com.example.clickstream.data;

import org.apache.spark.sql.*;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.spark.sql.functions.*;

public class FakeEventProcessor {

    public static void main(String[] args) throws TimeoutException, StreamingQueryException {
        SparkSession spark = SparkSession.builder()
                .appName("ClickstreamProcessor")
                .master("local[*]")
                .config("spark.sql.shuffle.partitions", "4")
                .config("spark.sql.streaming.checkpointLocation", "/tmp/clickstream-checkpoint")
                .getOrCreate();

        StructType schema = new StructType()
                .add("event_id", DataTypes.StringType)
                .add("event_name", DataTypes.StringType)
                .add("event_time", DataTypes.StringType)
                .add("event_date", DataTypes.StringType)
                .add("user_id", DataTypes.StringType)
                .add("anonymous_id", DataTypes.StringType)
                .add("session_id", DataTypes.StringType)
                .add("session_start_time", DataTypes.StringType)
                .add("session_sequence", DataTypes.IntegerType)
                .add("app_id", DataTypes.StringType)
                .add("schema_version", DataTypes.StringType)
                .add("platform", DataTypes.StringType)
                .add("page_url", DataTypes.StringType)
                .add("page_path", DataTypes.StringType)
                .add("page_referrer", DataTypes.StringType)
                .add("utm_source", DataTypes.StringType)
                .add("utm_medium", DataTypes.StringType)
                .add("utm_campaign", DataTypes.StringType)
                .add("device_type", DataTypes.StringType)
                .add("device_brand", DataTypes.StringType)
                .add("os_name", DataTypes.StringType)
                .add("browser_name", DataTypes.StringType)
                .add("browser_version", DataTypes.StringType)
                .add("screen_resolution", DataTypes.StringType)
                .add("user_agent", DataTypes.StringType)
                .add("language", DataTypes.StringType)
                .add("product_id", DataTypes.StringType)
                .add("product_category", DataTypes.StringType)
                .add("order_id", DataTypes.StringType)
                .add("revenue", DataTypes.DoubleType)
                .add("currency", DataTypes.StringType)
                .add("page_load_time", DataTypes.FloatType)
                .add("time_to_interactive", DataTypes.FloatType);

        Dataset<Row> kafkaStream = spark.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", "localhost:29092")
                .option("subscribe", "clickstream-events-100k")
                .option("startingOffsets", "latest")
                .load();

        Dataset<Row> parsedEvents = kafkaStream
                .selectExpr("CAST(value AS STRING)")
                .select(from_json(col("value"), schema).as("event"))
                .select("event.*")
                .withColumn("event_time", to_timestamp(col("event_time")))  // convert string to timestamp
                .withColumn("event_date", to_date(col("event_time")))       // derived column added here
                .withColumn("session_start_time", to_timestamp(col("session_start_time")))
                .withColumn("_ingested_at", current_timestamp())
                .withColumn("_processed_at", current_timestamp())
                .filter(col("event_id").isNotNull());

        StreamingQuery query = parsedEvents.writeStream()
                .foreachBatch((batchDF, batchId) -> {
                    batchDF.write()
                            .format("jdbc")
                            .option("url", "jdbc:clickhouse://localhost:8123/clickstream_100k")
                            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
                            .option("user", "phamviethoa")
                            .option("password", "clickstream_application")
                            .option("dbtable", "clickstream_100k.events")
                            .mode(SaveMode.Append)
                            .save();
                })
                .outputMode("update")
                .start();

        query.awaitTermination();
    }
}
