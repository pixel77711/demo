package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrintRequestDTO {
    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("attendee_qr_code")
    private String attendeeQrCode;

    @JsonProperty("attendee_name")
    private String attendeeName;

    @JsonProperty("timestamp")
    private Long timestamp;

    public PrintRequestDTO() {}

    public PrintRequestDTO(String jobId, String attendeeQrCode, String attendeeName) {
        this.jobId = jobId;
        this.attendeeQrCode = attendeeQrCode;
        this.attendeeName = attendeeName;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
    public String getJobId() {
        return jobId;
    }

    public void setJobId(String jobId) {
        this.jobId = jobId;
    }

    public String getAttendeeQrCode() {
        return attendeeQrCode;
    }

    public void setAttendeeQrCode(String attendeeQrCode) {
        this.attendeeQrCode = attendeeQrCode;
    }

    public String getAttendee Name() {
        return attendeeName;
    }

    public void setAttendeeName(String attendeeName) {
        this.attendeeName = attendeeName;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
