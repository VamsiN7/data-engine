package com.example.demo.config;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.indices.ExistsRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Configuration
@DependsOn("elasticsearchClient")
public class ElasticsearchIndexConfig {

    @Autowired
    private ElasticsearchClient elasticsearchClient;

    @PostConstruct
    public void setupIndex() throws IOException {
        String indexName = "plans";

        // Check if index exists
        boolean indexExists = elasticsearchClient.indices()
                .exists(ExistsRequest.of(e -> e.index(indexName)))
                .value();

        // Delete existing index if it exists
        if (indexExists) {
            elasticsearchClient.indices().delete(d -> d.index(indexName));
        }

        // Create index with explicit join mapping using a Map for relations
        Map<String, List<String>> relations = new HashMap<>();
        relations.put("plan", List.of("service", "plancostShare", "serviceCostShare", "linkedService"));

        // Create index with the join field mapping
        elasticsearchClient.indices().create(c -> c
                .index(indexName)
                .mappings(m -> m
                        .properties("plan_service_relation", p -> p
                                .join(j -> j
                                        .relations(relations)))
                        .properties("objectId", p -> p.keyword(k -> k))
                        .properties("objectType", p -> p.keyword(k -> k))
                        .properties("_org", p -> p.text(t -> t))
                        .properties("planType", p -> p.keyword(k -> k))
                        .properties("deductible", p -> p.integer(i -> i))
                        .properties("copay", p -> p.integer(i -> i))
                        .properties("serviceId", p -> p.keyword(k -> k))
                        .properties("name", p -> p.text(t -> t))
                        .properties("creationDate", p -> p.date(d -> d.format("yyyy-MM-dd")))));
    }
}