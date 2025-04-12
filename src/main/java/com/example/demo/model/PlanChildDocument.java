package com.example.demo.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.elasticsearch.core.join.JoinField;

import java.util.Map;

@Document(indexName = "plans")
public class PlanChildDocument {
    @Id
    private String objectId;

    @Field(type = FieldType.Object, name = "plan_service_relation")
    private JoinField<String> relation;

    @Field(type = FieldType.Text)
    private String objectType;
    
    @Field(type = FieldType.Text)
    private String _org;
    
    @Field(type = FieldType.Text)
    private String name;
    
    @Field(type = FieldType.Integer)
    private Integer deductible;

    @Field(type = FieldType.Integer)
    private Integer copay;
    
    @Field(type = FieldType.Text)
    private String serviceId;
    
    @Field(type = FieldType.Object)
    private Map<String, Object> additionalProperties;

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

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public Integer getDeductible() {
        return deductible;
    }

    public void setDeductible(Integer deductible) {
        this.deductible = deductible;
    }

    public Integer getCopay() {
        return copay;
    }

    public void setCopay(Integer copay) {
        this.copay = copay;
    }
    
    public String getServiceId() {
        return serviceId;
    }
    
    public void setServiceId(String serviceId) {
        this.serviceId = serviceId;
    }
    
    public Map<String, Object> getAdditionalProperties() {
        return additionalProperties;
    }
    
    public void setAdditionalProperties(Map<String, Object> additionalProperties) {
        this.additionalProperties = additionalProperties;
    }
}