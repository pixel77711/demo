package com.example.demo.attendee;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;

@Repository
public interface AttendeeRepository extends JpaRepository<Attendee, Long> {
    Optional<Attendee> findByQrCode(String qrCode);
    List<Attendee> findByCheckInStatus(CheckInStatus status);
}
