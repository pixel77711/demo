# ARCHITECTURE.md - System Design Deep Dive

## Problem Statement

The Solstice event needed a badge printing system that:
1. **Handles high volume**: Thousands of attendees scanning QR codes
2. **Prevents duplicates**: No double-printing of badges
3. **Works asynchronously**: Doesn't block the UI while printing
4. **Integrates with vendor API**: Uses external badge printer service
5. **Provides audit trail**: Complete history of all operations

## Original (Synchronous) Approach

```
Attendee Scan → API Call → Vendor Prints → Wait → Return Status
└─────────────────────────────────────────────────────────────┘
                      All blocking!
```

**Problems:**
- Long timeouts (printing takes 30-60 seconds)
- Vendor API failures block entire system
- Hard to retry failed jobs
- Poor UX (user waits in front of scanner)

## New (Asynchronous) Approach

```
Attendee Scan → API Call → Publish to Queue → Return "PENDING" → Vendor Prints
                                                                    → Webhook Callback
                                                                    → Update DB
└────────────────────────────────────────────────────────────────────────────┘
All non-blocking!
```

**Benefits:**
- API returns immediately (great UX)
- Vendor API failures don't block system
- Kafka queue acts as reliability buffer
- Easy to retry or reprocess
- Scales to handle traffic spikes

## Key Design Decisions

### 1. Why Kafka?

**Event Queue Benefits:**
- **Decoupling**: Backend doesn't depend on vendor uptime
- **Buffering**: If vendor slow, messages queue up safely
- **Replay**: Can reprocess messages if needed
- **Scaling**: Consumers can be added to handle load
- **Reliability**: Kafka persists messages to disk

**Compared to alternatives:**
- RabbitMQ: Kafka better for high volume, stream processing
- Redis: Not persistent, not ideal for critical operations
- Direct API calls: Blocking, unreliable

### 2. Duplicate-Scan Protection

**Why it matters:**
- Two scanners at different gates scanning same QR code
- Network delays causing duplicate webhook callbacks
- Attendee accidentally scanning twice

**Implementation:**
```
Status Machine:
  PENDING ──(success callback)──→ CHECKED_IN (final)
    ↑                                  ↓
    |                            (ignore any more callbacks)
    |                                  ↓
    └──────(failure callback)────────FAILED
         (allow retry)
```

**Key protection:**
```java
if (attendee.getCheckInStatus() != CheckInStatus.PENDING) {
    logger.warn("Ignoring duplicate callback");
    return; // Don't process
}
```

This ensures:
- First scan initiates print
- Second scan before completion returns same job ID
- After completion, any scan is rejected
- If failed, can retry from FAILED state

### 3. Why Webhook + Kafka?

**Flow:**
```
Vendor → Webhook (sync) → DB (audit) → Kafka (async) → Consumer → Update DB
```

**Why not direct consumer?**
- Vendor needs immediate acknowledgment
- We need audit trail of exact callback received
- Consumer might be down during webhook delivery
- Kafka consumer can retry independently

### 4. Immediate Return from Webhook

```java
@PostMapping("/print-callback")
public ResponseEntity<?> receivePrintCallback(...) {
    // Store immediately
    printCallbackRepository.save(callback);
    
    // Return success to vendor immediately!
    return ResponseEntity.ok(...);
    
    // Actual processing happens async in Kafka consumer
}
```

**Why?**
- Vendor needs fast acknowledgment
- We're not blocking on processing
- If consumer slow, vendor doesn't wait
- Decouples vendor from our processing speed

## Concurrency Scenarios

### Scenario 1: Same Person Scans Twice (5 seconds apart)

```
T=0s  Attendee scans → Status: PENDING, Job: JOB-1, Print initiated
T=5s  Attendee scans again → Status still PENDING → Return same job
T=30s Webhook arrives → Status becomes CHECKED_IN
T=35s Attendee tries again → Status CHECKED_IN → Rejected
```

**Result:** One badge printed ✓

### Scenario 2: Duplicate Webhook Callbacks

```
T=0s  Print initiated
T=30s Webhook #1 arrives → Status: PENDING → Update to CHECKED_IN
T=31s Webhook #2 arrives (retry) → Status: CHECKED_IN → Ignored
```

**Result:** Status only updated once ✓

### Scenario 3: Failed Print Job

```
T=0s  Print initiated → Status: PENDING
T=30s Webhook arrives with FAILED → Status: FAILED
T=35s Attendee scans again → Status: FAILED → Allowed to retry
T=36s New print initiated → Status: PENDING, Job: JOB-2
```

**Result:** Can retry after failure ✓

