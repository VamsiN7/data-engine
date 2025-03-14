package com.example.demo.model;

import java.time.LocalDateTime;

public class ErrorResponse {
    private String detail;
    private String code;
    private LocalDateTime timestamp;

    public ErrorResponse(String detail, String code) {
        this.detail = detail;
        this.code = code;
        this.timestamp = LocalDateTime.now();
    }

    // Getters and setters
    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
} 