package com.example.demo.repository;

import com.example.demo.model.PlanDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PlanElasticsearchRepository extends ElasticsearchRepository<PlanDocument, String> {
    
    List<PlanDocument> findByObjectType(String objectType);
    
    List<PlanDocument> findBy_org(String org);
    
    List<PlanDocument> findByPlanType(String planType);

    // Parent-child queries
    @Query("{\"bool\":{\"must\":{\"has_child\":{\"type\":\"service\",\"query\":{\"term\":{\"objectId\":\"?0\"}}}}}}")
    List<PlanDocument> findPlansByServiceId(String serviceId);
    
    @Query("{\"bool\":{\"must\":{\"has_child\":{\"type\":\"plancostShare\",\"query\":{\"range\":{\"copay\":{\"gte\":\"?0\"}}}}}}}")
    List<PlanDocument> findPlansByCostShareCopayGreaterThanEqual(Integer copay);
}