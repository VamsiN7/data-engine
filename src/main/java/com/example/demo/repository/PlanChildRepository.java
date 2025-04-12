package com.example.demo.repository;

import com.example.demo.model.PlanChildDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanChildRepository extends ElasticsearchRepository<PlanChildDocument, String> {
    
    @Query("{\"has_parent\": {\"parent_type\": \"plan\", \"query\": {\"term\": {\"objectId\": \"?0\"}}}}")
    List<PlanChildDocument> findChildrenByPlanId(String planId);
    
    @Query("{\"bool\": {\"must\": [{\"has_parent\": {\"parent_type\": \"plan\", \"query\": {\"term\": {\"objectId\": \"?0\"}}}}, {\"term\": {\"plan_service_relation.name\": \"?1\"}}]}}")
    List<PlanChildDocument> findChildrenByPlanIdAndRelationType(String planId, String relationType);
    
    List<PlanChildDocument> findByObjectType(String objectType);
    
    List<PlanChildDocument> findByName(String name);
    
    @Query("{\"bool\": {\"must\": [{\"term\": {\"plan_service_relation.name\": \"?0\"}}, {\"range\": {\"copay\": {\"gte\": \"?1\"}}}]}}")
    List<PlanChildDocument> findByRelationTypeAndCopayGreaterThanEqual(String relationType, int copay);
}