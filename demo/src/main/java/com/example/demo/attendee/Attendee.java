package com.example.demo.attendee;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "attendees")
public class Attendee {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String qrCode;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CheckInStatus checkInStatus; // PENDING, CHECKED_IN, FAILED

    @Column
    private String printJobId; // Reference to vendor's print job

    @Column
    private LocalDateTime checkInInitiatedAt;

    @Column
    private LocalDateTime checkInCompletedAt;

    @Column
    private String errorMessage;

    public Attendee() {}

    public Attendee(String qrCode, String name, String email) {
        this.qrCode = qrCode;
        this.name = name;
        this.email = email;
        this.checkInStatus = CheckInStatus.PENDING;
    }

    // Getters and Setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getQrCode() {
        return qrCode;
    }

    public void setQrCode(String qrCode) {
        this.qrCode = qrCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public CheckInStatus getCheckInStatus() {
        return checkInStatus;
    }

    public void setCheckInStatus(CheckInStatus checkInStatus) {
        this.checkInStatus = checkInStatus;
    }

    public String getPrintJobId() {
        return printJobId;
    }

    public void setPrintJobId(String printJobId) {
        this.printJobId = printJobId;
    }

    public LocalDateTime getCheckInInitiatedAt() {
        return checkInInitiatedAt;
    }

    public void setCheckInInitiatedAt(LocalDateTime checkInInitiatedAt) {
        this.checkInInitiatedAt = checkInInitiatedAt;
    }

    public LocalDateTime getCheckInCompletedAt() {
        return checkInCompletedAt;
    }

    public void setCheckInCompletedAt(LocalDateTime checkInCompletedAt) {
        this.checkInCompletedAt = checkInCompletedAt;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
