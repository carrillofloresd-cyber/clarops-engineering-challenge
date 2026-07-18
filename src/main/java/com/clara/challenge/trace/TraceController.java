package com.clara.challenge.trace;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping
public class TraceController {

  private final TraceEventService service;

  public TraceController(TraceEventService service) {
    this.service = service;
  }

  @PostMapping("/events")
  public ResponseEntity<TraceStatusResponse> recordEvent(@Valid @RequestBody EventRequest request) {
    return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.recordEvent(request));
  }

  @GetMapping("/traces/{traceId}/status")
  public ResponseEntity<TraceStatusResponse> getStatus(@PathVariable String traceId) {
    return ResponseEntity.ok(service.getTraceStatus(traceId));
  }
}
