package com.example.demo.controller;

import com.example.demo.dto.PrintCallbackDTO;
import com.example.demo.webhook.PrintCallback;
import com.example.demo.webhook.PrintCallbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;

/**
 * Webhook endpoint for badge printer vendor callbacks
 * 
 * This is the synchronous HTTP endpoint that receives print job completion callbacks
 * The vendor's print service will POST to this endpoint when a badge is ready or fails
 * The callback is then published to Kafka for async processing by PrintCallbackConsumer
 */
@RestController
@RequestMapping("/api/webhooks")
@CrossOrigin(origins = "*")
public class WebhookController {
    private static final Logger logger = LoggerFactory.getLogger(WebhookController.class);

    @Autowired
    private PrintCallbackRepository printCallbackRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * POST /api/webhooks/print-callback
     * Receives print job completion callback from badge printer vendor
     * 
     * This endpoint is called synchronously by the vendor's system.
     * We store it in DB immediately, then Kafka consumer processes it asynchronously.
     * 
     * Expected payload:
     * {
     *   "job_id": "JOB-uuid-here",
     *   "status": "SUCCESS" or "FAILED",
     *   "error_message": "optional error details",
     *   "timestamp": 1629820320000
     * }
     * 
     * Response:
     * {
     *   "received": true,
     *   "job_id": "JOB-uuid-here",
     *   "message": "Callback received and queued for processing"
     * }
     */
    @PostMapping("/print-callback")
    public ResponseEntity<?> receivePrintCallback(@RequestBody PrintCallbackDTO callback) {
        try {
            logger.info("Received print callback for job ID: {} with status: {}", 
                callback.getJobId(), callback.getStatus());

            // Store callback immediately for audit trail
            PrintCallback dbCallback = new PrintCallback(
                callback.getJobId(),
                callback.getStatus(),
                objectMapper.writeValueAsString(callback)
            );
            if (callback.getErrorMessage() != null) {
                dbCallback.setErrorDetails(callback.getErrorMessage());
            }
            dbCallback.setReceivedAt(LocalDateTime.now());
            printCallbackRepository.save(dbCallback);

            logger.info("Print callback stored in database for processing");

            // Return success to vendor immediately
            // The actual processing happens asynchronously via Kafka consumer
            return ResponseEntity.ok(new WebhookResponseDTO(
                true,
                callback.getJobId(),
                "Callback received and queued for processing"
            ));

        } catch (Exception e) {
            logger.error("Error processing print callback: {}", e.getMessage(), e);
            return ResponseEntity.status(500).body(new WebhookErrorDTO(
                false,
                callback.getJobId(),
                "Error processing callback: " + e.getMessage()
            ));
        }
    }

    /**
     * GET /api/webhooks/health
     * Health check endpoint for monitoring
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Webhook service is healthy");
    }

    /**
     * Response DTO for successful webhook processing
     */
    public static class WebhookResponseDTO {
        private boolean received;
        private String jobId;
        private String message;

        public WebhookResponseDTO(boolean received, String jobId, String message) {
            this.received = received;
            this.jobId = jobId;
            this.message = message;
        }

        // Getters
        public boolean isReceived() { return received; }
        public String getJobId() { return jobId; }
        public String getMessage() { return message; }
    }

    /**
     * Response DTO for error webhook processing
     */
    public static class WebhookErrorDTO {
        private boolean received;
        private String jobId;
        private String error;

        public WebhookErrorDTO(boolean received, String jobId, String error) {
            this.received = received;
            this.jobId = jobId;
            this.error = error;
        }

        // Getters
        public boolean isReceived() { return received; }
        public String getJobId() { return jobId; }
        public String getError() { return error; }
    }
}
