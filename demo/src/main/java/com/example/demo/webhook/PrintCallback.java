package com.example.demo.webhook;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "print_callbacks")
public class PrintCallback {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String jobId; // Vendor's print job ID

    @Column(nullable = false)
    private String status; // SUCCESS, FAILED

    @Column(nullable = false)
    private String payload; // Full callback payload as JSON

    @Column(nullable = false)
    private LocalDateTime receivedAt;

    @Column
    private String errorDetails;

    public PrintCallback() {}

    public PrintCallback(String jobId, String status, String payload) {
        this.jobId = jobId;
        this.status = status;
        this.payload = payload;
        this.receivedAt = LocalDateTime.now();
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public LocalDateTime getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDateTime receivedAt) {
        this.receivedAt = receivedAt;
    }

    public String getErrorDetails() {
        return errorDetails;
    }

    public void setErrorDetails(String errorDetails) {
        this.errorDetails = errorDetails;
    }
}
