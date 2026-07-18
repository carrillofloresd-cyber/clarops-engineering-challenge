package com.clara.challenge.trace;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceEventRepository extends JpaRepository<TraceEventEntity, Long> {
  boolean existsByEventId(String eventId);

  List<TraceEventEntity> findByTraceIdOrderByOccurredAtAsc(String traceId);
}
