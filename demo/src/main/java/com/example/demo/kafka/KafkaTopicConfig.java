package com.example.demo.kafka;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "kafka.topics")
public class KafkaTopicConfig {
    private String printRequest;
    private String printCallback;

    public String getPrintRequest() {
        return printRequest;
    }

    public void setPrintRequest(String printRequest) {
        this.printRequest = printRequest;
    }

    public String getPrintCallback() {
        return printCallback;
    }

    public void setPrintCallback(String printCallback) {
        this.printCallback = printCallback;
    }
}
