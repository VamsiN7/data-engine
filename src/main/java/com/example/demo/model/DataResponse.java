package com.example.demo.model;

import java.util.Map;

public class DataResponse {
    private Map<String, Object> data;

    public DataResponse(Map<String, Object> data) {
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }

    public void setData(Map<String, Object> data) {
        this.data = data;
    }
} 