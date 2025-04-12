package com.example.demo.service;

import com.example.demo.model.PlanDocument;
import com.example.demo.model.PlanChildDocument;
import com.example.demo.repository.PlanElasticsearchRepository;
import com.example.demo.repository.PlanChildRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private PlanElasticsearchRepository planRepository;

    @Autowired
    private PlanChildRepository childRepository;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void indexPlan(JSONObject planJson) {
        try {
            // 1. Create and save parent plan document
            PlanDocument planDocument = new PlanDocument();
            planDocument.setObjectId(planJson.getString("objectId"));
            planDocument.setObjectType(planJson.getString("objectType"));
            planDocument.set_org(planJson.getString("_org"));
            planDocument.setPlanType(planJson.getString("planType"));
            
            // Set the join field for parent - this is critical
            planDocument.setPlanRelation(new JoinField<>("plan"));
            
            // Parse the date string to LocalDate
            String dateStr = planJson.getString("creationDate");
            LocalDate creationDate = LocalDate.parse(dateStr, DateTimeFormatter.ofPattern("yyyy-MM-dd"));
            planDocument.setCreationDate(creationDate);
            
            // Save the plan document
            planRepository.save(planDocument);
            
            // 2. Create and save the plan cost share document
            JSONObject costShareJson = planJson.getJSONObject("planCostShares");
            PlanChildDocument costShareDoc = new PlanChildDocument();
            costShareDoc.setObjectId(costShareJson.getString("objectId"));
            costShareDoc.setObjectType(costShareJson.getString("objectType"));
            costShareDoc.set_org(costShareJson.getString("_org"));
            costShareDoc.setDeductible(costShareJson.getInt("deductible"));
            costShareDoc.setCopay(costShareJson.getInt("copay"));
            costShareDoc.setRelation(new JoinField<>("plancostShare", planDocument.getObjectId()));
            childRepository.save(costShareDoc);
            
            // 3. Process and save linked plan services
            JSONArray servicesArray = planJson.getJSONArray("linkedPlanServices");
            for (int i = 0; i < servicesArray.length(); i++) {
                JSONObject serviceJson = servicesArray.getJSONObject(i);
                JSONObject linkedService = serviceJson.getJSONObject("linkedService");
                JSONObject serviceCostShares = serviceJson.getJSONObject("planserviceCostShares");
                
                // Create and save service document
                PlanChildDocument serviceDoc = new PlanChildDocument();
                serviceDoc.setObjectId(serviceJson.getString("objectId"));
                serviceDoc.setObjectType(serviceJson.getString("objectType"));
                serviceDoc.set_org(serviceJson.getString("_org"));
                serviceDoc.setRelation(new JoinField<>("service", planDocument.getObjectId()));
                childRepository.save(serviceDoc);
                
                // Create and save linked service document
                PlanChildDocument linkedServiceDoc = new PlanChildDocument();
                linkedServiceDoc.setObjectId(linkedService.getString("objectId"));
                linkedServiceDoc.setObjectType(linkedService.getString("objectType"));
                linkedServiceDoc.set_org(linkedService.getString("_org"));
                linkedServiceDoc.setName(linkedService.getString("name"));
                linkedServiceDoc.setRelation(new JoinField<>("linkedService", planDocument.getObjectId()));
                childRepository.save(linkedServiceDoc);
                
                // Create and save service cost share document
                PlanChildDocument serviceCostShareDoc = new PlanChildDocument();
                serviceCostShareDoc.setObjectId(serviceCostShares.getString("objectId"));
                serviceCostShareDoc.setObjectType(serviceCostShares.getString("objectType"));
                serviceCostShareDoc.set_org(serviceCostShares.getString("_org"));
                serviceCostShareDoc.setDeductible(serviceCostShares.getInt("deductible"));
                serviceCostShareDoc.setCopay(serviceCostShares.getInt("copay"));
                serviceCostShareDoc.setServiceId(serviceJson.getString("objectId"));
                serviceCostShareDoc.setRelation(new JoinField<>("serviceCostShare", planDocument.getObjectId()));
                childRepository.save(serviceCostShareDoc);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index plan: " + e.getMessage(), e);
        }
    }

    public void deletePlan(String objectId) {
        try {
            // Delete the plan document
            planRepository.deleteById(objectId);
            
            // Delete all associated child documents
            List<PlanChildDocument> children = childRepository.findChildrenByPlanId(objectId);
            for (PlanChildDocument child : children) {
                childRepository.deleteById(child.getObjectId());
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to delete plan: " + e.getMessage(), e);
        }
    }

    public List<PlanDocument> searchByOrganization(String org) {
        return planRepository.findBy_org(org);
    }

    public List<PlanDocument> searchByPlanType(String planType) {
        return planRepository.findByPlanType(planType);
    }

    public List<PlanDocument> searchByServiceId(String serviceId) {
        return planRepository.findPlansByServiceId(serviceId);
    }
    
    public List<PlanDocument> searchPlansByCostShareCopayGreaterThanEqual(Integer copay) {
        return planRepository.findPlansByCostShareCopayGreaterThanEqual(copay);
    }
    
    public List<PlanChildDocument> searchChildrenByPlanId(String planId) {
        return childRepository.findChildrenByPlanId(planId);
    }
    
    public List<PlanChildDocument> searchChildrenByPlanIdAndRelationType(String planId, String relationType) {
        return childRepository.findChildrenByPlanIdAndRelationType(planId, relationType);
    }
}