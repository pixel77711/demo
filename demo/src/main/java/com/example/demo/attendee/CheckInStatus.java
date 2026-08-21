package com.example.demo.attendee;

public enum CheckInStatus {
    PENDING,      // Check-in initiated, waiting for print confirmation
    CHECKED_IN,   // Badge printed successfully
    FAILED        // Print job failed
}