## Database Consistency

### Transaction Boundaries

**Check-In Service:**
```java
@Transactional
public CheckInResponseDTO initiateCheckIn(String qrCode) {
    Attendee attendee = findByQrCode(qrCode);  // Atomic read
    attendee.setStatus(PENDING);                // Atomic write
    attendee.setPrintJobId(jobId);              // Same transaction
    attendeeRepository.save(attendee);          // Commit
    
    publishPrintRequest(jobId);                 // Separate operation
}
```

**Kafka Consumer:**
```java
@Transactional
public void handlePrintCallback(...) {
    // Atomically update attendee
    if (attendee.getStatus() == PENDING) {      // Check
        attendee.setStatus(CHECKED_IN);         // Update
        attendeeRepository.save(attendee);      // Commit
    }
}
```

### Why Two Transactions?
1. **Check-In**: Must be atomic (find + update = one transaction)
2. **Publish**: Can fail independently (message sent out of transaction)
3. **Consumer**: Must be atomic (check + update = one transaction)

**Benefit:** Even if Kafka publish fails, attendee is PENDING and can retry

## Failure Modes & Recovery

### Failure 1: Kafka Producer Fails
```
Attendee scan → Create PENDING record → Publish fails → Return PENDING status
↓
Result: Attendee waits forever (bad)
↓
Mitigation: Async error monitoring, webhook retry logic
```

### Failure 2: Vendor API Down
```
Message in Kafka queue → Consumer tries → Vendor unavailable → Retry later
↓
Result: Message stays in queue, retried when vendor recovers (good)
```

### Failure 3: Webhook Delivery Fails
```
Vendor retries → Webhook eventually succeeds → Callback processed
↓
Result: Database has audit trail, status updated (good)
```

### Failure 4: Database Connection Lost
```
Kafka consumer tries to update → DB unavailable → Retry on next message
↓
Result: Message stays in Kafka, retried when DB recovers (good)
```

## Performance Characteristics

### Latency
- **Check-in API response:** ~100ms (DB lookup + Kafka publish)
- **Badge printing:** ~30-60s (vendor dependent)
- **Total time to completion:** ~30-60s
- **User sees:** PENDING after 100ms, CHECKED_IN after 30-60s

### Throughput
- **Single API instance:** ~1000 check-ins/sec
- **Kafka broker:** ~100k messages/sec
- **Database:** ~10k updates/sec
- **Overall bottleneck:** Vendor printing speed

### Scalability
- **Add more API instances:** Horizontal scale with load balancer
- **Add more Kafka consumers:** Parallel callback processing
- **Add database replicas:** Read scaling for status checks

## Security Considerations

### 1. Webhook Authentication
```java
// TODO: Add HMAC signature verification
// Vendor signs callback with secret key
// We verify signature to ensure it's really from vendor

// TODO: Add rate limiting
// Prevent vendor from flooding us with callbacks
```

### 2. SQL Injection Protection
```java
// Using Spring JPA, not raw SQL queries
// Parameterized queries by default
attendeeRepository.findByQrCode(qrCode); // Safe
```

### 3. API Authentication
```java
// TODO: Add API key validation
// TODO: Add role-based access control
// TODO: Add request signing
```

## Monitoring & Observability

### Key Metrics to Track

1. **Check-in API**
   - Request rate
   - Response time
   - Error rate
   - Duplicate scan rate

2. **Kafka Topics**
   - Message lag
   - Consumer lag
   - Message throughput
   - Failed message rate

3. **Database**
   - Connection pool usage
   - Query execution time
   - Update rate by status

4. **Business Metrics**
   - PENDING → CHECKED_IN rate
   - PENDING → FAILED rate
   - Average print time
   - Failed print rate by vendor

## Testing Strategy

### Unit Tests
- CheckInService logic
- Duplicate-scan protection
- Status transitions

### Integration Tests
- Kafka producer/consumer
- Database transactions
- Webhook endpoint

### Load Tests
- 1000+ concurrent check-ins
- Kafka throughput limits
- Database connection pool

### Chaos Tests
- Vendor service failures
- Database connection loss
- Kafka broker down
- Network delays

## Evolution Path

**Phase 1 (Current):** Basic async architecture
- Simple webhook
- Kafka queue
- Duplicate protection

**Phase 2:** Enhanced reliability
- Webhook retry logic
- Message retry with backoff
- Vendor health checks

**Phase 3:** Real-time updates
- WebSocket for live status
- Admin dashboard
- Analytics

**Phase 4:** Multi-vendor
- Support multiple printers
- Load balancing across printers
- Printer failover
- Vendor performance comparison
