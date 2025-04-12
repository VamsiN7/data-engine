package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelation;
import org.springframework.data.elasticsearch.annotations.JoinTypeRelations;
import org.springframework.data.elasticsearch.core.join.JoinField;

@Document(indexName = "plans")
public class LinkedPlanServiceDocument {
    @Id
    private String objectId;

    @Field(type = FieldType.Object, name = "plan_service_relation")
    @JoinTypeRelations(relations = {
        @JoinTypeRelation(parent = "linkedPlanService", children = {"linkedService", "serviceCostShare"})
    })
    private JoinField<String> relation;

    @Field(type = FieldType.Text)
    private String objectType;
    
    @Field(type = FieldType.Text)
    private String _org;

    // Getters and Setters
    public String getObjectId() {
        return objectId;
    }

    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    public JoinField<String> getRelation() {
        return relation;
    }

    public void setRelation(JoinField<String> relation) {
        this.relation = relation;
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
}