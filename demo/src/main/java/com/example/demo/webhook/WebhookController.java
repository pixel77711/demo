package com.example.demo.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/webhooks")
public class WebhookController {
    @Autowired
    private WebhookService webhookService;

    /**
     * POST /api/webhooks - Receive webhook events
     * Example payload:
     * {
     *   "event_type": "payment.completed",
     *   "source": "payment_gateway",
     *   "payload": {
     *     "transaction_id": "12345",
     *     "amount": 99.99,
     *     "currency": "USD"
     *   }
     * }
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> receiveWebhook(@RequestBody WebhookRequest request) {
        try {
            WebhookEvent event = webhookService.processWebhook(request);
            
            Map<String, Object> response = new HashMap<>();
            response.put("id", event.getId());
            response.put("status", "received");
            response.put("message", "Webhook processed successfully");
            response.put("event_type", event.getEventType());
            
            return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
        } catch (Exception e) {
            Map<String, Object> errorResponse = new HashMap<>();
            errorResponse.put("status", "error");
            errorResponse.put("message", "Failed to process webhook: " + e.getMessage());
            
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(errorResponse);
        }
    }

    /**
     * GET /api/webhooks - Retrieve all webhook events
     */
    @GetMapping
    public ResponseEntity<List<WebhookEvent>> getAllWebhooks() {
        List<WebhookEvent> events = webhookService.getAllEvents();
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/webhooks/{id} - Retrieve a specific webhook event
     */
    @GetMapping("/{id}")
    public ResponseEntity<WebhookEvent> getWebhookById(@PathVariable Long id) {
        WebhookEvent event = webhookService.getEventById(id);
        if (event != null) {
            return ResponseEntity.ok(event);
        }
        return ResponseEntity.notFound().build();
    }

    /**
     * GET /api/webhooks/events/by-status?status=SUCCESS - Filter by status
     */
    @GetMapping("/events/by-status")
    public ResponseEntity<List<WebhookEvent>> getWebhooksByStatus(@RequestParam String status) {
        List<WebhookEvent> events = webhookService.getEventsByStatus(status);
        return ResponseEntity.ok(events);
    }

    /**
     * GET /api/webhooks/events/by-type?type=payment.completed - Filter by event type
     */
    @GetMapping("/events/by-type")
    public ResponseEntity<List<WebhookEvent>> getWebhooksByType(@RequestParam String type) {
        List<WebhookEvent> events = webhookService.getEventsByType(type);
        return ResponseEntity.ok(events);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<Map<String, String>> health() {
        Map<String, String> response = new HashMap<>();
        response.put("status", "up");
        response.put("service", "webhook-service");
        return ResponseEntity.ok(response);
    }
}
