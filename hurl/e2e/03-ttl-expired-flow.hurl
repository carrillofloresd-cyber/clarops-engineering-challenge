POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-expired-evt-001",
  "traceId": "hurl-trace-expired-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2020-01-01T00:00:00Z",
  "nextExpectedEvent": "RULES_EVALUATED",
  "nextEventTtlSeconds": 5,
  "finalEvent": false,
  "metadata": {
    "country": "MX"
  }
}
```
HTTP 202
[Asserts]
jsonpath "$.status" == "WAITING_OTHER_EVENT"

GET {{base_url}}/traces/hurl-trace-expired-001/status
HTTP 200
[Asserts]
jsonpath "$.status" == "TTL_EXPIRED_FOR_EVENT"
jsonpath "$.nextExpectedEvent" == "RULES_EVALUATED"
