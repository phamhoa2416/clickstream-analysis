//package com.example.clickstream.service;
//
//import org.apache.spark.sql.Column;
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.expressions.Window;
//import org.apache.spark.sql.expressions.WindowSpec;
//import org.springframework.stereotype.Service;
//
//import static org.apache.spark.sql.functions.*;
//
//@Service
//public class SessionService {
//    private static final long SESSION_TIMEOUT = 30 * 60;
//
//    public Dataset<Row> sessionizeEvents(Dataset<Row> events) {
//        WindowSpec windowSpec = Window.partitionBy("userId")
//                .orderBy(col("eventTimestamp"));
//
//        Column prevTimestamp = lag("eventTimestamp", 1).over(windowSpec);
//        Column timeDiff = unix_timestamp(col("eventTimestamp"))
//                .minus(unix_timestamp(prevTimestamp));
//
//        Column session = when(timeDiff.gt(SESSION_TIMEOUT), 1).otherwise(0);
//
//        return events
//                .withColumn("sessionId",
//                        concat_ws("-", col("userId"), sum(session).over(windowSpec)))
//                .groupBy("sessionId")
//                .agg(
//                        first("userId").alias("userId"),
//                        min("eventTimestamp").alias("session_start"),
//                        max("eventTimestamp").alias("session_end"),
//                        count("*").alias("event_count")
////                        (max(col("event_timestamp").cast("long"))
////                                .minus(min(col("event_timestamp").cast("long"))).as("duration_seconds"))
//                );
//    }
//
//}
