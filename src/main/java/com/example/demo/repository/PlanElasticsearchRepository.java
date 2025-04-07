package com.example.demo.repository;

import com.example.demo.model.PlanDocument;

import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

// @Repository
// public interface PlanElasticsearchRepository extends ElasticsearchRepository<PlanDocument, String> {
    
//     List<PlanDocument> findByObjectType(String objectType);
    
//     List<PlanDocument> findBy_org(String org);
    
//     List<PlanDocument> findByPlanType(String planType);
    
//     List<PlanDocument> findByLinkedPlanServices_ObjectId(String serviceId);
// } 
@Repository
public interface PlanElasticsearchRepository extends ElasticsearchRepository<PlanDocument, String> {
    
    List<PlanDocument> findByObjectType(String objectType);
    
    List<PlanDocument> findBy_org(String org);
    
    List<PlanDocument> findByPlanType(String planType);
    
    // Replace this method with a custom query
    @Query("{\"bool\": {\"must\": [{\"nested\": {\"path\": \"linkedPlanServices\", \"query\": {\"match\": {\"linkedPlanServices.objectId\": \"?0\"}}}}]}}")
    List<PlanDocument> findByLinkedPlanServices_ObjectId(String serviceId);
}