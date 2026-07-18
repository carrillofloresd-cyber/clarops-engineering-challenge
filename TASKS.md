# TASKS

## Task 1 - Clarify requirements and define assumptions

- Review challenge requirements and status model.
- Identify open questions and define explicit assumptions.
- Define acceptance criteria for API behavior and tests.

## Task 2 - Design data model and DDL

- Define `traces` table for current state.
- Define `trace_events` table for event history.
- Add constraints for status/result validity.
- Add indexes for trace lookup and event ordering.
- Implement SQL init script in `docker/init-scripts/db/01-init-schema.sql`.

## Task 3 - Implement event ingestion contract

- Create `EventRequest` DTO with validation.
- Implement `POST /events` endpoint.
- Add idempotency safeguard via unique `eventId` behavior.
- Persist event history and update trace state transactionally.

## Task 4 - Implement trace status query logic

- Implement `GET /traces/{traceId}/status` endpoint.
- Return full status payload (`TraceStatusResponse`).
- Implement lazy TTL expiration evaluation on status query.

## Task 5 - Implement business state transitions

- STARTED when no next expected event and not final.
- WAITING_OTHER_EVENT when next event + TTL is provided.
- COMPLETED when `finalEvent=true`.
- TTL_EXPIRED_FOR_EVENT when deadline passed at query time.

## Task 6 - Add unit tests for core logic

- Add service-level tests for required status transitions.
- Add tests for duplicate event rejection.
- Add tests for completed-trace immutability.
- Add tests for pre-deadline waiting behavior.

## Task 7 - Add API-level tests

- Add controller tests for `POST /events` accepted response.
- Add controller tests for `GET /traces/{traceId}/status` success response.
- Add controller test for validation failure (`400`).

## Task 8 - Create Hurl E2E suite

- Add E2E scenarios for STARTED, WAITING_OTHER_EVENT, COMPLETED, TTL_EXPIRED_FOR_EVENT.
- Add additional scenario for expected-event arrival advancing flow.
- Ensure scenarios use unique trace/event IDs.

## Task 9 - Create E2E execution scripts

- Add Docker-aware runner (`scripts/run-hurl-e2e.ps1`).
- Add local fallback runner (`scripts/run-hurl-e2e-local.ps1`).
- Add Java 25 pinning and Hurl path checks.

## Task 10 - Final documentation pack

- Rewrite `README.md` with checklist mapping, assumptions, decisions, trade-offs, run/test instructions, and examples.
- Provide AI usage audit in `AI_USAGE.md`.
- Capture testing evidence from surefire reports and E2E runner attempts.

