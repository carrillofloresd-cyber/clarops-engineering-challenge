# Clarops Distributed Event Watchdog - Submission

## 1. Problem Understanding

This project implements a lightweight event watchdog for distributed operational flows.
Each event belongs to a trace identified by `traceId`, and the service tracks the current flow status based on:

- incoming events via `POST /api/events`
- current trace state query via `GET /api/traces/{traceId}/status`
- TTL evaluation for expected next events

The implemented status model is:

- `STARTED`
- `WAITING_OTHER_EVENT`
- `TTL_EXPIRED_FOR_EVENT`
- `COMPLETED`

## 2. Submission Checklist (Mapped 1:1)

|                            Challenge Requirement                             | Status |                                                                 Evidence                                                                 |
|------------------------------------------------------------------------------|--------|------------------------------------------------------------------------------------------------------------------------------------------|
| Functional code                                                              | Done   | `src/main/java/com/clara/challenge/trace/`                                                                                               |
| Database schema DDL                                                          | Done   | `docker/init-scripts/db/01-init-schema.sql`                                                                                              |
| SQL init script in docker/init-scripts/db                                    | Done   | `docker/init-scripts/db/01-init-schema.sql`                                                                                              |
| README with understanding/assumptions/decisions/trade-offs/run/Hurl/examples | Done   | This document                                                                                                                            |
| `TASKS.md`                                                                   | Done   | `TASKS.md`                                                                                                                               |
| `AI_USAGE.md`                                                                | Done   | `AI_USAGE.md`                                                                                                                            |
| Unit tests                                                                   | Done   | `src/test/java/com/clara/challenge/trace/TraceEventServiceTest.java`, `src/test/java/com/clara/challenge/trace/TraceControllerTest.java` |
| Hurl E2E tests                                                               | Done   | `hurl/e2e/*.hurl`                                                                                                                        |
| All Hurl tests pass successfully                                             | Done   | `scripts/run-hurl-e2e-local.ps1` completed successfully with all scenarios passing                                                       |

## 3. Implemented API Contract

### 3.1 POST /api/events

Consumes one event and creates/updates trace state.

Example request:

```json
{
  "eventId": "evt-001",
  "traceId": "trace-123",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "country": "MX",
    "entityId": "company-123"
  }
}
```

Example response (202 Accepted):

```json
{
  "traceId": "trace-123",
  "status": "WAITING_OTHER_EVENT",
  "lastEventName": "APPLICATION_RECEIVED",
  "lastEventResult": "SUCCESS",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextExpectedBefore": "2026-06-15T10:02:00Z",
  "eventsReceived": 1
}
```

### 3.2 GET /api/traces/{traceId}/status

Returns current trace state and lazily evaluates TTL expiration.

Example response:

```json
{
  "traceId": "trace-123",
  "status": "TTL_EXPIRED_FOR_EVENT",
  "lastEventName": "APPLICATION_RECEIVED",
  "lastEventResult": "SUCCESS",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextExpectedBefore": "2026-06-15T10:02:00Z",
  "eventsReceived": 1
}
```

### 3.3 Error Contract Examples (400 / 404 / 409)

Validation error (400 Bad Request), example for invalid payload on `POST /api/events`:

```json
{
  "timestamp": "2026-07-17T19:34:08.700Z",
  "status": 400,
  "error": "Bad Request",
  "code": "VALIDATION_ERROR",
  "message": "eventId must not be blank; result must match \"SUCCESS|ERROR\"",
  "path": "/api/events"
}
```

Unknown trace (404 Not Found), example for `GET /api/traces/trace-missing/status`:

```json
{
  "timestamp": "2026-07-17T19:34:08.820Z",
  "status": 404,
  "error": "Not Found",
  "code": "TRACE_NOT_FOUND",
  "message": "Trace not found",
  "path": "/api/traces/trace-missing/status"
}
```

Duplicate event ID (409 Conflict), example for `POST /api/events`:

```json
{
  "timestamp": "2026-07-17T19:34:08.760Z",
  "status": 409,
  "error": "Conflict",
  "code": "DUPLICATE_EVENT_ID",
  "message": "Event ID already exists",
  "path": "/api/events"
}
```

## 4. Design Decisions and Trade-offs

### 4.1 Data model strategy

Decision:

- Store both current state and event history.

Tables:

- `clarops_challenge_schema.traces` stores current trace snapshot.
- `clarops_challenge_schema.trace_events` stores immutable event history.
- `clarops_challenge_schema.health` remains for setup validation endpoint.

Trade-off:

- Data duplication (latest fields in `traces`) improves query speed and simplifies status responses.

### 4.2 TTL strategy

Decision:

- `nextExpectedBefore = occurredAt + nextEventTtlSeconds`.

Trade-off:

- Uses event business time (not reception time), which is deterministic but can expire quickly if events arrive late.

### 4.3 Expiration evaluation strategy

Decision:

- No scheduler/background jobs.
- TTL expiration is computed when status is queried.

Trade-off:

- Simpler MVP and lower operational complexity.
- Expiration transitions happen lazily at read-time.

### 4.4 Consistency strategy

Decision:

- API-level validation for required fields and allowed `result` values.
- DB-level check constraints and unique constraints.
- Transactional service methods for trace/event updates.

