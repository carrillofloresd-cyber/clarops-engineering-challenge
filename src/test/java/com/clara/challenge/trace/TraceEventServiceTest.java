package com.clara.challenge.trace;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TraceEventServiceTest {

  private TraceRepository traceRepository;
  private TraceEventRepository eventRepository;
  private TraceEventService service;
  private final Map<String, TraceEntity> traces = new HashMap<>();
  private final Map<String, TraceEventEntity> events = new HashMap<>();

  @BeforeEach
  void setUp() {
    traceRepository = mock(TraceRepository.class);
    eventRepository = mock(TraceEventRepository.class);

    when(traceRepository.findByTraceId(anyString()))
        .thenAnswer(invocation -> Optional.ofNullable(traces.get(invocation.getArgument(0))));
    when(traceRepository.save(any(TraceEntity.class)))
        .thenAnswer(
            invocation -> {
              TraceEntity entity = invocation.getArgument(0);
              traces.put(entity.getTraceId(), entity);
              return entity;
            });

    when(eventRepository.existsByEventId(anyString()))
        .thenAnswer(invocation -> events.containsKey(invocation.getArgument(0)));
    when(eventRepository.save(any(TraceEventEntity.class)))
        .thenAnswer(
            invocation -> {
              TraceEventEntity entity = invocation.getArgument(0);
              events.put(entity.getEventId(), entity);
              return entity;
            });

    service =
        new TraceEventService(
            traceRepository,
            eventRepository,
            Clock.fixed(Instant.parse("2026-06-15T10:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  void shouldCreateStartedTrace_WhenFirstEventHasNoNextExpectedEvent() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-1",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            null,
            null,
            false,
            Map.of("country", "MX"));

    TraceStatusResponse response = service.recordEvent(request);

    assertThat(response.status()).isEqualTo(TraceStatus.STARTED);
    assertThat(response.eventsReceived()).isEqualTo(1);
    assertThat(traceRepository.findByTraceId("trace-1")).isPresent();
  }

  @Test
  void shouldMoveTraceToWaiting_WhenEventDefinesNextExpectedEvent() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-2",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            "RULES_EVALUATED",
            120,
            false,
            Map.of());

    TraceStatusResponse response = service.recordEvent(request);

    assertThat(response.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(response.nextExpectedEvent()).isEqualTo("RULES_EVALUATED");
    assertThat(response.nextExpectedBefore()).isEqualTo(Instant.parse("2026-06-15T10:02:00Z"));
  }

  @Test
  void shouldMarkTraceCompleted_WhenFinalEventIsReceived() {
    EventRequest request =
        new EventRequest(
            "evt-1",
            "trace-3",
            "APPLICATION_RECEIVED",
            "ERROR",
            Instant.parse("2026-06-15T10:00:00Z"),
            null,
            null,
            true,
            Map.of());

    TraceStatusResponse response = service.recordEvent(request);

    assertThat(response.status()).isEqualTo(TraceStatus.COMPLETED);
    assertThat(traceRepository.findByTraceId("trace-3").orElseThrow().getCompletedAt()).isNotNull();
  }

  @Test
  void shouldRejectNewEvent_WhenTraceIsAlreadyCompleted() {
    service.recordEvent(
        new EventRequest(
            "evt-1",
            "trace-4",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            null,
            null,
            true,
            Map.of()));

    assertThatThrownBy(
            () ->
                service.recordEvent(
                    new EventRequest(
                        "evt-2",
                        "trace-4",
                        "RULES_EVALUATED",
                        "SUCCESS",
                        Instant.parse("2026-06-15T10:00:30Z"),
                        null,
                        null,
                        false,
                        Map.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Completed trace");
  }

  @Test
  void shouldKeepWaitingStatus_WhenDeadlineHasNotPassed() {
    service.recordEvent(
        new EventRequest(
            "evt-1",
            "trace-5",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            "RULES_EVALUATED",
            60,
            false,
            Map.of()));

    TraceEventService beforeDeadlineService =
        new TraceEventService(
            traceRepository,
            eventRepository,
            Clock.fixed(Instant.parse("2026-06-15T10:00:30Z"), ZoneOffset.UTC));

    TraceStatusResponse response = beforeDeadlineService.getTraceStatus("trace-5");

    assertThat(response.status()).isEqualTo(TraceStatus.WAITING_OTHER_EVENT);
    assertThat(response.nextExpectedBefore()).isEqualTo(Instant.parse("2026-06-15T10:01:00Z"));
  }

  @Test
  void shouldReturnTtlExpired_WhenCurrentTimePassesTheDeadline() {
    service.recordEvent(
        new EventRequest(
            "evt-1",
            "trace-6",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            "RULES_EVALUATED",
            60,
            false,
            Map.of()));

    TraceEventService laterService =
        new TraceEventService(
            traceRepository,
            eventRepository,
            Clock.fixed(Instant.parse("2026-06-15T10:02:01Z"), ZoneOffset.UTC));

    TraceStatusResponse response = laterService.getTraceStatus("trace-6");

    assertThat(response.status()).isEqualTo(TraceStatus.TTL_EXPIRED_FOR_EVENT);
  }

  @Test
  void shouldRejectDuplicateEventId() {
    service.recordEvent(
        new EventRequest(
            "evt-1",
            "trace-5",
            "APPLICATION_RECEIVED",
            "SUCCESS",
            Instant.parse("2026-06-15T10:00:00Z"),
            null,
            null,
            false,
            Map.of()));

    assertThatThrownBy(
            () ->
                service.recordEvent(
                    new EventRequest(
                        "evt-1",
                        "trace-5",
                        "RULES_EVALUATED",
                        "SUCCESS",
                        Instant.parse("2026-06-15T10:00:30Z"),
                        null,
                        null,
                        false,
                        Map.of())))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("already exists");
  }
}
