# Async Webhook Migration - Badge Printing System

## Overview

This project demonstrates a modern asynchronous architecture for integrating with external badge printing services. The system uses event-driven design patterns with Apache Kafka to handle badge printing workflows reliably and efficiently.

## Architecture

### Components

```
┌─────────────────────────────────────────────────────────────────┐
│                      Frontend/Mobile App                         │
│              (Scans QR code to initiate check-in)               │
└──────────────────────┬──────────────────────────────────────────┘
                       │
                       ▼
        ┌──────────────────────────┐
        │  CheckInController       │
        │  /api/checkin/initiate   │
        └──────────┬───────────────┘
                   │
                   ▼
        ┌──────────────────────────┐
        │  CheckInService          │
        │  (Orchestrates flow)     │
        └──────────┬───────────────┘
                   │
                   ▼
        ┌──────────────────────────┐
        │ PrintRequestProducer     │
        │ (Publishes to Kafka)     │
        └──────────┬───────────────┘
                   │
                   ▼
        ┌──────────────────────────┐         ┌─────────────────┐
        │  Kafka Topic             │────────▶│  Vendor Service │
        │  print-requests          │         │  (Badge Printer)│
        └──────────────────────────┘         └────────┬────────┘
                                                      │
                                                      │ Prints badge
                                                      │
                                                      ▼
                                             ┌─────────────────┐
                                             │ Webhook Endpoint│
                                             │ /api/webhooks/  │
                                             │ print-callback  │
                                             └────────┬────────┘
                                                      │
                                                      ▼
        ┌──────────────────────────────┐    ┌─────────────────┐
        │ Kafka Topic                  │◀───│ DB Storage      │
        │ print-callbacks              │    │ (Audit Trail)   │
        └──────────┬───────────────────┘    └─────────────────┘
                   │
                   ▼
        ┌──────────────────────────┐
        │PrintCallbackConsumer     │
        │ (Kafka Consumer)         │
        └──────────┬───────────────┘
                   │
                   ▼
        ┌──────────────────────────┐
        │ Attendee Database        │
        │ (Update check-in status) │
        └──────────────────────────┘
```

## Key Design Patterns

### 1. **Asynchronous Processing**
- Badge printing is **not** blocking the check-in process
- Frontend receives immediate `PENDING` status
- Actual printing happens in background via Kafka consumer
- Frontend can poll `/api/checkin/status` to update UI

### 2. **Duplicate-Scan Protection**
- System prevents attendees from being checked in multiple times
- Implemented using status flags: `PENDING`, `CHECKED_IN`, `FAILED`
- Kafka consumer only updates if status is still `PENDING`
- If attendee already `CHECKED_IN`, subsequent scans are rejected

### 3. **Decoupled Architecture**
- Backend (Spring Boot) is decoupled from badge printer vendor
- No direct synchronous calls to vendor API
- Event-driven communication via Kafka message queue
- Easy to switch vendors without changing core logic

### 4. **Audit Trail**
- All print callbacks stored in database before processing
- Provides complete history of badge printing attempts
- Useful for debugging and compliance

## Workflow Sequence

```
1. INITIATE CHECK-IN
   Attendee scans QR code → Frontend calls POST /api/checkin/initiate
   ↓
2. LOOKUP ATTENDEE
   Backend queries database by QR code
   ↓
3. VALIDATE (Duplicate-Scan Protection)
   Check if attendee already CHECKED_IN
   ↓
4. CREATE PRINT JOB
   Generate unique Job ID: JOB-{UUID}
   Update attendee status to PENDING
   Save to database
   ↓
5. PUBLISH TO KAFKA
   Send PrintRequestDTO to Kafka queue
   Return PENDING response immediately to frontend
   ↓
6. VENDOR PROCESSES (Asynchronous)
   Vendor service consumes message from Kafka
   Prints badge
   Sends callback to webhook when done
   ↓
7. WEBHOOK RECEIVES CALLBACK
   POST /api/webhooks/print-callback receives result
   Store in database for audit trail
   Acknowledge to vendor immediately
   ↓
8. KAFKA CONSUMER PROCESSES
   Listen for callback in print-callbacks topic
   Find attendee by job ID
   Check duplicate-scan protection (must be PENDING)
   Update status to CHECKED_IN or FAILED
   ↓
9. FRONTEND UPDATES
   Poll /api/checkin/status periodically
   Display updated status to attendee
```

## Database Schema

