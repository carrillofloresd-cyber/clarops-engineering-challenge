package com.clara.challenge.trace;

import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TraceRepository extends JpaRepository<TraceEntity, Long> {
  Optional<TraceEntity> findByTraceId(String traceId);
}
