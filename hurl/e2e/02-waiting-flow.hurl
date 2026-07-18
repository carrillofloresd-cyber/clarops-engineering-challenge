POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-waiting-evt-001",
  "traceId": "hurl-trace-waiting-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2099-06-15T10:00:00Z",
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
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.nextExpectedBefore" == "2099-06-15T10:02:00Z"

GET {{base_url}}/traces/hurl-trace-waiting-001/status
HTTP 200
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
jsonpath "$.eventsReceived" == 1
