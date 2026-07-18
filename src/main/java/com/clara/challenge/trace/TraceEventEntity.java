package com.clara.challenge.trace;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "trace_events", schema = "clarops_challenge_schema")
@Getter
@Setter
public class TraceEventEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "trace_id", nullable = false)
  private String traceId;

  @Column(name = "event_id", nullable = false, unique = true)
  private String eventId;

  @Column(name = "event_name", nullable = false)
  private String eventName;

  @Column(nullable = false)
  private String result;

  @Column(name = "occurred_at", nullable = false)
  private Instant occurredAt;

  @Column(name = "next_expected_event")
  private String nextExpectedEvent;

  @Column(name = "next_event_ttl_seconds")
  private Integer nextEventTtlSeconds;

  @Column(name = "final_event", nullable = false)
  private Boolean finalEvent = false;

  @Column(nullable = false)
  private String metadata;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;
}
