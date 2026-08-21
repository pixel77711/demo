package com.example.demo.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PrintCallbackRepository extends JpaRepository<PrintCallback, Long> {
    Optional<PrintCallback> findByJobId(String jobId);
}
