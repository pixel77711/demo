package com.example.demo.controller;

import com.example.demo.dto.CheckInResponseDTO;
import com.example.demo.service.CheckInService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/checkin")
@CrossOrigin(origins = "*")
public class CheckInController {
    private static final Logger logger = LoggerFactory.getLogger(CheckInController.class);

    @Autowired
    private CheckInService checkInService;

    /**
     * POST /api/checkin/initiate
     * Initiates async check-in for an attendee by QR code
     * 
     * Request: { "qr_code": "ABC123" }
     * Response: { "attendee_id": 1, "status": "PENDING", "message": "...", "timestamp": 123456 }
     * 
     * Returns PENDING immediately while badge print happens asynchronously
     */
    @PostMapping("/initiate")
    public ResponseEntity<CheckInResponseDTO> initiateCheckIn(
            @RequestParam(name = "qr_code") String qrCode) {
        
        logger.info("Received check-in request for QR code: {}", qrCode);
        
        CheckInResponseDTO response = checkInService.initiateCheckIn(qrCode);
        
        // Return 200 OK even for PENDING - it's not an error state
        return ResponseEntity.ok(response);
    }

    /**
     * GET /api/checkin/status
     * Poll current check-in status for an attendee
     * Useful for UI to update from PENDING -> CHECKED_IN or FAILED
     * 
     * Request: GET /api/checkin/status?qr_code=ABC123
     * Response: { "attendee_id": 1, "status": "CHECKED_IN", "message": "...", "timestamp": 123456 }
     */
    @GetMapping("/status")
    public ResponseEntity<CheckInResponseDTO> getCheckInStatus(
            @RequestParam(name = "qr_code") String qrCode) {
        
        logger.info("Received status request for QR code: {}", qrCode);
        
        CheckInResponseDTO response = checkInService.getCheckInStatus(qrCode);
        
        return ResponseEntity.ok(response);
    }

    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<String> health() {
        return ResponseEntity.ok("Check-in service is healthy");
    }
}
