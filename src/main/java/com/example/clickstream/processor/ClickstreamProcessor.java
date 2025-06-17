package com.example.clickstream.processor;

import com.example.clickstream.config.AppConfig;
import com.example.monitoring.metrics.ClickstreamMetrics;
import org.apache.spark.sql.Dataset;
import org.apache.spark.sql.Row;
import org.apache.spark.sql.SparkSession;
import org.apache.spark.sql.streaming.StreamingQuery;
import org.apache.spark.sql.streaming.StreamingQueryException;
import org.apache.spark.sql.types.DataTypes;
import org.apache.spark.sql.types.StructType;

import java.util.concurrent.TimeoutException;

import static org.apache.avro.LogicalTypes.uuid;
import static org.apache.spark.sql.functions.*;

public class ClickstreamProcessor {
    private final SparkSession sparkSession;
    private final AppConfig appConfig;
    private final ClickstreamMetrics metrics;

    public ClickstreamProcessor(
            SparkSession sparkSession,
            AppConfig appConfig,
            ClickstreamMetrics clickstreamMetrics
    ) {
        this.sparkSession = sparkSession;
        this.appConfig = appConfig;
        this.metrics = clickstreamMetrics;
    }

    public void startProcessing() throws StreamingQueryException, TimeoutException {
        StructType schema = createSchema();

        Dataset<Row> kafkaDf = sparkSession.readStream()
                .format("kafka")
                .option("kafka.bootstrap.servers", appConfig.getKafkaBootstrapServers())
                .option("subscribe", appConfig.getKafkaTopic())
                .option("startingOffsets", "earliest")
                .option("maxOffsetsPerTrigger", 10000)
                .load();

        Dataset<Row> eventDf = kafkaDf
                .select(
                        from_json(col("value").cast("string"), schema).alias("event_data"),
                        col("timestamp").alias("kafka_timestamp")
                )
                .withColumn("event_id", col("event_data.eventId"))
                .withColumn("event_name", lower(col("event_data.eventName")))
                .withColumn("event_time", to_timestamp(col("event_data.eventTimestamp")))

                .withColumn("user_id", col("event_data.userId"))
                .withColumn("anonymous_id",
                    when(col("user_id").isNull(), expr("uuid()"))
                            .otherwise(null)
                    )

                .withColumn("session_id", col("event_data.sessionId"))
                .withColumn("session_start_time", to_timestamp(col("event_data.sessionStartTime")))
                .withColumn("session_sequence", col("event_data.sessionSequence"))

                .withColumn("app_id", col("event_data.appId"))
                .withColumn("schema_version", col("event_data.schemaVersion"))

                .withColumn("platform", col("event_data.platform"))
                .withColumn("page_url", trim(col("event_data.pageUrl")))
                .withColumn("page_path",
                        regexp_extract(col("page_url"), "^https?://[^/]+(/[^?]*)", 1)) // Enrich: extract path
                .withColumn("page_referrer", col("event_data.pageReferrer"))

                .withColumn("utm_source", col("event_data.utmSource"))
                .withColumn("utm_medium", col("event_data.utmMedium"))
                .withColumn("utm_campaign", col("event_data.utmCampaign"))

                .withColumn("device_type", col("event_data.deviceType"))
                .withColumn("device_brand", col("event_data.deviceBrand"))
                .withColumn("os_name", col("event_data.osName"))
                .withColumn("browser_name", col("event_data.browserName"))
                .withColumn("browser_version", col("event_data.browserVersion"))
                .withColumn("screen_resolution", col("event_data.screenResolution"))
                .withColumn("user_agent", col("event_data.userAgent"))
                .withColumn("language", col("event_data.eventParams.language"))

                .withColumn("product_id", col("event_data.productId"))
                .withColumn("product_category", col("event_data.productCategory"))
                .withColumn("order_id", col("event_data.orderId"))
                .withColumn("revenue", col("event_data.revenue").cast("double"))
                .withColumn("currency", col("event_data.currency"))

                .withColumn("page_load_time", col("event_data.pageLoadTime").cast("float"))
                .withColumn("time_to_interactive", col("event_data.timeToInteractive").cast("float"))
//                .withColumn("_processed_at", current_timestamp())

                .withWatermark("event_time", "10 minutes")
                .filter(col("event_id").isNotNull().and(col("event_name").isNotNull()))

                .drop("event_data", "kafka_timestamp");

        StreamingQuery query = eventDf.writeStream()
                .foreachBatch((batchDf, batchId) -> {
                    try {
                        metrics.recordBatchMetrics(batchDf);
                        Row[] rows = (Row[]) batchDf.select(
                                avg(unix_timestamp(col("event_time")).multiply(1000)).cast("long")
                        ).collect();

                        if (rows.length > 0 && !rows[0].isNullAt(0)) {
                            long lag = System.currentTimeMillis() - rows[0].getLong(0);
                            metrics.updateLag(lag);
                        }

                        batchDf.write()
                                .format("jdbc")
                                .option("url", appConfig.getClickhouseUrl())
                                .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
                                .option("dbtable", "clickstream.events")
                                .option("user", appConfig.getClickhouseUser())
                                .option("password", appConfig.getClickhousePassword())
                                .mode("append")
                                .save();
                    } catch (Exception e) {
                        metrics.recordError("batch_processing_error");
                        throw new RuntimeException("Failed to process batch " + batchId, e);
                    }
                })
                .option("checkpointLocation", appConfig.getSparkCheckpointLocation())
                .trigger(org.apache.spark.sql.streaming.Trigger.ProcessingTime("1 second"))
                .outputMode("append")
                .start();

        query.awaitTermination();
    }

