//package com.example.clickstream.validation;
//
//
//import org.apache.spark.sql.Dataset;
//import org.apache.spark.sql.Row;
//import org.apache.spark.sql.SparkSession;
//import org.apache.spark.sql.api.java.UDF1;
//import org.apache.spark.sql.types.DataTypes;
//import org.springframework.stereotype.Component;
//
//import java.time.Instant;
//
//import static org.apache.spark.sql.functions.callUDF;
//import static org.apache.spark.sql.functions.col;
//
//@Component
//public class EventValidator {
//    private final SparkSession sparkSession;
//
//    public EventValidator(SparkSession sparkSession) {
//        this.sparkSession = sparkSession;
//        registerUDFs();
//    }
//
//    public Dataset<Row> validateEvents(Dataset<Row> events) {
//        return events
//                .filter((col("eventName").isNotNull())
//                        .and(col("eventTimestamp").isNotNull())
//                        .and(col("userId").isNotNull())
//                        .and(col("sessionId").isNotNull()))
//                .filter(callUDF("validUrl", col("pageUrl")))
//                .filter(callUDF("validTimestamp", col("eventTimestamp")));
//    }
//
//    private void registerUDFs() {
//        sparkSession.udf().register("validUrl", (UDF1<String, Boolean>) url -> {
//            if (url == null) return true;
//            return url.matches("^(https?|ftp)://[^\\s/$.?#].[^\\s]*$");
//        }, DataTypes.BooleanType);
//
//        sparkSession.udf().register("validTimestamp", (UDF1<String, Boolean>) timestamp -> {
//            try {
//                Instant.parse(timestamp);
//                return true;
//            } catch (Exception e) {
//                return false;
//            }
//        }, DataTypes.BooleanType);
//    }
//}
