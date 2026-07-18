POST {{base_url}}/events
Content-Type: application/json
```json
{
  "eventId": "hurl-started-evt-001",
  "traceId": "hurl-trace-started-001",
  "eventName": "APPLICATION_RECEIVED",
  "result": "SUCCESS",
  "occurredAt": "2026-06-15T10:00:00Z",
  "finalEvent": false,
  "metadata": {
    "country": "MX",
    "entityId": "company-123"
  }
}
```
HTTP 202
[Asserts]
jsonpath "$.traceId" == "hurl-trace-started-001"
jsonpath "$.status" == "STARTED"
jsonpath "$.eventsReceived" == 1

GET {{base_url}}/traces/hurl-trace-started-001/status
HTTP 200
[Asserts]
jsonpath "$.status" == "STARTED"
jsonpath "$.lastEventName" == "APPLICATION_RECEIVED"
jsonpath "$.eventsReceived" == 1
