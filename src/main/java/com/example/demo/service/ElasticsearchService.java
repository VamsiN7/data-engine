package com.example.demo.service;

import com.example.demo.model.PlanDocument;
import com.example.demo.model.PlanServiceDocument;
import com.example.demo.repository.PlanElasticsearchRepository;
import com.example.demo.repository.PlanServiceElasticsearchRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.elasticsearch.core.join.JoinField;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
public class ElasticsearchService {

    @Autowired
    private PlanElasticsearchRepository planRepository;

    @Autowired
    private PlanServiceElasticsearchRepository serviceRepository;

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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            LocalDate creationDate = LocalDate.parse(dateStr, formatter);
            planDocument.setCreationDate(creationDate);
            
            planDocument.setPlanCostShares(planJson.getJSONObject("planCostShares").toMap());
            
            // Save the plan document
            planRepository.save(planDocument);
            
            // 2. Process and save service documents
            JSONArray servicesArray = planJson.getJSONArray("linkedPlanServices");
            for (int i = 0; i < servicesArray.length(); i++) {
                JSONObject serviceJson = servicesArray.getJSONObject(i);
                JSONObject linkedService = serviceJson.getJSONObject("linkedService");
                
                PlanServiceDocument serviceDocument = new PlanServiceDocument();
                serviceDocument.setObjectId(serviceJson.getString("objectId"));
                serviceDocument.setObjectType(serviceJson.getString("objectType"));
                serviceDocument.setName(linkedService.getString("name")); // Get name from linkedService
                
                // Set service cost shares
                serviceDocument.setServiceCostShares(
                    serviceJson.getJSONObject("planserviceCostShares").toMap()
                );
                
                serviceDocument.setServiceRelation(new JoinField<>("service", planDocument.getObjectId()));
                
                serviceRepository.save(serviceDocument);
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to index plan: " + e.getMessage(), e);
        }
    }

    public void deletePlan(String objectId) {
        try {
            // Delete the plan document
            planRepository.deleteById(objectId);
            
            // Delete all associated service documents
            List<PlanServiceDocument> services = serviceRepository.findServicesByPlanId(objectId);
            for (PlanServiceDocument service : services) {
                serviceRepository.deleteById(service.getObjectId());
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
}