package com.example.demo.webhook;

import com.fasterxml.jackson.annotation.JsonProperty;

public class WebhookRequest {
    @JsonProperty("event_type")
    private String eventType;

    @JsonProperty("payload")
    private Object payload;

    @JsonProperty("source")
    private String source;

    public WebhookRequest() {}

    public WebhookRequest(String eventType, Object payload, String source) {
        this.eventType = eventType;
        this.payload = payload;
        this.source = source;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }
}
