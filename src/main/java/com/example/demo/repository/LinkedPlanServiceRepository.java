package com.example.demo.repository;

import com.example.demo.model.LinkedPlanServiceDocument;
import org.springframework.data.elasticsearch.annotations.Query;
import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinkedPlanServiceRepository extends ElasticsearchRepository<LinkedPlanServiceDocument, String> {
    
    @Query("{\"has_parent\": {\"parent_type\": \"plan\", \"query\": {\"term\": {\"objectId\": \"?0\"}}}}")
    List<LinkedPlanServiceDocument> findByPlanId(String planId);
}