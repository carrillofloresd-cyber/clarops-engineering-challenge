POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-completed-evt-001",
  "traceId": "hurl-trace-completed-001",
  "eventName": "DECISION_MADE",
  "result": "ERROR",
  "occurredAt": "2026-06-15T11:00:00Z",
  "finalEvent": true,
  "metadata": {
    "country": "MX",
    "reason": "manual-review-failed"
  }
}
```
HTTP 202
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.lastEventResult" == "ERROR"

GET {{base_url}}/traces/hurl-trace-completed-001/status
HTTP 200
[Asserts]
jsonpath "$.status" == "COMPLETED"
jsonpath "$.eventsReceived" == 1
