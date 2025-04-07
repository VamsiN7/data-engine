package com.example.demo.service;

import com.example.demo.model.PlanDocument;
import com.example.demo.repository.PlanElasticsearchRepository;
import org.json.JSONArray;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
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

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public void indexPlan(JSONObject planJson) {
        try {
            PlanDocument planDocument = new PlanDocument();
            planDocument.setObjectId(planJson.getString("objectId"));
            planDocument.setObjectType(planJson.getString("objectType"));
            planDocument.set_org(planJson.getString("_org"));
            planDocument.setPlanType(planJson.getString("planType"));
            
            // Parse the date string to LocalDate
            String dateStr = planJson.getString("creationDate");
            LocalDate creationDate = LocalDate.parse(dateStr, DATE_FORMATTER);
            planDocument.setCreationDate(creationDate);
            
            planDocument.setPlanCostShares(planJson.getJSONObject("planCostShares").toMap());
            
            // Convert JSONArray to List<Map<String, Object>>
            JSONArray servicesArray = planJson.getJSONArray("linkedPlanServices");
            List<Map<String, Object>> servicesList = new ArrayList<>();
            for (int i = 0; i < servicesArray.length(); i++) {
                servicesList.add(servicesArray.getJSONObject(i).toMap());
            }
            planDocument.setLinkedPlanServices(servicesList);
            
            planRepository.save(planDocument);
        } catch (Exception e) {
            throw new RuntimeException("Failed to index plan: " + e.getMessage(), e);
        }
    }

    public void deletePlan(String objectId) {
        planRepository.deleteById(objectId);
    }

    public List<PlanDocument> searchByOrganization(String org) {
        return planRepository.findBy_org(org);
    }

    public List<PlanDocument> searchByPlanType(String planType) {
        return planRepository.findByPlanType(planType);
    }

    public List<PlanDocument> searchByServiceId(String serviceId) {
        return planRepository.findByLinkedPlanServices_ObjectId(serviceId);
    }
}