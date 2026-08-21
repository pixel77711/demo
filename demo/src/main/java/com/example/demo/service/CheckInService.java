package com.example.demo.service;

import com.example.demo.attendee.Attendee;
import com.example.demo.attendee.AttendeeRepository;
import com.example.demo.attendee.CheckInStatus;
import com.example.demo.dto.CheckInResponseDTO;
import com.example.demo.dto.PrintRequestDTO;
import com.example.demo.kafka.PrintRequestProducer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Service
public class CheckInService {
    private static final Logger logger = LoggerFactory.getLogger(CheckInService.class);

    @Autowired
    private AttendeeRepository attendeeRepository;

    @Autowired
    private PrintRequestProducer printRequestProducer;

    /**
     * Initiate async check-in process for an attendee
     * 
     * Flow:
     * 1. Check if attendee exists by QR code
     * 2. Verify attendee hasn't already been checked in (duplicate-scan protection)
     * 3. Generate unique print job ID
     * 4. Publish print request to Kafka queue
     * 5. Return PENDING status immediately (UI shows pending state)
     * 6. Webhook callback will update status to CHECKED_IN or FAILED
     */
    public CheckInResponseDTO initiateCheckIn(String qrCode) {
        try {
            logger.info("Initiating check-in for QR code: {}", qrCode);

            // Step 1: Look up attendee by QR code
            Optional<Attendee> attendeeOpt = attendeeRepository.findByQrCode(qrCode);
            
            if (!attendeeOpt.isPresent()) {
                logger.warn("Attendee not found for QR code: {}", qrCode);
                return new CheckInResponseDTO(
                    null, qrCode, null, "FAILED", 
                    "Attendee not found. Please verify QR code."
                );
            }

            Attendee attendee = attendeeOpt.get();

            // Step 2: DUPLICATE-SCAN PROTECTION - Check if already checked in
            if (attendee.getCheckInStatus() == CheckInStatus.CHECKED_IN) {
                logger.warn("Attendee {} already checked in. Rejecting duplicate scan.", qrCode);
                return new CheckInResponseDTO(
                    attendee.getId(), qrCode, attendee.getName(), "CHECKED_IN",
                    "Attendee already checked in. Badge has been printed."
                );
            }

            // If status is PENDING or FAILED, allow re-attempt
            if (attendee.getCheckInStatus() == CheckInStatus.PENDING) {
                logger.info("Attendee {} check-in already pending. Returning current status.", qrCode);
                return new CheckInResponseDTO(
                    attendee.getId(), qrCode, attendee.getName(), "PENDING",
                    "Check-in in progress. Badge printer is processing your request."
                );
            }

            // Step 3: Generate unique print job ID
            String jobId = "JOB-" + UUID.randomUUID().toString();
            attendee.setPrintJobId(jobId);
            attendee.setCheckInStatus(CheckInStatus.PENDING);
            attendee.setCheckInInitiatedAt(LocalDateTime.now());
            attendeeRepository.save(attendee);

            // Step 4: Publish print request to Kafka queue
            PrintRequestDTO printRequest = new PrintRequestDTO(
                jobId,
                attendee.getQrCode(),
                attendee.getName()
            );
            printRequestProducer.publishPrintRequest(printRequest);

            logger.info("Print request published for attendee {} with job ID: {}", 
                qrCode, jobId);

            // Step 5: Return PENDING response immediately
            return new CheckInResponseDTO(
                attendee.getId(), qrCode, attendee.getName(), "PENDING",
                "Check-in initiated. Badge is being printed. Please wait..."
            );

        } catch (Exception e) {
            logger.error("Error during check-in process for QR code {}: {}", 
                qrCode, e.getMessage(), e);
            return new CheckInResponseDTO(
                null, qrCode, null, "FAILED",
                "An error occurred during check-in. Please try again."
            );
        }
    }

    /**
     * Get current check-in status for an attendee (for polling/status updates)
     */
    public CheckInResponseDTO getCheckInStatus(String qrCode) {
        Optional<Attendee> attendeeOpt = attendeeRepository.findByQrCode(qrCode);
        
        if (!attendeeOpt.isPresent()) {
            return new CheckInResponseDTO(
                null, qrCode, null, "FAILED", "Attendee not found."
            );
        }

        Attendee attendee = attendeeOpt.get();
        String status = attendee.getCheckInStatus().toString();
        String message = getStatusMessage(attendee.getCheckInStatus());

        return new CheckInResponseDTO(
            attendee.getId(), qrCode, attendee.getName(), status, message
        );
    }

    private String getStatusMessage(CheckInStatus status) {
        switch (status) {
            case PENDING:
                return "Check-in in progress. Badge is being printed.";
            case CHECKED_IN:
                return "Check-in successful! Badge has been printed.";
            case FAILED:
                return "Check-in failed. Please contact staff.";
            default:
                return "Unknown status.";
        }
    }
}
