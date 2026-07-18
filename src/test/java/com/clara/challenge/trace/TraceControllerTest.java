package com.clara.challenge.trace;

import com.clara.challenge.ApiExceptionHandler;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class TraceControllerTest {

  private MockMvc mockMvc;
  private TraceEventService traceEventService;

  @BeforeEach
  void setUp() {
    traceEventService = Mockito.mock(TraceEventService.class);
    mockMvc =
        MockMvcBuilders.standaloneSetup(new TraceController(traceEventService))
            .setControllerAdvice(new ApiExceptionHandler())
            .build();
  }

  @Test
  void shouldRecordEventAndReturnAcceptedStatus() throws Exception {
    TraceStatusResponse response =
        new TraceStatusResponse(
            "trace-1", TraceStatus.STARTED, "APPLICATION_RECEIVED", "SUCCESS", null, null, 1);

    when(traceEventService.recordEvent(any(EventRequest.class))).thenReturn(response);

    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "eventId": "evt-1",
                      "traceId": "trace-1",
                      "eventName": "APPLICATION_RECEIVED",
                      "result": "SUCCESS",
                      "occurredAt": "2026-06-15T10:00:00Z"
                    }
                    """))
        .andExpect(status().isAccepted())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.traceId").value("trace-1"))
        .andExpect(jsonPath("$.status").value("STARTED"))
        .andExpect(jsonPath("$.eventsReceived").value(1));
  }

  @Test
  void shouldReturnTraceStatus() throws Exception {
    TraceStatusResponse response =
        new TraceStatusResponse(
            "trace-2",
            TraceStatus.WAITING_OTHER_EVENT,
            "APPLICATION_RECEIVED",
            "SUCCESS",
            "RULES_EVALUATED",
            Instant.parse("2026-06-15T10:01:00Z"),
            1);

    when(traceEventService.getTraceStatus("trace-2")).thenReturn(response);

    mockMvc
        .perform(get("/traces/trace-2/status"))
        .andExpect(status().isOk())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.traceId").value("trace-2"))
        .andExpect(jsonPath("$.status").value("WAITING_OTHER_EVENT"))
        .andExpect(jsonPath("$.nextExpectedEvent").value("RULES_EVALUATED"));
  }

  @Test
  void shouldRejectInvalidEventRequest() throws Exception {
    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "eventId": "",
                      "traceId": "trace-3",
                      "eventName": "APPLICATION_RECEIVED",
                      "result": "INVALID",
                      "occurredAt": "2026-06-15T10:00:00Z"
                    }
                    """))
        .andExpect(status().isBadRequest())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
        .andExpect(jsonPath("$.path").value("/events"));
  }

  @Test
  void shouldReturnConflictForDuplicateEventId() throws Exception {
    doThrow(new IllegalArgumentException("Event ID already exists"))
        .when(traceEventService)
        .recordEvent(any(EventRequest.class));

    mockMvc
        .perform(
            post("/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(
                    """
                    {
                      "eventId": "evt-1",
                      "traceId": "trace-1",
                      "eventName": "APPLICATION_RECEIVED",
                      "result": "SUCCESS",
                      "occurredAt": "2026-06-15T10:00:00Z"
                    }
                    """))
        .andExpect(status().isConflict())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("DUPLICATE_EVENT_ID"))
        .andExpect(jsonPath("$.message").value("Event ID already exists"))
        .andExpect(jsonPath("$.path").value("/events"));
  }

  @Test
  void shouldReturnNotFoundForUnknownTrace() throws Exception {
    when(traceEventService.getTraceStatus("trace-missing"))
        .thenThrow(new IllegalArgumentException("Trace not found"));

    mockMvc
        .perform(get("/traces/trace-missing/status"))
        .andExpect(status().isNotFound())
        .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
        .andExpect(jsonPath("$.code").value("TRACE_NOT_FOUND"))
        .andExpect(jsonPath("$.message").value("Trace not found"))
        .andExpect(jsonPath("$.path").value("/traces/trace-missing/status"));
  }
}
