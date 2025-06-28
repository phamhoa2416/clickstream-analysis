USE clickstream;

-- Materialized view for daily user metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.daily_user_metrics
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, user_id, app_id)
TTL event_date + toIntervalMonth(12)
AS
    SELECT
        event_date,
        user_id,
        app_id,
        count() AS total_events,
        sumIf(revenue, revenue IS NOT NULL) AS total_revenue,
        sumIf(page_load_time, page_load_time IS NOT NULL) AS total_page_load_time,
        countIf(page_load_time IS NOT NULL) AS count_page_load_time,
        sumIf(time_to_interactive, time_to_interactive IS NOT NULL) AS total_time_to_interactive,
        countIf(time_to_interactive IS NOT NULL) AS count_time_to_interactive
    FROM clickstream.events
GROUP BY event_date, user_id, app_id;

-- Materialized view for hourly user metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.hourly_user_metrics
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, event_hour, event_name)
TTL event_date + toIntervalMonth(6)
AS
    SELECT
        event_date,
        event_hour,
        app_id,
        event_name,
        count() AS event_count,
        sumIf(revenue, revenue IS NOT NULL) AS total_revenue,
    FROM clickstream.events
GROUP BY event_date, event_hour, app_id, event_name;

-- Materialized view for session metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.session_analytics
ENGINE = ReplacingMergeTree(_updated_at)
PARTITION BY (session_start_time)
ORDER BY (session_start_time, session_id, app_id)
TTL toDate(session_start_time) + toIntervalMonth(12)
AS
    SELECT
        session_id,
        app_id,
        user_id,
        session_start_time,
        max(event_time) AS session_end_time,
        dateDiff('second', session_start_time, max(event_time)) AS session_duration_seconds,
        count() AS total_events,
        uniq(event_name) AS unique_events_types,
        sum(revenue) AS session_revenue,
        avg(page_load_time) AS avg_page_load_time,
        platform,
        device_type,
        browser_name,
        page_path AS landing_page,
        utm_source,
        utm_medium,
        utm_campaign,
        now() AS _updated_at
    FROM clickstream.events
    GROUP BY
        session_id,
        app_id,
        user_id,
        session_start_time,
        platform,
        device_type,
        browser_name,
        page_path,
        utm_source,
        utm_medium,
        utm_campaign;

-- Materialized view for marketing attribution
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.marketing_attribution
ENGINE = SummingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, app_id, utm_source, utm_medium, utm_campaign)
TTL event_date + toIntervalMonth(6)
AS
    SELECT
        event_date,
        app_id,
        utm_source,
        utm_medium,
        utm_campaign,
        count() AS total_events,
        sumIf(revenue, revenue IS NOT NULL) AS total_revenue,
        sumIf(page_load_time, page_load_time IS NOT NULL) AS total_page_load_time,
        countIf(page_load_time IS NOT NULL) AS count_page_load_time
    FROM clickstream.events
WHERE coalesce(utm_source, '') != ''
OR coalesce(utm_medium, '') != ''
OR coalesce(utm_campaign, '') != ''
GROUP BY event_date, app_id, utm_source, utm_medium, utm_campaign;

-- Materialized view for advanced user analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.daily_user_advanced_analytics
ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, user_id, app_id)
TTL event_date + toIntervalMonth(12)
AS
    SELECT
        event_date,
        app_id,
        user_id,
        uniqState(session_id) AS total_sessions,
        uniqState(event_name) AS unique_event_types,
        avgState(page_load_time) AS avg_page_load_time,
        avgState(time_to_interactive) AS avg_time_to_interactive,
        minState(event_time) AS first_event_time,
        maxState(event_time) AS last_event_time
    FROM clickstream.events
GROUP BY event_date, app_id, user_id;

-- Materialized view for page performance metrics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.page_performance
ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(date)
ORDER BY (date, app_id, page_path)
TTL date + toIntervalMonth(6)
AS
    SELECT
        event_date as date,
        app_id,
        page_path,
        count() as page_views,
        uniq(user_id) as unique_users,
        uniq(session_id) as unique_sessions,
        avg(page_load_time) as avg_page_load_time,
        avg(time_to_interactive) as avg_time_to_interactive,
        sum(revenue) as total_revenue
    FROM clickstream.events
WHERE page_path != ''
GROUP BY date, app_id, page_path;

-- Materialized view for device analytics
CREATE MATERIALIZED VIEW IF NOT EXISTS clickstream.device_analytics
ENGINE = AggregatingMergeTree
PARTITION BY toYYYYMM(date)
ORDER BY (date, app_id, device_type, platform)
TTL date + toIntervalMonth(6)
AS
    SELECT
        event_date as date,
        app_id,
        device_type,
        platform,
        os_name,
        browser_name,
        count() as total_events,
        uniq(user_id) as unique_users,
        uniq(session_id) as unique_sessions,
        sum(revenue) as total_revenue,
        avg(page_load_time) as avg_page_load_time
    FROM clickstream.events
GROUP BY date, app_id, device_type, platform, os_name, browser_name;

CREATE OR REPLACE VIEW clickstream.daily_user_summary AS
SELECT
    d.event_date,
    d.app_id,
    d.user_id,
    d.total_events,
    d.total_revenue,
    d.total_page_load_time / NULLIF(d.count_page_load_time, 0) AS avg_page_load_time,
    d.total_time_to_interactive / NULLIF(d.count_time_to_interactive, 0) AS avg_time_to_interactive,
    a.total_sessions,
    a.unique_event_types,
    minMerge(a.first_event_time) AS first_event_time,
    maxMerge(a.last_event_time) AS last_event_time
FROM clickstream.daily_user_metrics d
         LEFT JOIN clickstream.daily_user_advanced_analytics a
                   ON d.event_date = a.event_date AND d.app_id = a.app_id AND d.user_id = a.user_id
GROUP BY
    d.event_date, d.app_id, d.user_id, d.total_events, d.total_revenue,
    d.total_page_load_time, d.count_page_load_time,
    d.total_time_to_interactive, d.count_time_to_interactive,
    a.total_sessions, a.unique_event_types;

SET optimize_aggregation_in_order = 1;
SET optimize_skip_unused_shards = 1;
SET optimize_skip_unused_shards_nesting = 1;

