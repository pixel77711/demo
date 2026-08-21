package com.example.demo.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

public class PrintCallbackDTO {
    @JsonProperty("job_id")
    private String jobId;

    @JsonProperty("status")
    private String status; // SUCCESS or FAILED

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("timestamp")
    private Long timestamp;

    public PrintCallbackDTO() {}

    public PrintCallbackDTO(String jobId, String status) {
        this.jobId = jobId;
        this.status = status;
        this.timestamp = System.currentTimeMillis();
    }

    // Getters and Setters
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

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Long timestamp) {
        this.timestamp = timestamp;
    }
}