    private StructType createSchema() {
        return new StructType()
                .add("eventId", DataTypes.StringType)
                .add("eventName", DataTypes.StringType)
                .add("eventTimestamp", DataTypes.StringType)
                .add("userId", DataTypes.StringType)
                .add("sessionId", DataTypes.StringType)
                .add("sessionStartTime", DataTypes.StringType)
                .add("sessionSequence", DataTypes.IntegerType)
                .add("appId", DataTypes.StringType)
                .add("schemaVersion", DataTypes.StringType)
                .add("platform", DataTypes.StringType)
                .add("pageUrl", DataTypes.StringType)
                .add("pageReferrer", DataTypes.StringType)
                .add("utmSource", DataTypes.StringType)
                .add("utmMedium", DataTypes.StringType)
                .add("utmCampaign", DataTypes.StringType)
                .add("deviceType", DataTypes.StringType)
                .add("deviceBrand", DataTypes.StringType)
                .add("osName", DataTypes.StringType)
                .add("browserName", DataTypes.StringType)
                .add("browserVersion", DataTypes.StringType)
                .add("screenResolution", DataTypes.StringType)
                .add("userAgent", DataTypes.StringType)
                .add("productId", DataTypes.StringType)
                .add("productCategory", DataTypes.StringType)
                .add("orderId", DataTypes.StringType)
                .add("revenue", DataTypes.DoubleType)
                .add("currency", DataTypes.StringType)
                .add("pageLoadTime", DataTypes.FloatType)
                .add("timeToInteractive", DataTypes.FloatType)
                .add("eventParams", new StructType()
                        .add("event_id", DataTypes.StringType)
                        .add("event_name", DataTypes.StringType)
                        .add("event_time", DataTypes.StringType)
                        .add("user_id", DataTypes.StringType)
                        .add("element_text", DataTypes.StringType)
                        .add("track", DataTypes.StringType)
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

//package com.example.clickstream.processor;
//
//import com.example.clickstream.config.AppConfig;
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.SparkSession;
//import org.apache.spark.sql.streaming.StreamingQuery;
//import org.apache.spark.sql.streaming.StreamingQueryException;
//import org.apache.spark.sql.types.DataTypes;
//import org.apache.spark.sql.types.StructType;
//
//import java.util.concurrent.TimeoutException;
//
//import static org.apache.spark.sql.functions.*;
//import static org.apache.spark.sql.functions.to_timestamp;
//
//public class ClickstreamProcessor {
//    private final SparkSession sparkSession;
//    private final AppConfig appConfig;
//
//    public ClickstreamProcessor(
//            SparkSession sparkSession,
//            AppConfig appConfig) {
//        this.sparkSession = sparkSession;
//        this.appConfig = appConfig;
//    }
//
//    public void startProcessing() throws StreamingQueryException, TimeoutException {
//        StructType schema = createSchema();
//
//        Dataset<Row> kafkaDf = sparkSession.readStream()
//                .format("kafka")
//                .option("kafka.bootstrap.servers", appConfig.getKafkaBootstrapServers())
//                .option("subscribe", appConfig.getKafkaTopic())
//                .option("startingOffsets", "earliest")
//                .load();
//
//        Dataset<Row> eventDf = kafkaDf
//                .select(
//                        from_json(col("value").cast("string"), schema).alias("event_data"),
//                        col("timestamp").alias("kafka_timestamp")
//                )
//                .withColumn("event_id", col("event_data.eventId"))
//                .withColumn("event_name", col("event_data.eventName"))
//                .withColumn("event_time", date_format(to_timestamp(col("event_data.eventParams.event_time")), "yyyy-MM-dd HH:mm:ss").cast("string"))
//                .withColumn("user_id", col("event_data.userId"))
//                .withColumn("session_id", col("event_data.sessionId"))
////                .withColumn("app_id", col("event_data.appId"))
//                .withColumn("platform", col("event_data.platform"))
//                .withColumn("page_url", col("event_data.eventParams.page_url"))
//                .drop("event_data", "kafka_timestamp");
//
//        StreamingQuery query = eventDf.writeStream()
//                .foreachBatch((batchDf, batchId) -> {
//                    batchDf.write()
//                            .format("jdbc")
//                            .option("url", appConfig.getClickhouseUrl())
//                            .option("driver", "com.clickhouse.jdbc.ClickHouseDriver")
//                            .option("dbtable", "events")
//                            .option("user", appConfig.getClickhouseUser())
//                            .option("password", appConfig.getClickhousePassword())
//                            .mode("append")
//                            .save();
//                })
//                .outputMode("append")
//                .start();
//
//        query.awaitTermination();
//    }
//
//    private StructType createSchema() {
//        return new StructType()
//                .add("eventId", DataTypes.StringType)
//                .add("eventName", DataTypes.StringType)
//                .add("eventTime", DataTypes.StringType)
//                .add("userId", DataTypes.StringType)
//                .add("sessionId", DataTypes.StringType)
//                .add("appId", DataTypes.StringType)
//                .add("platform", DataTypes.StringType)
//                .add("eventParams", new StructType()
//                        .add("event_id", DataTypes.StringType)
//                        .add("event_name", DataTypes.StringType)
//                        .add("event_time", DataTypes.StringType)
//                        .add("user_id", DataTypes.StringType)
//                        .add("element_text", DataTypes.StringType)
//                        .add("track", DataTypes.StringType)
//                        .add("app_id", DataTypes.StringType)
//                        .add("platform", DataTypes.StringType)
//                        .add("page_url", DataTypes.StringType)
//                        .add("page_referrer", DataTypes.StringType)
//                        .add("screen_resolution", DataTypes.StringType)
//                        .add("viewport_size", DataTypes.StringType)
//                        .add("element_type", DataTypes.StringType)
//                        .add("element_class", DataTypes.StringType)
//                        .add("language", DataTypes.StringType)
//                        .add("user_agent", DataTypes.StringType)
//                        .add("page_path", DataTypes.StringType)
//                        .add("productId", DataTypes.StringType)
//                        .add("sessionId", DataTypes.StringType)
//                );
//    }
//}