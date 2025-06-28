CREATE DATABASE IF NOT EXISTS clickstream;

CREATE TABLE IF NOT EXISTS clickstream.events (
    -- Core identifiers
    event_id String,
    event_name String,

    -- Timestamps
    event_time DateTime64(3),
    event_date Date MATERIALIZED toDate(event_time),
    event_hour UInt8 MATERIALIZED toHour(event_time),
    event_minute UInt8 MATERIALIZED toMinute(event_time),

    -- User Context
    user_id String,
    anonymous_id Nullable(String),

    -- Session Context
    session_id String,
    session_start_time DateTime64(3),
    session_sequence UInt32,

    -- Application Context
    app_id String,
    schema_version String,

    -- Platform Context
    platform String,

    -- Page Context
    page_url String,
    page_path String,
    page_referrer String,

    -- Marketing Context
    utm_source Nullable(String),
    utm_medium Nullable(String),
    utm_campaign Nullable(String),

    -- Device Context
    device_type String,
    device_brand String,
    os_name String,
    browser_name String,
    browser_version String,
    screen_resolution String,
    user_agent String,
    language String,

    -- Business Context
    product_id Nullable(String),
    product_category Nullable(String),
    order_id Nullable(String),
    revenue Nullable(Float64),
    currency Nullable(String),

    -- Performance metrics
    page_load_time Nullable(Float32),
    time_to_interactive Nullable(Float32),

    -- Metadata
    _ingested_at DateTime64(3) DEFAULT now()
)
    ENGINE = ReplacingMergeTree(_ingested_at)
        PARTITION BY toYYYYMM(event_date)
        ORDER BY (event_date, user_id, session_id, event_id, event_time)
        TTL event_date + toIntervalMonth(18)
        SETTINGS index_granularity = 8192;

ALTER TABLE clickstream.events
    ADD INDEX idx_user_id user_id TYPE bloom_filter GRANULARITY 1;
ALTER TABLE clickstream.events
    ADD INDEX idx_session_id session_id TYPE bloom_filter GRANULARITY 1;
ALTER TABLE clickstream.events
    ADD INDEX idx_event_name event_name TYPE bloom_filter GRANULARITY 1;
ALTER TABLE clickstream.events
    ADD INDEX idx_app_id app_id TYPE bloom_filter GRANULARITY 1;
ALTER TABLE clickstream.events
    ADD INDEX idx_product_id product_id TYPE bloom_filter GRANULARITY 1;
ALTER TABLE clickstream.events
    ADD INDEX idx_order_id order_id TYPE bloom_filter GRANULARITY 1;