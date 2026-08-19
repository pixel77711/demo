package com.example.demo.webhook;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "webhook_events")
public class WebhookEvent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String eventType;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String payload;

    @Column(nullable = false)
    private String source;

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column(nullable = false)
    private String status; // RECEIVED, PROCESSING, SUCCESS, FAILED

    private String errorMessage;

    public WebhookEvent() {}

    public WebhookEvent(String eventType, String payload, String source) {
        this.eventType = eventType;
        this.payload = payload;
        this.source = source;
        this.receivedAt = LocalDateTime.now();
        this.status = "RECEIVED";
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