Trade-off:

- Keeps logic simple but does not enforce strict expected-event-name transition matching.

## 5. Open Questions and Assumptions

1. Duplicate `eventId`:

- Rejected (`Event ID already exists`).

2. Event name different from currently expected event:

- Accepted in current implementation (no strict expected-name gate).

3. Expected event arrives after TTL expired:

- Accepted as a new event; state recalculated from latest event.

4. TTL origin:

- Calculated from `occurredAt`.

5. Completed trace receives more events:

- Rejected (`Completed trace does not accept new events`).

6. First event is also final event:

- Trace is immediately `COMPLETED`.

7. Event with `result=ERROR` and next expected event:

- Allowed; result does not directly drive trace status.

8. Metadata storage:

- Stored as JSON (`JSONB` in Postgres schema).

9. Inconsistent state prevention:

- Transaction boundaries + DB constraints + unique IDs.

10. Unknown trace query:

- Throws `IllegalArgumentException` (`Trace not found`) and is mapped to `404 Not Found` with the standard API error payload.

## 6. Data Model (DDL)

Defined in:

- `docker/init-scripts/db/01-init-schema.sql`

Includes:

- schema creation (`clarops_challenge_schema`)
- `traces` table with status/result constraints
- `trace_events` history table with FK to `traces`
- indexes for status and trace/event lookup

## 7. How to Run the Project

Primary setup instructions are in `SETUP.md`.

Quick run:

```bash
./mvnw spring-boot:run
```

Health check:

```bash
curl http://localhost:8080/api/health
```

Expected value:

```text
clarops sr engineer challenge
```

## 8. How to Run Tests

### 8.1 Unit and controller tests

```bash
./mvnw test
```

### 8.2 Hurl E2E tests

Scenario files:

- `hurl/e2e/01-started-flow.hurl`
- `hurl/e2e/02-waiting-flow.hurl`
- `hurl/e2e/03-ttl-expired-flow.hurl`
- `hurl/e2e/04-completed-flow.hurl`
- `hurl/e2e/05-expected-event-advances-flow.hurl`

Run with Docker-aware orchestration:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-hurl-e2e.ps1
```

Run local fallback (in-memory DB):

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-hurl-e2e-local.ps1
```

Run Hurl directly when app is already up:

```bash
"C:\Program Files\hurl\hurl.exe" --test --variable base_url=http://localhost:8080/api hurl/e2e/*.hurl
```

## 9. Testing Evidence

### 9.1 Unit/controller evidence (from surefire reports)

- `target/surefire-reports/com.clara.challenge.trace.TraceEventServiceTest.txt`
  - Tests run: 7, Failures: 0, Errors: 0
- `target/surefire-reports/com.clara.challenge.trace.TraceControllerTest.txt`
  - Tests run: 3, Failures: 0, Errors: 0
- `target/surefire-reports/com.clara.challenge.ClaropsChallengeApplicationTests.txt`
  - Tests run: 1, Failures: 0, Errors: 0

### 9.2 E2E evidence (current runtime)

- Deterministic local runner executed successfully via `scripts/run-hurl-e2e-local.ps1`.
- All scenario files passed:
  - `hurl/e2e/01-started-flow.hurl`
  - `hurl/e2e/02-waiting-flow.hurl`
  - `hurl/e2e/03-ttl-expired-flow.hurl`
  - `hurl/e2e/04-completed-flow.hurl`
  - `hurl/e2e/05-expected-event-advances-flow.hurl`
- Aggregate result from the runner output: 5 succeeded files, 0 failed files.
- Runner output includes final confirmation: `All Hurl E2E tests passed.`

### 9.3 Proof block (latest successful local run)

Command:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/run-hurl-e2e-local.ps1
```

Observed result excerpt:

```text
Succeeded files:   1 (100.0%)
Failed files:      0 (0.0%)
...
(repeated for all 5 scenario files)
...
All Hurl E2E tests passed.
```

Command:

```bash
./mvnw test
```

Observed result excerpt:

```text
Running mvnw test with Java 25...
TESTS_OK
```

## 10. Request/Response Examples by Required Status

### STARTED

Request:

```json
{
  "eventId": "evt-started-1",
  "traceId": "trace-started-1",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "finalEvent": false
}
```

Response:

```json
{
  "traceId": "trace-started-1",
  "status": "STARTED",
  "lastEventName": "APPLICATION_RECEIVED",
  "lastEventResult": "SUCCESS",
  "nextExpectedEvent": null,
  "nextExpectedBefore": null,
  "eventsReceived": 1
}
```

### WAITING_OTHER_EVENT

Request includes `nextExpectedEvent` and `nextEventTtlSeconds`.

Response includes:

- `status = WAITING_OTHER_EVENT`
- `nextExpectedEvent`
- `nextExpectedBefore`

### TTL_EXPIRED_FOR_EVENT

After deadline passes and status is queried:

- `status = TTL_EXPIRED_FOR_EVENT`

### COMPLETED

When `finalEvent=true` is posted:

- `status = COMPLETED`

## 11. Known Gaps / Next Improvements

- Enforce strict expected-event transition when domain requires it.
- Add CI pipeline stage that runs Hurl and publishes artifacts.

