package com.clara.challenge.trace;

import java.time.Instant;

public record TraceStatusResponse(
    String traceId,
    TraceStatus status,
    String lastEventName,
    String lastEventResult,
    String nextExpectedEvent,
    Instant nextExpectedBefore,
    Integer eventsReceived) {}
