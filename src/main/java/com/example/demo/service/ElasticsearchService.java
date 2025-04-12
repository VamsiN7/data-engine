package com.example.demo.service;

import com.example.demo.model.PlanDocument;
import com.example.demo.model.LinkedPlanServiceDocument;
import com.example.demo.model.PlanChildDocument;
import com.example.demo.repository.PlanElasticsearchRepository;
import com.example.demo.repository.LinkedPlanServiceRepository;
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

    @Autowired
    private LinkedPlanServiceRepository linkedPlanServiceRepository;

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

            // 2. Create and save plan cost shares as direct child of plan
            JSONObject costShareJson = planJson.getJSONObject("planCostShares");
            PlanChildDocument costShareDoc = new PlanChildDocument();
            costShareDoc.setObjectId(costShareJson.getString("objectId"));
            costShareDoc.setObjectType(costShareJson.getString("objectType"));
            costShareDoc.set_org(costShareJson.getString("_org"));
            costShareDoc.setDeductible(costShareJson.getInt("deductible"));
            costShareDoc.setCopay(costShareJson.getInt("copay"));
            costShareDoc.setRelation(new JoinField<>("plancostShare", planDocument.getObjectId()));
            childRepository.save(costShareDoc);

            // 3. Process linked plan services - now mid-level parents
            JSONArray servicesArray = planJson.getJSONArray("linkedPlanServices");
            for (int i = 0; i < servicesArray.length(); i++) {
                JSONObject serviceJson = servicesArray.getJSONObject(i);
                JSONObject linkedServiceJson = serviceJson.getJSONObject("linkedService");
                JSONObject serviceCostSharesJson = serviceJson.getJSONObject("planserviceCostShares");

                // 3a. Create and save linkedPlanService as child of plan but parent of other
                // docs
                LinkedPlanServiceDocument linkedPlanService = new LinkedPlanServiceDocument();
                linkedPlanService.setObjectId(serviceJson.getString("objectId"));
                linkedPlanService.setObjectType(serviceJson.getString("objectType"));
                linkedPlanService.set_org(serviceJson.getString("_org"));

                // Set relation as child of plan
                linkedPlanService.setRelation(new JoinField<>("linkedPlanService", planDocument.getObjectId()));
                linkedPlanServiceRepository.save(linkedPlanService);

                // 3b. Create linked service as child of linkedPlanService
                PlanChildDocument linkedService = new PlanChildDocument();
                linkedService.setObjectId(linkedServiceJson.getString("objectId"));
                linkedService.setObjectType(linkedServiceJson.getString("objectType"));
                linkedService.set_org(linkedServiceJson.getString("_org"));
                linkedService.setName(linkedServiceJson.getString("name"));

                // Set relation as child of linkedPlanService
                linkedService.setRelation(new JoinField<>("linkedService", linkedPlanService.getObjectId()));
                childRepository.save(linkedService);

                // 3c. Create service cost share as child of linkedPlanService
                PlanChildDocument serviceCostShare = new PlanChildDocument();
                serviceCostShare.setObjectId(serviceCostSharesJson.getString("objectId"));
                serviceCostShare.setObjectType(serviceCostSharesJson.getString("objectType"));
                serviceCostShare.set_org(serviceCostSharesJson.getString("_org"));
                serviceCostShare.setDeductible(serviceCostSharesJson.getInt("deductible"));
                serviceCostShare.setCopay(serviceCostSharesJson.getInt("copay"));
                serviceCostShare.setServiceId(serviceJson.getString("objectId"));

                // Set relation as child of linkedPlanService
                serviceCostShare.setRelation(new JoinField<>("serviceCostShare", linkedPlanService.getObjectId()));
                childRepository.save(serviceCostShare);
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