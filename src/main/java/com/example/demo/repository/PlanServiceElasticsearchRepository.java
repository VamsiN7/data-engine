package com.example.demo.repository;

import com.example.demo.model.PlanServiceDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanServiceElasticsearchRepository extends ElasticsearchRepository<PlanServiceDocument, String> {
    
    @Query("{\"has_parent\": {\"parent_type\": \"plan\", \"query\": {\"term\": {\"objectId\": \"?0\"}}}}")
    List<PlanServiceDocument> findServicesByPlanId(String planId);
    
    List<PlanServiceDocument> findByObjectType(String objectType);
    
    List<PlanServiceDocument> findByName(String name);
} 