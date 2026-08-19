package com.example.demo.webhook;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;

@Service
public class WebhookService {
    @Autowired
    private WebhookEventRepository webhookEventRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Process an incoming webhook request
     */
    public WebhookEvent processWebhook(WebhookRequest request) {
        try {
            // Convert payload to JSON string
            String payloadJson = objectMapper.writeValueAsString(request.getPayload());

            // Create and save webhook event
            WebhookEvent event = new WebhookEvent(
                request.getEventType(),
                payloadJson,
                request.getSource()
            );

            // Business logic: process different event types
            switch (request.getEventType()) {
                case "payment.completed":
                    handlePaymentCompleted(event, payloadJson);
                    break;
                case "user.created":
                    handleUserCreated(event, payloadJson);
                    break;
                case "order.shipped":
                    handleOrderShipped(event, payloadJson);
                    break;
                default:
                    // Default handler for unknown event types
                    event.setStatus("SUCCESS");
                    break;
            }

            return webhookEventRepository.save(event);
        } catch (Exception e) {
            WebhookEvent event = new WebhookEvent(
                request.getEventType(),
                objectMapper.valueToTree(request.getPayload()).toString(),
                request.getSource()
            );
            event.setStatus("FAILED");
            event.setErrorMessage(e.getMessage());
            return webhookEventRepository.save(event);
        }
    }

    private void handlePaymentCompleted(WebhookEvent event, String payload) {
        // TODO: Implement payment completion logic
        System.out.println("Processing payment.completed: " + payload);
        event.setStatus("SUCCESS");
    }

    private void handleUserCreated(WebhookEvent event, String payload) {
        // TODO: Implement user creation logic
        System.out.println("Processing user.created: " + payload);
        event.setStatus("SUCCESS");
    }

    private void handleOrderShipped(WebhookEvent event, String payload) {
        // TODO: Implement order shipment logic
        System.out.println("Processing order.shipped: " + payload);
        event.setStatus("SUCCESS");
    }

    /**
     * Get all webhook events
     */
    public List<WebhookEvent> getAllEvents() {
        return webhookEventRepository.findAll();
    }

    /**
     * Get webhook events by status
     */
    public List<WebhookEvent> getEventsByStatus(String status) {
        return webhookEventRepository.findByStatus(status);
    }

    /**
     * Get webhook events by type
     */
    public List<WebhookEvent> getEventsByType(String eventType) {
        return webhookEventRepository.findByEventType(eventType);
    }

    /**
     * Get webhook event by ID
     */
    public WebhookEvent getEventById(Long id) {
        return webhookEventRepository.findById(id).orElse(null);
    }
}
