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
@Table(name = "traces", schema = "clarops_challenge_schema")
@Getter
@Setter
public class TraceEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(name = "trace_id", nullable = false, unique = true)
  private String traceId;

  @Column(nullable = false)
  private String status;

  @Column(name = "last_event_name")
  private String lastEventName;

  @Column(name = "last_event_result")
  private String lastEventResult;

  @Column(name = "next_expected_event")
  private String nextExpectedEvent;

  @Column(name = "next_expected_before")
  private Instant nextExpectedBefore;

  @Column(name = "events_received", nullable = false)
  private Integer eventsReceived = 0;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @Column(name = "updated_at", nullable = false)
  private Instant updatedAt;

  @Column(name = "completed_at")
  private Instant completedAt;
}
