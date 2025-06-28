//package com.example.analytics.service;
//
//import com.example.clickstream.config.AppConfig;
//import lombok.extern.slf4j.Slf4j;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.jdbc.core.JdbcTemplate;
//import org.springframework.stereotype.Service;
//
//import java.time.LocalDate;
//import java.time.LocalDateTime;
//import java.util.*;
//import java.util.concurrent.CompletableFuture;
//import java.util.concurrent.TimeUnit;
//
//@Slf4j
//@Service
//public class AnalyticsService {
//    private final JdbcTemplate clickHouseJdbcTemplate;
//    private final AppConfig appConfig;
//
//    @Autowired
//    public AnalyticsService(
//            JdbcTemplate clickHouseJdbcTemplate,
//            AppConfig appConfig) {
//        this.clickHouseJdbcTemplate = clickHouseJdbcTemplate;
//        this.appConfig = appConfig;
//    }
//
//    // Performance Benchmarking Methods
//
//    public Map<String, Object> benchmarkQueryPerformance() {
//        Map<String, Object> results = new HashMap<>();
//        List<String> queries = getBenchmarkQueries();
//
//        for (String queryName : queries.keySet()) {
//            String query = queries.get(queryName);
//            long startTime = System.currentTimeMillis();
//
//            try {
//                List<Map<String, Object>> result = clickHouseJdbcTemplate.queryForList(query);
//                long executionTime = System.currentTimeMillis() - startTime;
//
//                results.put(queryName + "_execution_time_ms", executionTime);
//                results.put(queryName + "_result_count", result.size());
//                results.put(queryName + "_status", "success");
//
//                log.info("Query '{}' executed in {} ms, returned {} rows", queryName, executionTime, result.size());
//            } catch (Exception e) {
//                results.put(queryName + "_status", "error");
//                results.put(queryName + "_error", e.getMessage());
//                log.error("Query '{}' failed: {}", queryName, e.getMessage());
//            }
//        }
//
//        return results;
//    }
//
//    private Map<String, String> getBenchmarkQueries() {
//        Map<String, String> queries = new LinkedHashMap<>();
//
//        // Basic aggregation queries
//        queries.put("total_events_count",
//                "SELECT count() as total_events FROM clickstream.events");
//
//        queries.put("daily_events_trend",
//                "SELECT event_date, count() as events FROM clickstream.events " +
//                        "WHERE event_date >= today() - 30 GROUP BY event_date ORDER BY event_date");
//
//        queries.put("top_event_types",
//                "SELECT event_name, count() as count FROM clickstream.events " +
//                        "WHERE event_date >= today() - 7 GROUP BY event_name ORDER BY count DESC LIMIT 10");
//
//        queries.put("user_engagement",
//                "SELECT user_id, count() as events, uniq(session_id) as sessions " +
//                        "FROM clickstream.events WHERE event_date >= today() - 7 " +
//                        "GROUP BY user_id ORDER BY events DESC LIMIT 100");
//
//        queries.put("page_performance",
//                "SELECT page_path, avg(page_load_time) as avg_load_time, count() as views " +
//                        "FROM clickstream.events WHERE page_load_time > 0 AND event_date >= today() - 7 " +
//                        "GROUP BY page_path ORDER BY avg_load_time DESC LIMIT 20");
//
//        queries.put("revenue_analysis",
//                "SELECT event_date, sum(revenue) as daily_revenue, uniq(user_id) as paying_users " +
//                        "FROM clickstream.events WHERE revenue > 0 AND event_date >= today() - 30 " +
//                        "GROUP BY event_date ORDER BY event_date");
//
//        return queries;
//    }
//
//    // Real-time Analytics Methods
//
//    public Map<String, Object> getRealTimeMetrics() {
//        Map<String, Object> metrics = new HashMap<>();
//
//        try {
//            // Current hour metrics
//            LocalDateTime now = LocalDateTime.now();
//            int currentHour = now.getHour();
//            LocalDate today = LocalDate.now();
//
//            String currentHourQuery =
//                    "SELECT count() as events, uniq(user_id) as users, uniq(session_id) as sessions, " +
//                            "sum(revenue) as revenue FROM clickstream.events " +
//                            "WHERE event_date = today() AND event_hour = " + currentHour;
//
//            Map<String, Object> currentHourMetrics = clickHouseJdbcTemplate.queryForMap(currentHourQuery);
//            metrics.put("current_hour", currentHourMetrics);
//
//            // Today's metrics
//            String todayQuery =
//                    "SELECT count() as events, uniq(user_id) as users, uniq(session_id) as sessions, " +
//                            "sum(revenue) as revenue, avg(page_load_time) as avg_load_time " +
//                            "FROM clickstream.events WHERE event_date = today()";
//
//            Map<String, Object> todayMetrics = clickHouseJdbcTemplate.queryForMap(todayQuery);
//            metrics.put("today", todayMetrics);
//
//            // Top performing pages today
//            String topPagesQuery =
//                    "SELECT page_path, count() as views FROM clickstream.events " +
//                            "WHERE event_date = today() AND page_path != '' " +
//                            "GROUP BY page_path ORDER BY views DESC LIMIT 10";
//
//            List<Map<String, Object>> topPages = clickHouseJdbcTemplate.queryForList(topPagesQuery);
//            metrics.put("top_pages_today", topPages);
//
//        } catch (Exception e) {
//            log.error("Error getting real-time metrics: {}", e.getMessage());
//            metrics.put("error", e.getMessage());
//        }
//
//        return metrics;
//    }
//
//    // User Analytics Methods
//
//    public Map<String, Object> getUserAnalytics(String userId, LocalDate startDate, LocalDate endDate) {
//        Map<String, Object> analytics = new HashMap<>();
//
//        try {
//            // User activity summary
//            String userSummaryQuery =
//                    "SELECT count() as total_events, uniq(session_id) as total_sessions, " +
//                            "sum(revenue) as total_revenue, avg(page_load_time) as avg_page_load_time, " +
//                            "min(event_time) as first_event, max(event_time) as last_event " +
//                            "FROM clickstream.events " +
//                            "WHERE user_id = ? AND event_date BETWEEN ? AND ?";
//
//            Map<String, Object> userSummary = clickHouseJdbcTemplate.queryForMap(
//                    userSummaryQuery, userId, startDate, endDate);
//            analytics.put("summary", userSummary);
//
//            // User's event timeline
//            String timelineQuery =
//                    "SELECT event_date, event_name, page_path, revenue, page_load_time " +
//                            "FROM clickstream.events " +
//                            "WHERE user_id = ? AND event_date BETWEEN ? AND ? " +
//                            "ORDER BY event_time DESC LIMIT 100";
//
//            List<Map<String, Object>> timeline = clickHouseJdbcTemplate.queryForList(
//                    timelineQuery, userId, startDate, endDate);
//            analytics.put("timeline", timeline);
//
//            // User's session analysis
//            String sessionQuery =
//                    "SELECT session_id, count() as events, sum(revenue) as revenue, " +
//                            "avg(page_load_time) as avg_load_time, min(event_time) as start_time, " +
//                            "max(event_time) as end_time " +
//                            "FROM clickstream.events " +
//                            "WHERE user_id = ? AND event_date BETWEEN ? AND ? " +
//                            "GROUP BY session_id ORDER BY start_time DESC";
//
//            List<Map<String, Object>> sessions = clickHouseJdbcTemplate.queryForList(
//                    sessionQuery, userId, startDate, endDate);
//            analytics.put("sessions", sessions);
//
//        } catch (Exception e) {
//            log.error("Error getting user analytics for {}: {}", userId, e.getMessage());
//            analytics.put("error", e.getMessage());
//        }
//
//        return analytics;
//    }
//
//    // Session Analytics Methods
//    public Map<String, Object> getSessionAnalytics(LocalDate date) {
//        Map<String, Object> analytics = new HashMap<>();
//
//        try {
//            // Session duration distribution
//            String durationQuery =
//                    "SELECT session_duration_seconds, count() as session_count " +
//                            "FROM clickstream.session_analytics " +
//                            "WHERE session_date = ? " +
//                            "GROUP BY session_duration_seconds ORDER BY session_duration_seconds";
//
//            List<Map<String, Object>> durationDistribution = clickHouseJdbcTemplate.queryForList(
//                    durationQuery, date);
//            analytics.put("duration_distribution", durationDistribution);
//
//            // Top landing pages
//            String landingPagesQuery =
//                    "SELECT landing_page, count() as sessions, avg(session_duration_seconds) as avg_duration " +
//                            "FROM clickstream.session_analytics " +
//                            "WHERE session_date = ? AND landing_page != '' " +
//                            "GROUP BY landing_page ORDER BY sessions DESC LIMIT 10";
//
//            List<Map<String, Object>> landingPages = clickHouseJdbcTemplate.queryForList(
//                    landingPagesQuery, date);
//            analytics.put("top_landing_pages", landingPages);
//
//            // Device and platform breakdown
//            String deviceQuery =
//                    "SELECT device_type, platform, count() as sessions, " +
//                            "avg(session_duration_seconds) as avg_duration " +
//                            "FROM clickstream.session_analytics " +
//                            "WHERE session_date = ? " +
//                            "GROUP BY device_type, platform ORDER BY sessions DESC";
//
//            List<Map<String, Object>> deviceBreakdown = clickHouseJdbcTemplate.queryForList(
//                    deviceQuery, date);
//            analytics.put("device_breakdown", deviceBreakdown);
//
//        } catch (Exception e) {
//            log.error("Error getting session analytics for {}: {}", date, e.getMessage());
//            analytics.put("error", e.getMessage());
//        }
//
//        return analytics;
//    }
//
//    // Marketing Attribution Methods
//
//    public Map<String, Object> getMarketingAttribution(LocalDate startDate, LocalDate endDate) {
//        Map<String, Object> attribution = new HashMap<>();
//
//        try {
//            // Campaign performance
//            String campaignQuery =
//                    "SELECT utm_campaign, utm_source, utm_medium, " +
//                            "sum(total_events) as events, sum(unique_users) as users, " +
//                            "sum(total_revenue) as revenue " +
//                            "FROM clickstream.marketing_attribution " +
//                            "WHERE date BETWEEN ? AND ? " +
//                            "GROUP BY utm_campaign, utm_source, utm_medium " +
//                            "ORDER BY revenue DESC";
//
//            List<Map<String, Object>> campaignPerformance = clickHouseJdbcTemplate.queryForList(
//                    campaignQuery, startDate, endDate);
//            attribution.put("campaign_performance", campaignPerformance);
//
//            // Source breakdown
//            String sourceQuery =
//                    "SELECT utm_source, sum(total_events) as events, sum(unique_users) as users, " +
//                            "sum(total_revenue) as revenue, avg(avg_page_load_time) as avg_load_time " +
//                            "FROM clickstream.marketing_attribution " +
//                            "WHERE date BETWEEN ? AND ? AND utm_source != '' " +
//                            "GROUP BY utm_source ORDER BY revenue DESC";
//
//            List<Map<String, Object>> sourceBreakdown = clickHouseJdbcTemplate.queryForList(
//                    sourceQuery, startDate, endDate);
//            attribution.put("source_breakdown", sourceBreakdown);
//
//        } catch (Exception e) {
//            log.error("Error getting marketing attribution: {}", e.getMessage());
//            attribution.put("error", e.getMessage());
//        }
//
//        return attribution;
//    }
//
//    // Data Archiving Methods
//
//    public Map<String, Object> archiveOldData(LocalDate archiveDate) {
//        Map<String, Object> result = new HashMap<>();
//
//        try {
//            // Archive events older than the specified date
//            String archiveQuery =
//                    "INSERT INTO clickstream.events_archive " +
//                            "SELECT event_id, event_name, event_time, event_date, user_id, session_id, " +
//                            "app_id, platform, page_url, page_path, device_type, browser_name, " +
//                            "revenue, page_load_time, _ingested_at, now() as _archived_at " +
//                            "FROM clickstream.events " +
//                            "WHERE event_date < ?";
//
//            int archivedRows = clickHouseJdbcTemplate.update(archiveQuery, archiveDate);
//            result.put("archived_rows", archivedRows);
//            result.put("archive_date", archiveDate.toString());
//            result.put("status", "success");
//
//            log.info("Archived {} rows from events table for date {}", archivedRows, archiveDate);
//
//        } catch (Exception e) {
//            log.error("Error archiving data: {}", e.getMessage());
//            result.put("error", e.getMessage());
//            result.put("status", "error");
//        }
//
//        return result;
//    }
//
//    // Data Quality Methods
//
//    public Map<String, Object> getDataQualityMetrics() {
//        Map<String, Object> qualityMetrics = new HashMap<>();
//
//        try {
//            // Check for null values in critical fields
//            String nullCheckQuery =
//                    "SELECT " +
//                            "countIf(event_id = '') as null_event_ids, " +
//                            "countIf(event_name = '') as null_event_names, " +
//                            "countIf(user_id = '') as null_user_ids, " +
//                            "countIf(session_id = '') as null_session_ids, " +
//                            "countIf(event_time = 0) as null_timestamps " +
//                            "FROM clickstream.events " +
//                            "WHERE event_date >= today() - 7";
//
//            Map<String, Object> nullChecks = clickHouseJdbcTemplate.queryForMap(nullCheckQuery);
//            qualityMetrics.put("null_checks", nullChecks);
//
//            // Check for future timestamps
//            String futureTimestampQuery =
//                    "SELECT count() as future_timestamps " +
//                            "FROM clickstream.events " +
//                            "WHERE event_time > now() AND event_date >= today() - 7";
//
//            Map<String, Object> futureTimestamps = clickHouseJdbcTemplate.queryForMap(futureTimestampQuery);
//            qualityMetrics.put("future_timestamps", futureTimestamps);
//
//            // Check for duplicate events
//            String duplicateQuery =
//                    "SELECT count() as duplicate_events " +
//                            "FROM (SELECT event_id, count() as cnt FROM clickstream.events " +
//                            "WHERE event_date >= today() - 7 GROUP BY event_id HAVING cnt > 1)";
//
//            Map<String, Object> duplicates = clickHouseJdbcTemplate.queryForMap(duplicateQuery);
//            qualityMetrics.put("duplicate_events", duplicates);
//
//        } catch (Exception e) {
//            log.error("Error getting data quality metrics: {}", e.getMessage());
//            qualityMetrics.put("error", e.getMessage());
//        }
//
//        return qualityMetrics;
//    }
//
//    // Async Methods for Performance
//
//    public CompletableFuture<Map<String, Object>> getRealTimeMetricsAsync() {
//        return CompletableFuture.supplyAsync(this::getRealTimeMetrics);
//    }
//
//    public CompletableFuture<Map<String, Object>> getUserAnalyticsAsync(String userId, LocalDate startDate, LocalDate endDate) {
//        return CompletableFuture.supplyAsync(() -> getUserAnalytics(userId, startDate, endDate));
//    }
//}