CREATE DATABASE IF NOT EXISTS clickstream;

CREATE TABLE IF NOT EXISTS clickstream.events (
    -- Core identifiers
    event_id String,
    event_name String,

    -- Timestamps
    event_time DateTime64(3),
    event_date Date MATERIALIZED toDate(event_time),

    -- User/device identifiers
    user_id String,
    session_id String,

    -- Platform context
    app_id String,
    platform String,
    page_url String,

    -- Custom parameters
    event_params Map(String, String),

    -- Metadata
    _ingested_at DateTime64(3) DEFAULT now()
)
ENGINE = ReplacingMergeTree(_ingested_at)
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, app_id, session_id, event_id)
TTL event_date + toIntervalMonth(18)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS clickstream.sessions (
    -- Core identifiers
    session_id String,
    user_id String,

    -- Timestamps
    session_start DateTime64(3),
    session_end DateTime64(3),
    session_date Date MATERIALIZED toDate(session_start),
    session_duration_seconds UInt32 MATERIALIZED
        dateDiff('second', session_start, session_end),

    -- User/device context
    app_id String,
    platform String,

    -- Device category
    device_category String,

    -- Metadata
    _ingested_at DateTime64(3) DEFAULT now()
)
ENGINE = ReplacingMergeTree(_ingested_at)
PARTITION BY toYYYYMM(session_date)
ORDER BY (session_date, app_id, session_id)
TTL session_date + toIntervalMonth(18)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS clickstream.users (
    -- Core identifiers
    user_id String,
    app_id String,

    -- Timestamps
    first_touch_time DateTime64(3),
    first_touch_date Date MATERIALIZED toDate(first_touch_time),
    last_touch_time DateTime64(3),
    last_touch_date Date MATERIALIZED toDate(last_touch_time),

    -- User attribution
    first_traffic_source String,
    last_traffic_source String,

    -- Metadata
    _updated_at DateTime64(3) DEFAULT now()
)
ENGINE = ReplacingMergeTree(_updated_at)
PARTITION BY toYYYYMM(first_touch_time)
ORDER BY (first_touch_date, app_id, user_id)
TTL first_touch_date + toIntervalMonth(18)
SETTINGS index_granularity = 8192;

CREATE TABLE IF NOT EXISTS clickstream.items (
    -- Core identifiers
    item_id String,
    app_id String,

    -- Content
    name String,
    brand String,
    category String,

    -- Commercial attributes
    price Float64,
    currency String,
    quantity UInt32,

    -- Foreign keys
    event_id String,
    event_time DateTime64(3),
    event_date DATE MATERIALIZED toDate(event_time),

    -- Metadata
    _ingested_at DateTime64(3) DEFAULT now()
)
ENGINE = ReplacingMergeTree(_ingested_at)
PARTITION BY toYYYYMM(event_date)
ORDER BY (event_date, app_id, category, item_id)
TTL event_date + toIntervalMonth(18)
SETTINGS index_granularity = 8192;