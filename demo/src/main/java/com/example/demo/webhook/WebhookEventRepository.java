package com.example.demo.webhook;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface WebhookEventRepository extends JpaRepository<WebhookEvent, Long> {
    List<WebhookEvent> findByStatus(String status);
    List<WebhookEvent> findByEventType(String eventType);
    List<WebhookEvent> findBySource(String source);
}