### Attendee
```sql
CREATE TABLE attendee (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    qr_code VARCHAR(255) UNIQUE NOT NULL,
    name VARCHAR(255) NOT NULL,
    check_in_status ENUM('PENDING', 'CHECKED_IN', 'FAILED') DEFAULT 'PENDING',
    print_job_id VARCHAR(255),
    check_in_initiated_at TIMESTAMP,
    check_in_completed_at TIMESTAMP,
    error_message TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

### PrintCallback
```sql
CREATE TABLE print_callback (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    job_id VARCHAR(255) NOT NULL,
    status VARCHAR(50) NOT NULL,
    callback_payload LONGTEXT,
    error_details TEXT,
    received_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
```

## API Endpoints

### Check-In API

**POST /api/checkin/initiate**
```json
Request:
{
  "qr_code": "ABC123DEF456"
}

Response (200 OK):
{
  "attendee_id": 1,
  "qr_code": "ABC123DEF456",
  "name": "John Doe",
  "status": "PENDING",
  "message": "Check-in initiated. Badge is being printed. Please wait...",
  "timestamp": 1629820320000
}
```

**GET /api/checkin/status?qr_code=ABC123DEF456**
```json
Response (200 OK):
{
  "attendee_id": 1,
  "qr_code": "ABC123DEF456",
  "name": "John Doe",
  "status": "CHECKED_IN",
  "message": "Check-in successful! Badge has been printed.",
  "timestamp": 1629820320000
}
```

### Webhook API

**POST /api/webhooks/print-callback**
```json
Request (from vendor):
{
  "job_id": "JOB-550e8400-e29b-41d4-a716-446655440000",
  "status": "SUCCESS",
  "error_message": null,
  "timestamp": 1629820320000
}

Response (200 OK):
{
  "received": true,
  "job_id": "JOB-550e8400-e29b-41d4-a716-446655440000",
  "message": "Callback received and queued for processing"
}
```

## Configuration (application.properties)

```properties
# Database
spring.datasource.url=jdbc:mysql://localhost:3306/solstice_badges
spring.datasource.username=root
spring.datasource.password=password

# Kafka
spring.kafka.bootstrap-servers=localhost:9092
kafka.topics.print-request=print-requests
kafka.topics.print-callback=print-callbacks

# Server
server.port=8080
```

## Running the System

### Prerequisites
- Java 11+
- MySQL 8.0+
- Apache Kafka 2.8+
- Spring Boot 2.6+

### Steps

1. **Start MySQL**
   ```bash
   mysql -u root -p < schema.sql
   ```

2. **Start Kafka**
   ```bash
   bin/kafka-server-start.sh config/server.properties
   ```

3. **Start the Application**
   ```bash
   mvn spring-boot:run
   ```

4. **Test the API**
   ```bash
   # Initiate check-in
   curl -X POST "http://localhost:8080/api/checkin/initiate?qr_code=ABC123"
   
   # Check status
   curl -X GET "http://localhost:8080/api/checkin/status?qr_code=ABC123"
   
   # Simulate vendor callback
   curl -X POST "http://localhost:8080/api/webhooks/print-callback" \
     -H "Content-Type: application/json" \
     -d '{"job_id":"JOB-uuid","status":"SUCCESS"}'
   ```

## Error Handling

### Duplicate Scan Detection
If an attendee is scanned twice:
- First scan: Returns `PENDING`, initiates print job
- Second scan (before badge completes): Returns `PENDING` (same job)
- Third scan (after badge printed): Returns `CHECKED_IN` (rejected)

### Failed Print Jobs
If vendor reports failure:
- Webhook receives status: `FAILED`
- Attendee status updated to `FAILED`
- Error message stored for staff review
- Attendee can re-scan to retry

## Monitoring & Debugging

### Check Pending Check-Ins
```java
List<Attendee> pending = attendeeRepository.findAllPending();
```

### Check Failed Check-Ins
```java
List<Attendee> failed = attendeeRepository.findAllFailed();
```

### View Audit Trail
```sql
SELECT * FROM print_callback ORDER BY created_at DESC;
```

## Benefits of This Architecture

✅ **Scalability**: Can handle high check-in volume without blocking
✅ **Reliability**: Kafka ensures no messages are lost
✅ **Flexibility**: Easy to add new vendors or retry logic
✅ **Auditability**: Complete history of all operations
✅ **User Experience**: Immediate feedback while printing happens background
✅ **Duplicate Prevention**: Strong guarantees against double-printing

## Future Enhancements

- [ ] WebSocket support for real-time status updates
- [ ] Batch printing for efficiency
- [ ] Printer queue management and load balancing
- [ ] Retry logic with exponential backoff
- [ ] Multi-printer failover support
- [ ] Admin dashboard for monitoring
- [ ] Printer analytics and reporting

## Commit History

1. **Commit 1**: Initial project setup with Spring Boot, JPA, Kafka
2. **Commit 2**: Entity models (Attendee, CheckInStatus, PrintCallback)
3. **Commit 3**: Event classes and enums
4. **Commit 4**: JPA repositories and DTOs
5. **Commit 5**: Kafka producer for print requests
6. **Commit 6**: Kafka consumer with duplicate-scan protection
7. **Commit 7**: Check-in service orchestrating async workflow
8. **Commit 8**: REST controllers for check-in and webhook
9. **Commit 9**: Repository queries and application configuration
10. **Commit 10**: Documentation and architecture summary
