package com.example.demo.dto;

public class CheckInResponseDTO {
    private Long attendeeId;
    private String qrCode;
    private String name;
    private String status; // PENDING, CHECKED_IN, FAILED
    private String message;
    private Long timestamp;

    public CheckInResponseDTO() {}

    public CheckInResponseDTO(Long attendeeId, String qrCode, String name, String status, String message) {
        this.attendeeId = attendeeId;
        this.qrCode = qrCode;
        this.name = name;
        this.status = status;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public Long getAttendeeId() {
        return attendeeId;
    }

    public void setAttendeeId(Long attendeeId) {
        this.attendeeId = attendeeId;
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

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
