-- ClickHouse Performance Benchmarking Scripts
-- These scripts help measure and optimize ClickHouse query performance

-- 1. Basic Performance Metrics
SELECT
    'Total Events' as metric,
    count() as value,
    formatReadableSize(sum(length(event_id) + length(event_name) + length(user_id))) as data_size
FROM clickstream.events;

-- 2. Query Performance Benchmark
-- Measure execution time for different query patterns

-- Simple aggregation
SELECT 'Simple Count' as query_type, count() as result, now() as timestamp
FROM clickstream.events
WHERE event_date >= today() - 7;

-- Complex aggregation with grouping
SELECT 'Complex Aggregation' as query_type,
       event_date,
       count() as events,
       uniq(user_id) as users,
       uniq(session_id) as sessions,
       sum(revenue) as revenue,
       avg(page_load_time) as avg_load_time
FROM clickstream.events
WHERE event_date >= today() - 30
GROUP BY event_date
ORDER BY event_date;

-- Join-like operations using subqueries
SELECT 'Subquery Performance' as query_type,
       e.event_name,
       count() as event_count,
       avg(e.page_load_time) as avg_load_time
FROM clickstream.events e
WHERE e.event_date >= today() - 7
  AND e.user_id IN (
    SELECT user_id
    FROM clickstream.events
    WHERE event_date >= today() - 7
    GROUP BY user_id
    HAVING count() > 10
)
GROUP BY e.event_name
ORDER BY event_count DESC
LIMIT 10;

-- 3. Index Performance Test
-- Test bloom filter index performance
SELECT 'Index Test - User ID' as test_type,
       count() as result_count,
       now() as timestamp
FROM clickstream.events
WHERE user_id = 'test-user-id'
  AND event_date >= today() - 7;

-- Test session ID index
SELECT 'Index Test - Session ID' as test_type,
       count() as result_count,
       now() as timestamp
FROM clickstream.events
WHERE session_id = 'test-session-id'
  AND event_date >= today() - 7;

-- 4. Materialized View Performance
-- Compare direct query vs materialized view
SELECT 'Direct Query vs MV' as comparison,
       'Direct Query' as query_type,
       count() as events,
       uniq(user_id) as users,
       sum(revenue) as revenue
FROM clickstream.events
WHERE event_date = today();

-- Materialized view query
SELECT 'Direct Query vs MV' as comparison,
       'Materialized View' as query_type,
       sum(total_events) as events,
       uniq(user_id) as users,
       sum(total_revenue) as revenue
FROM clickstream.daily_user_metrics
WHERE date = today();

-- 5. Partition Performance Test
-- Test different partition strategies
SELECT 'Partition Performance' as test_type,
       partition,
       count() as rows,
       formatReadableSize(sum(length(event_id) + length(event_name))) as size
FROM system.parts
WHERE database = 'clickstream' AND table = 'events'
GROUP BY partition
ORDER BY partition DESC
LIMIT 10;

-- 6. Memory Usage Analysis
SELECT 'Memory Usage' as metric,
       formatReadableSize(sum(data_compressed_bytes)) as compressed_size,
       formatReadableSize(sum(data_uncompressed_bytes)) as uncompressed_size,
       round(sum(data_uncompressed_bytes) / sum(data_compressed_bytes), 2) as compression_ratio
FROM system.parts
WHERE database = 'clickstream';

-- 7. Query Complexity Analysis
-- Test different query complexity levels
SELECT 'Query Complexity Level 1' as level,
       event_date,
       count() as events
FROM clickstream.events
WHERE event_date >= today() - 7
GROUP BY event_date;

SELECT 'Query Complexity Level 2' as level,
       event_date,
       event_name,
       count() as events,
       uniq(user_id) as users
FROM clickstream.events
WHERE event_date >= today() - 7
GROUP BY event_date, event_name;

SELECT 'Query Complexity Level 3' as level,
       event_date,
       event_name,
       device_type,
       count() as events,
       uniq(user_id) as users,
       sum(revenue) as revenue,
       avg(page_load_time) as avg_load_time
FROM clickstream.events
WHERE event_date >= today() - 7
GROUP BY event_date, event_name, device_type;

-- 8. Performance Optimization Queries
-- Test optimized query patterns

-- Use PREWHERE for better performance
SELECT 'PREWHERE Optimization' as optimization,
       count() as events
FROM clickstream.events
    PREWHERE event_date >= today() - 7
WHERE event_name = 'page_view';

-- Test LIMIT optimization
SELECT 'LIMIT Optimization' as optimization,
       event_name,
       count() as events
FROM clickstream.events
WHERE event_date >= today() - 7
GROUP BY event_name
ORDER BY events DESC
LIMIT 10;

-- 9. System Performance Metrics
SELECT 'System Performance' as category,
       metric,
       value,
       description
FROM system.metrics
WHERE metric IN (
                 'Query',
                 'SelectQuery',
                 'InsertQuery',
                 'Merge',
                 'ReplicatedMerge',
                 'DelayedInserts',
                 'RejectedInserts',
                 'FailedQuery',
                 'FailedSelectQuery',
                 'FailedInsertQuery'
    )
ORDER BY metric;

-- 10. Query Log Analysis
SELECT 'Query Performance Analysis' as analysis,
       query,
       type,
       formatReadableTimeDelta(query_duration_ms / 1000) as duration,
       formatReadableSize(memory_usage) as memory,
       read_rows,
       read_bytes,
       formatReadableSize(read_bytes) as read_size
FROM system.query_log
WHERE type = 'QueryFinish'
  AND event_time >= now() - INTERVAL 1 HOUR
ORDER BY query_duration_ms DESC
LIMIT 20;