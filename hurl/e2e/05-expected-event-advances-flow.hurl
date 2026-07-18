POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-advance-evt-001",
  "traceId": "hurl-trace-advance-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 120,
  "finalEvent": false,
  "metadata": {
    "country": "MX"
  }
}
```
HTTP 202
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"

POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-advance-evt-002",
  "traceId": "hurl-trace-advance-001",
  "eventName": "RULES_EVALUATED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:01:00Z",
  "finalEvent": false,
  "metadata": {
    "country": "MX"
  }
}
```
HTTP 202
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "RULES_EVALUATED"
jsonpath "$.eventsReceived" == 2

GET {{base_url}}/traces/hurl-trace-advance-001/status
HTTP 200
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.eventsReceived" == 2
