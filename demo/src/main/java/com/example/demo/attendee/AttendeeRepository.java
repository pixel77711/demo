package com.example.demo.attendee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    Optional<Attendee> findByQrCode(String qrCode);
    
    List<Attendee> findByCheckInStatus(CheckInStatus status);
    
    // Query to find attendee by print job ID (used by webhook consumer)
    Optional<Attendee> findByPrintJobId(String printJobId);
    
    // Query to get all pending check-ins (useful for monitoring/debugging)
    @Query("SELECT a FROM Attendee a WHERE a.checkInStatus = 'PENDING' ORDER BY a.checkInInitiatedAt DESC")
    List<Attendee> findAllPending();
    
    // Query to get all failed check-ins
    @Query("SELECT a FROM Attendee a WHERE a.checkInStatus = 'FAILED' ORDER BY a.checkInInitiatedAt DESC")
    List<Attendee> findAllFailed();
}
