# AI_USAGE

## 1. AI tools used

- GitHub Copilot Chat (GPT-5.3-Codex)

## 2. Scope of AI assistance

AI was used to support:

- requirements analysis and assumption drafting
- SQL schema design and constraint/index suggestions
- Spring Boot service/controller/repository scaffolding
- test design and implementation
- Hurl test creation
- troubleshooting command/runtime issues
- documentation generation

## 3. Prompt examples used

### 3.1 Implementation prompt style

```text
Implement a Spring Boot service for distributed event watchdog with:
- POST /events
- GET /traces/{traceId}/status
Track statuses STARTED, WAITING_OTHER_EVENT, TTL_EXPIRED_FOR_EVENT, COMPLETED.
Persist current trace state and event history.
Use occurredAt + nextEventTtlSeconds for expected deadline.
Reject duplicate eventId and reject events for completed traces.
```

### 3.2 Unit test prompt standard (required by challenge)

```text
Define unit tests for the Event Watchdog state transition service.

Use the following standard:
- Test method names follow: shouldExpectedBehavior_WhenCondition.
- Each test validates one business rule only.
- Use Arrange / Act / Assert structure.
- Avoid testing Spring wiring unless needed.
- Focus on state transitions, TTL expiration, final event behavior,
  duplicate events, and completed-trace behavior.
- Do not add tests for undefined behavior unless explicitly documented
  as assumptions.
```

### 3.3 Hurl E2E prompt style

```text
Generate Hurl E2E scenarios for:
1) STARTED
2) WAITING_OTHER_EVENT
3) COMPLETED
4) TTL_EXPIRED_FOR_EVENT
Use public API only and assert status payload fields.
```

## 4. Human review and adjustments

The following items were manually reviewed/adjusted after AI generation:

- trace-state transition semantics and guard rules
- SQL constraints and indexes
- Java 25 environment handling in scripts
- local E2E fallback behavior and script compatibility
- final documentation wording and challenge checklist mapping

## 5. Verification policy followed

- No behavior was documented as successful without test artifact evidence.
- Unit/controller test evidence was taken from surefire reports.
- E2E status was reported with current runtime evidence and noted as pending when failing.

