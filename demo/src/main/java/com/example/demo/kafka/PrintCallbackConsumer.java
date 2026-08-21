package com.example.demo.kafka;

import com.example.demo.dto.PrintCallbackDTO;
import com.example.demo.attendee.Attendee;
import com.example.demo.attendee.AttendeeRepository;
import com.example.demo.attendee.CheckInStatus;
import com.example.demo.webhook.PrintCallback;
import com.example.demo.webhook.PrintCallbackRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDateTime;
import java.util.Optional;

@Component
public class PrintCallbackConsumer {
    private static final Logger logger = LoggerFactory.getLogger(PrintCallbackConsumer.class);

    @Autowired
    private PrintCallbackRepository printCallbackRepository;

    @Autowired
    private AttendeeRepository attendeeRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Listen for print job completion callbacks from the vendor
     * Handles both SUCCESS and FAILED outcomes
     * Ensures duplicate-scan protection: only update if attendee hasn't been checked in yet
     */
    @KafkaListener(
        topics = "${kafka.topics.print-callback}",
        groupId = "solstice-badge-service"
    )
    public void handlePrintCallback(PrintCallbackDTO callback) {
        try {
            logger.info("Received print callback for job ID: {} with status: {}", 
                callback.getJobId(), callback.getStatus());

            // Store the callback in database for audit trail
            PrintCallback dbCallback = new PrintCallback(
                callback.getJobId(),
                callback.getStatus(),
                objectMapper.writeValueAsString(callback)
            );
            if (callback.getErrorMessage() != null) {
                dbCallback.setErrorDetails(callback.getErrorMessage());
            }
            printCallbackRepository.save(dbCallback);

            // Find attendee by print job ID
            // Note: In production, job ID should be linked to attendee during print request
            // For this implementation, we need to query attendees by their print job ID
            Attendee attendee = findAttendeeByPrintJobId(callback.getJobId());
            
            if (attendee == null) {
                logger.warn("No attendee found for print job ID: {}", callback.getJobId());
                return;
            }

            // DUPLICATE-SCAN PROTECTION: Only update if still PENDING
            if (attendee.getCheckInStatus() != CheckInStatus.PENDING) {
                logger.warn("Attendee {} already checked in. Ignoring duplicate callback for job ID: {}", 
                    attendee.getQrCode(), callback.getJobId());
                return;
            }

            // Process based on callback status
            if ("SUCCESS".equalsIgnoreCase(callback.getStatus())) {
                attendee.setCheckInStatus(CheckInStatus.CHECKED_IN);
                attendee.setCheckInCompletedAt(LocalDateTime.now());
                logger.info("Attendee {} successfully checked in", attendee.getQrCode());
            } else if ("FAILED".equalsIgnoreCase(callback.getStatus())) {
                attendee.setCheckInStatus(CheckInStatus.FAILED);
                attendee.setErrorMessage(callback.getErrorMessage());
                logger.error("Print job failed for attendee {}: {}", 
                    attendee.getQrCode(), callback.getErrorMessage());
            }

            attendeeRepository.save(attendee);
            logger.info("Attendee record updated successfully");

        } catch (Exception e) {
            logger.error("Error processing print callback: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to find attendee by print job ID
     * In production, consider adding a direct query method to repository
     */
    private Attendee findAttendeeByPrintJobId(String jobId) {
        // This is a placeholder - in real implementation, add:
        // Optional<Attendee> findByPrintJobId(String printJobId) to AttendeeRepository
        return null; // Will be implemented in next commit
    }
}
