package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelation;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelations;
import org.springframework.data.elasticsearch.core.join.JoinField;

import java.time.LocalDate;
import java.util.Map;

@Document(indexName = "plans")
public class PlanDocument {

    @Id
    private String objectId;

    @Field(type = FieldType.Object, name = "plan_service_relation")
    @JoinTypeRelations(relations = {
        @JoinTypeRelation(parent = "plan", children = {"service"})
    })
    private JoinField<String> planRelation = new JoinField<>("plan");

    @Field(type = FieldType.Text)
    private String objectType;

    @Field(type = FieldType.Text)
    private String _org;

    @Field(type = FieldType.Text)
    private String planType;

    @Field(type = FieldType.Date, format = {}, pattern = "yyyy-MM-dd")
    private LocalDate creationDate;

    @Field(type = FieldType.Object)
    private Map<String, Object> planCostShares;

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public JoinField<String> getPlanRelation() {
        return planRelation;
    }

    public void setPlanRelation(JoinField<String> planRelation) {
        this.planRelation = planRelation;
    }

    public String getObjectType() {
        return objectType;
    }

    public void setObjectType(String objectType) {
        this.objectType = objectType;
    }

    public String get_org() {
        return _org;
    }

    public void set_org(String _org) {
        this._org = _org;
    }

    public String getPlanType() {
        return planType;
    }

    public void setPlanType(String planType) {
        this.planType = planType;
    }

    public LocalDate getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDate creationDate) {
        this.creationDate = creationDate;
    }

    public Map<String, Object> getPlanCostShares() {
        return planCostShares;
    }

    public void setPlanCostShares(Map<String, Object> planCostShares) {
        this.planCostShares = planCostShares;
    }
}