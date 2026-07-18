package com.clara.challenge.trace;

import java.time.Clock;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TraceEventService {

  private final TraceRepository traceRepository;
  private final TraceEventRepository eventRepository;
  private final Clock clock;

  public TraceEventService(
      TraceRepository traceRepository, TraceEventRepository eventRepository, Clock clock) {
    this.traceRepository = traceRepository;
    this.eventRepository = eventRepository;
    this.clock = clock;
  }

  @Transactional
  public TraceStatusResponse recordEvent(EventRequest request) {
    if (eventRepository.existsByEventId(request.eventId())) {
      throw new IllegalArgumentException("Event ID already exists");
    }

    Instant now = clock.instant();
    TraceEntity trace =
        traceRepository
            .findByTraceId(request.traceId())
            .orElseGet(() -> createTrace(request.traceId(), now));

    if (trace.getStatus() != null && trace.getStatus().equals(TraceStatus.COMPLETED.name())) {
      throw new IllegalArgumentException("Completed trace does not accept new events");
    }

    if (request.isFinalEvent()) {
      trace.setStatus(TraceStatus.COMPLETED.name());
      trace.setCompletedAt(now);
    } else if (request.nextExpectedEvent() != null && request.nextEventTtlSeconds() != null) {
      trace.setStatus(TraceStatus.WAITING_OTHER_EVENT.name());
      trace.setNextExpectedEvent(request.nextExpectedEvent());
      trace.setNextExpectedBefore(request.occurredAt().plusSeconds(request.nextEventTtlSeconds()));
    } else {
      trace.setStatus(TraceStatus.STARTED.name());
      trace.setNextExpectedEvent(null);
      trace.setNextExpectedBefore(null);
    }

    trace.setLastEventName(request.eventName());
    trace.setLastEventResult(request.result());
    trace.setEventsReceived(trace.getEventsReceived() + 1);
    trace.setUpdatedAt(now);
    traceRepository.save(trace);

    TraceEventEntity eventEntity = new TraceEventEntity();
    eventEntity.setTraceId(request.traceId());
    eventEntity.setEventId(request.eventId());
    eventEntity.setEventName(request.eventName());
    eventEntity.setResult(request.result());
    eventEntity.setOccurredAt(request.occurredAt());
    eventEntity.setNextExpectedEvent(request.nextExpectedEvent());
    eventEntity.setNextEventTtlSeconds(request.nextEventTtlSeconds());
    eventEntity.setFinalEvent(request.isFinalEvent());
    eventEntity.setMetadata(request.metadata() == null ? "{}" : request.metadata().toString());
    eventEntity.setCreatedAt(now);
    eventRepository.save(eventEntity);

    return toResponse(trace);
  }

  @Transactional(readOnly = true)
  public TraceStatusResponse getTraceStatus(String traceId) {
    TraceEntity trace =
        traceRepository
            .findByTraceId(traceId)
            .orElseThrow(() -> new IllegalArgumentException("Trace not found"));
    Instant now = clock.instant();

    if (TraceStatus.COMPLETED.name().equals(trace.getStatus())) {
      return toResponse(trace);
    }

    if (trace.getNextExpectedEvent() != null
        && trace.getNextExpectedBefore() != null
        && now.isAfter(trace.getNextExpectedBefore())) {
      trace.setStatus(TraceStatus.TTL_EXPIRED_FOR_EVENT.name());
      trace.setUpdatedAt(now);
      traceRepository.save(trace);
    }

    return toResponse(trace);
  }

  private TraceEntity createTrace(String traceId, Instant now) {
    TraceEntity entity = new TraceEntity();
    entity.setTraceId(traceId);
    entity.setStatus(TraceStatus.STARTED.name());
    entity.setEventsReceived(0);
    entity.setCreatedAt(now);
    entity.setUpdatedAt(now);
    return entity;
  }

  private TraceStatusResponse toResponse(TraceEntity trace) {
    TraceStatus status = TraceStatus.valueOf(trace.getStatus());
    return new TraceStatusResponse(
        trace.getTraceId(),
        status,
        trace.getLastEventName(),
        trace.getLastEventResult(),
        trace.getNextExpectedEvent(),
        trace.getNextExpectedBefore(),
        trace.getEventsReceived());
  }
}
