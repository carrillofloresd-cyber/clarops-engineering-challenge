-- ============================================================
-- Clarops Challenge — Initial Schema
-- Distributed event tracking, TTL expiration, and operational
-- flow analysis.
-- Idempotent — safe to re-execute.
-- UUIDs must be provided by the application layer.
-- ============================================================
CREATE
  SCHEMA IF NOT EXISTS clarops_challenge_schema;
SET
search_path TO clarops_challenge_schema;

-- -------------------------
-- health
-- Single-row table used by the health endpoint.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS health(
      id BIGSERIAL PRIMARY KEY,
      message VARCHAR(255) NOT NULL
    );

INSERT
  INTO
    health(message) SELECT
      'clarops sr engineer challenge'
    WHERE
      NOT EXISTS(
        SELECT
          1
        FROM
          health
      );

-- -------------------------
-- traces
-- Current state for each distributed trace.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS traces(
      id BIGSERIAL PRIMARY KEY,
      trace_id VARCHAR(255) NOT NULL UNIQUE,
      status VARCHAR(50) NOT NULL,
      last_event_name VARCHAR(255),
      last_event_result VARCHAR(20),
      next_expected_event VARCHAR(255),
      next_expected_before TIMESTAMPTZ,
      events_received INTEGER NOT NULL DEFAULT 0,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      completed_at TIMESTAMPTZ,
      CONSTRAINT traces_status_chk CHECK(
        status IN(
          'STARTED',
          'WAITING_OTHER_EVENT',
          'TTL_EXPIRED_FOR_EVENT',
          'COMPLETED'
        )
      ),
      CONSTRAINT traces_last_event_result_chk CHECK(
        last_event_result IS NULL
        OR last_event_result IN(
          'SUCCESS',
          'ERROR'
        )
      )
    );

-- -------------------------
-- trace_events
-- Full event history for each trace.
-- -------------------------
CREATE
  TABLE
    IF NOT EXISTS trace_events(
      id BIGSERIAL PRIMARY KEY,
      trace_id VARCHAR(255) NOT NULL,
      event_id VARCHAR(255) NOT NULL UNIQUE,
      event_name VARCHAR(255) NOT NULL,
      RESULT VARCHAR(20) NOT NULL,
      occurred_at TIMESTAMPTZ NOT NULL,
      next_expected_event VARCHAR(255),
      next_event_ttl_seconds INTEGER,
      final_event BOOLEAN NOT NULL DEFAULT FALSE,
      metadata JSONB NOT NULL DEFAULT '{}'::jsonb,
      created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
      CONSTRAINT trace_events_trace_fk FOREIGN KEY(trace_id) REFERENCES traces(trace_id) ON
      DELETE
        CASCADE,
        CONSTRAINT trace_events_result_chk CHECK(
          RESULT IN(
            'SUCCESS',
            'ERROR'
          )
        ),
        CONSTRAINT trace_events_ttl_chk CHECK(
          next_event_ttl_seconds IS NULL
          OR next_event_ttl_seconds >= 0
        )
    );

CREATE
  INDEX IF NOT EXISTS idx_trace_events_trace_id_occurred_at ON
  trace_events(
    trace_id,
    occurred_at
  );

CREATE
  INDEX IF NOT EXISTS idx_traces_status ON
  traces(status);
