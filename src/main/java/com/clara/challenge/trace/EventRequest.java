package com.clara.challenge.trace;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.Instant;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public record EventRequest(
    @NotBlank String eventId,
    @NotBlank String traceId,
    @NotBlank String eventName,
    @NotBlank @Pattern(regexp = "SUCCESS|ERROR") String result,
    @NotNull Instant occurredAt,
    String nextExpectedEvent,
    Integer nextEventTtlSeconds,
    Boolean finalEvent,
    Map<String, Object> metadata) {

  public boolean isFinalEvent() {
    return Boolean.TRUE.equals(finalEvent);
  }
}
