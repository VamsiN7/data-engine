package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.join.JoinField;

import java.util.Map;

@Document(indexName = "plans")
public class PlanServiceDocument {
    @Id
    private String objectId;

    @Field(type = FieldType.Object, name = "plan_service_relation")
    private JoinField<String> serviceRelation;

    @Field(type = FieldType.Text)
    private String objectType;

    @Field(type = FieldType.Text)
    private String name;

    @Field(type = FieldType.Object)
    private Map<String, Object> serviceCostShares;

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public JoinField<String> getServiceRelation() {
        return serviceRelation;
    }

    public void setServiceRelation(JoinField<String> serviceRelation) {
        this.serviceRelation = serviceRelation;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getServiceCostShares() {
        return serviceCostShares;
    }

    public void setServiceCostShares(Map<String, Object> serviceCostShares) {
        this.serviceCostShares = serviceCostShares;
    }
}