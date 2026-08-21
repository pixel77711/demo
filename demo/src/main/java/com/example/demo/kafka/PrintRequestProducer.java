package com.example.demo.kafka;

import com.example.demo.dto.PrintRequestDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Component
public class PrintRequestProducer {
    private static final Logger logger = LoggerFactory.getLogger(PrintRequestProducer.class);

    @Autowired
    private KafkaTemplate<String, PrintRequestDTO> kafkaTemplate;

    @Autowired
    private KafkaTopicConfig kafkaTopicConfig;

    /**
     * Publish a print request to the Kafka message queue
     * The vendor's print service will consume this and process asynchronously
     */
    public void publishPrintRequest(PrintRequestDTO printRequest) {
        try {
            logger.info("Publishing print request for attendee: {} with job ID: {}", 
                printRequest.getAttendeeQrCode(), printRequest.getJobId());
            
            kafkaTemplate.send(kafkaTopicConfig.getPrintRequest(), 
                printRequest.getJobId(), printRequest);
            
            logger.info("Print request published successfully");
        } catch (Exception e) {
            logger.error("Failed to publish print request: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to publish print request", e);
        }
    }
}
