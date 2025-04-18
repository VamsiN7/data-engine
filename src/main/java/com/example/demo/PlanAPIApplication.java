package com.example.demo;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONTokener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.util.DigestUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.demo.model.DataResponse;
import com.example.demo.model.ErrorResponse;
import com.example.demo.model.PlanDocument;
import com.example.demo.service.ElasticsearchService;
import com.example.demo.service.RabbitMQProducerService;

@SpringBootApplication
public class PlanAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanAPIApplication.class, args);
	}
}

@RestController
@RequestMapping("/api/v1/plans")
class PlanController {

	private Schema planSchema;
	private final RedisTemplate<String, String> redisTemplate;
	private final ElasticsearchService elasticsearchService;
	private final RabbitMQProducerService rabbitMQProducerService;

	@Autowired
	public PlanController(
			RedisTemplate<String, String> redisTemplate,
			ElasticsearchService elasticsearchService,
			RabbitMQProducerService rabbitMQProducerService) {
		this.redisTemplate = redisTemplate;
		this.elasticsearchService = elasticsearchService;
		this.rabbitMQProducerService = rabbitMQProducerService;
		try (InputStream schemaStream = getClass().getResourceAsStream("/plan-schema.json")) {
			if (schemaStream == null) {
				throw new RuntimeException("Schema file not found");
			}
			JSONObject rawSchema = new JSONObject(new JSONTokener(schemaStream));
			planSchema = SchemaLoader.load(rawSchema);
		} catch (Exception e) {
			throw new RuntimeException("Failed to load JSON Schema", e);
		}
	}

	// POST /api/v1/plans - Create a new plan
	@PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> createPlan(
			@RequestBody String planJson,
			Authentication authentication) {
		try {
			JSONObject jsonObject = new JSONObject(planJson);
			planSchema.validate(jsonObject);

			String objectId = jsonObject.getString("objectId");
			String etag = DigestUtils.md5DigestAsHex(planJson.getBytes());

			// Create metadata
			Map<String, Object> metadata = new HashMap<>();
			metadata.put("created_by", authentication.getName());
			metadata.put("created_at", LocalDateTime.now().toString());
			metadata.put("updated_at", LocalDateTime.now().toString());

			// Create response
			DataResponse response = new DataResponse(jsonObject.toMap());

			// Store in Redis with metadata
			JSONObject redisData = new JSONObject();
			redisData.put("data", jsonObject);
			redisData.put("metadata", metadata);
			redisData.put("etag", etag);
			redisTemplate.opsForValue().set(objectId, redisData.toString());

			// Send message to RabbitMQ for async processing
			rabbitMQProducerService.sendMessage(jsonObject);

			URI location = ServletUriComponentsBuilder.fromCurrentRequest()
					.path("/{id}")
					.buildAndExpand(objectId)
					.toUri();

			return ResponseEntity.created(location)
					.eTag(etag)
					.body(response);

		} catch (ValidationException ve) {
			return ResponseEntity.badRequest()
					.body(new ErrorResponse(ve.getAllMessages().toString(), "VALIDATION_ERROR"));
		} catch (Exception e) {
			return ResponseEntity.badRequest()
					.body(new ErrorResponse("Invalid JSON payload", "INVALID_PAYLOAD"));
		}
	}

	// GET /api/v1/plans/{id} - Retrieve a plan with ETag support
	@GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> getPlan(
			@PathVariable("id") String id,
			@RequestHeader(value = "If-None-Match", required = false) String ifNoneMatch) {
		try {
			String planJson = redisTemplate.opsForValue().get(id);
			if (planJson == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Plan not found", "NOT_FOUND"));
			}

			JSONObject jsonObject = new JSONObject(planJson);
			String etag = jsonObject.getString("etag");

			if (etag.equals(ifNoneMatch)) {
				return ResponseEntity.status(HttpStatus.NOT_MODIFIED)
						.eTag(etag)
						.build();
			}

			// Return only the data portion
			return ResponseEntity.ok()
					.eTag(etag)
					.body(jsonObject.getJSONObject("data").toString());

		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to retrieve plan", "INTERNAL_ERROR"));
		}
	}

	// PUT /api/v1/plans/{id} - Update a plan
	@PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> updatePlan(
			@PathVariable("id") String id,
			@RequestBody String planJson,
			@RequestHeader(value = "If-Match", required = false) String ifMatch,
			Authentication authentication) {
		try {
			String existingJson = redisTemplate.opsForValue().get(id);
			if (existingJson == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Plan not found", "NOT_FOUND"));
			}

			JSONObject existing = new JSONObject(existingJson);
			String existingEtag = existing.getString("etag");

			if (ifMatch != null && !existingEtag.equals(ifMatch)) {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
						.body(new ErrorResponse("Precondition failed", "PRECONDITION_FAILED"));
			}

			JSONObject newPlan = new JSONObject(planJson);
			planSchema.validate(newPlan);

			String newEtag = DigestUtils.md5DigestAsHex(planJson.getBytes());

			// Update metadata
			Map<String, Object> metadata = existing.getJSONObject("metadata").toMap();
			metadata.put("updated_by", authentication.getName());
			metadata.put("updated_at", LocalDateTime.now().toString());

			// Create response
			DataResponse response = new DataResponse(newPlan.toMap());

			// Store in Redis with metadata
			JSONObject redisData = new JSONObject();
			redisData.put("data", newPlan);
			redisData.put("metadata", metadata);
			redisData.put("etag", newEtag);
			redisTemplate.opsForValue().set(id, redisData.toString());

			// Send message to RabbitMQ for async processing
			rabbitMQProducerService.sendMessage(newPlan);

			return ResponseEntity.ok()
					.eTag(newEtag)
					.body(response);

		} catch (ValidationException ve) {
			return ResponseEntity.badRequest()
					.body(new ErrorResponse(ve.getAllMessages().toString(), "VALIDATION_ERROR"));
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to update plan", "INTERNAL_ERROR"));
		}
	}

	// Helper method to deep merge objects
	private Map<String, Object> deepMerge(Map<String, Object> existing, Map<String, Object> patch) {
		Map<String, Object> result = new HashMap<>(existing);

		for (Map.Entry<String, Object> entry : patch.entrySet()) {
			String key = entry.getKey();
			Object patchValue = entry.getValue();

			if (patchValue instanceof Map && result.containsKey(key) && result.get(key) instanceof Map) {
				// Recursively merge nested objects
				result.put(key, deepMerge(
						(Map<String, Object>) result.get(key),
						(Map<String, Object>) patchValue));
			} else if (patchValue instanceof List && result.containsKey(key) && result.get(key) instanceof List) {
				// Handle arrays with special merge logic
				result.put(key, mergeArrays((List<Object>) result.get(key), (List<Object>) patchValue));
			} else {
				// For primitive values or new fields, use the patch value
				result.put(key, patchValue);
			}
		}

		return result;
	}

	// Helper method to merge arrays with specific rules
	private List<Object> mergeArrays(List<Object> existing, List<Object> patch) {
		List<Object> result = new ArrayList<>(existing);

		for (Object patchItem : patch) {
			if (patchItem instanceof Map) {
				Map<String, Object> patchMap = (Map<String, Object>) patchItem;
				String objectId = (String) patchMap.get("objectId");

				if (objectId == null) {
					// If no objectId, append as new item
					result.add(patchItem);
					continue;
				}

				// Find existing item with matching objectId
				boolean found = false;
				for (int i = 0; i < result.size(); i++) {
					Object existingItem = result.get(i);
					if (existingItem instanceof Map) {
						Map<String, Object> existingMap = (Map<String, Object>) existingItem;
						String existingId = (String) existingMap.get("objectId");

						if (objectId.equals(existingId)) {
							// If found and different, replace with new item
							if (!existingItem.equals(patchItem)) {
								result.set(i, patchItem);
							}
							found = true;
							break;
						}
					}
				}

				// If not found, append as new item
				if (!found) {
					result.add(patchItem);
				}
			} else {
				// For non-map items, append if not already present
				if (!result.contains(patchItem)) {
					result.add(patchItem);
				}
			}
		}

		return result;
	}

	// PATCH /api/v1/plans/{id} - Partially update a plan
	@PatchMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
	public ResponseEntity<?> patchPlan(
			@PathVariable("id") String id,
			@RequestBody String planJson,
			@RequestHeader(value = "If-Match", required = false) String ifMatch,
			Authentication authentication) {
		try {
			String existingJson = redisTemplate.opsForValue().get(id);
			if (existingJson == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Plan not found", "NOT_FOUND"));
			}

			JSONObject existing = new JSONObject(existingJson);
			String existingEtag = existing.getString("etag");

			// Check If-Match header
			if (!existingEtag.equals(ifMatch)) {
				return ResponseEntity.status(HttpStatus.PRECONDITION_FAILED)
						.body(new ErrorResponse("Precondition failed", "PRECONDITION_FAILED"));
			}

			JSONObject patch = new JSONObject(planJson);

			// Handle both direct plan data and full response format
			JSONObject patchData;
			if (patch.has("data")) {
				patchData = patch.getJSONObject("data");
			} else {
				patchData = patch;
			}

			// Deep merge the data
			Map<String, Object> mergedData = deepMerge(
					existing.getJSONObject("data").toMap(),
					patchData.toMap());

			// Validate the merged data against the schema
			planSchema.validate(new JSONObject(mergedData));

			// Update metadata
			Map<String, Object> metadata = existing.getJSONObject("metadata").toMap();
			metadata.put("updated_by", authentication.getName());
			metadata.put("updated_at", LocalDateTime.now().toString());

			String newEtag = DigestUtils.md5DigestAsHex(new JSONObject(mergedData).toString().getBytes());

			// Create response
			DataResponse response = new DataResponse(mergedData);

			// Store in Redis with metadata
			JSONObject redisData = new JSONObject();
			redisData.put("data", new JSONObject(mergedData));
			redisData.put("metadata", metadata);
			redisData.put("etag", newEtag);
			redisTemplate.opsForValue().set(id, redisData.toString());

			// Send message to RabbitMQ for async processing
			// rabbitMQProducerService.sendMessage(patch);
			// Send message to RabbitMQ for async processing
			JSONObject updatedPlan = new JSONObject(mergedData);
			rabbitMQProducerService.sendMessage(updatedPlan);

			return ResponseEntity.ok()
					.eTag(newEtag)
					.body(response);

		} catch (ValidationException ve) {
			return ResponseEntity.badRequest()
					.body(new ErrorResponse(ve.getAllMessages().toString(), "VALIDATION_ERROR"));
		} catch (Exception e) {
			System.out.println("Error: " + e.getMessage());
			e.printStackTrace();
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to patch plan: " + e.getMessage(), "INTERNAL_ERROR"));
		}
	}

	// DELETE /api/v1/plans/{id} - Delete a plan
	// @DeleteMapping("/{id}")
	// public ResponseEntity<?> deletePlan(@PathVariable("id") String id) {
	// try {
	// Boolean deleted = redisTemplate.delete(id);
	// if (deleted == null || !deleted) {
	// return ResponseEntity.status(HttpStatus.NOT_FOUND)
	// .body(new ErrorResponse("Plan not found", "NOT_FOUND"));
	// }

	// // Send delete message to RabbitMQ
	// JSONObject deleteMessage = new JSONObject();
	// deleteMessage.put("operation", "delete");
	// deleteMessage.put("objectId", id);
	// rabbitMQProducerService.sendMessage(deleteMessage);

	// return ResponseEntity.noContent().build();
	// } catch (Exception e) {
	// return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
	// .body(new ErrorResponse("Failed to delete plan", "INTERNAL_ERROR"));
	// }
	// }
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deletePlan(@PathVariable("id") String id) {
		try {
			// Check if this is a direct plan ID
			Boolean planExists = redisTemplate.hasKey(id);
			if (planExists != null && planExists) {
				// This is a top-level plan ID - delete it directly
				redisTemplate.delete(id);

				// Send delete message to RabbitMQ
				JSONObject deleteMessage = new JSONObject();
				deleteMessage.put("operation", "delete");
				deleteMessage.put("objectId", id);
				rabbitMQProducerService.sendMessage(deleteMessage);

				return ResponseEntity.noContent().build();
			} else {
				// This might be a child object ID - search for it in all plans
				boolean childFound = false;

				// Get all keys (could be optimized with pattern matching if Redis supports it)
				Set<String> keys = redisTemplate.keys("*");
				for (String planKey : keys) {
					String planJson = redisTemplate.opsForValue().get(planKey);
					if (planJson == null)
						continue;

					JSONObject existing = new JSONObject(planJson);
					JSONObject planData = existing.getJSONObject("data");

					// Check if this plan contains the child ID
					if (containsChildWithId(planData, id)) {
						// Remove the child with that ID
						removeChildWithId(planData, id);

						// Update plan metadata
						String username = "system"; // or get from authentication if available
						Map<String, Object> metadata = existing.getJSONObject("metadata").toMap();
						metadata.put("updated_by", username);
						metadata.put("updated_at", LocalDateTime.now().toString());

						// Calculate new ETag
						String newEtag = DigestUtils.md5DigestAsHex(planData.toString().getBytes());

						// Store updated plan
						JSONObject redisData = new JSONObject();
						redisData.put("data", planData);
						redisData.put("metadata", metadata);
						redisData.put("etag", newEtag);
						redisTemplate.opsForValue().set(planKey, redisData.toString());

						// Send message to RabbitMQ for async processing
						rabbitMQProducerService.sendMessage(planData);

						childFound = true;
						break;
					}
				}

				if (childFound) {
					return ResponseEntity.noContent().build();
				} else {
					return ResponseEntity.status(HttpStatus.NOT_FOUND)
							.body(new ErrorResponse("Object not found", "NOT_FOUND"));
				}
			}
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to delete object: " + e.getMessage(), "INTERNAL_ERROR"));
		}
	}

	// Helper method to check if a plan contains a child with the given ID
	private boolean containsChildWithId(JSONObject planData, String childId) {
		// Check planCostShares
		if (planData.has("planCostShares")) {
			JSONObject costShares = planData.getJSONObject("planCostShares");
			if (childId.equals(costShares.optString("objectId"))) {
				return true;
			}
		}

		// Check linkedPlanServices
		if (planData.has("linkedPlanServices")) {
			JSONArray services = planData.getJSONArray("linkedPlanServices");
			for (int i = 0; i < services.length(); i++) {
				JSONObject service = services.getJSONObject(i);

				// Check service itself
				if (childId.equals(service.optString("objectId"))) {
					return true;
				}

				// Check linkedService
				if (service.has("linkedService")) {
					JSONObject linkedService = service.getJSONObject("linkedService");
					if (childId.equals(linkedService.optString("objectId"))) {
						return true;
					}
				}

				// Check planserviceCostShares
				if (service.has("planserviceCostShares")) {
					JSONObject serviceCostShares = service.getJSONObject("planserviceCostShares");
					if (childId.equals(serviceCostShares.optString("objectId"))) {
						return true;
					}
				}
			}
		}

		return false;
	}

	// Helper method to remove a child with the given ID
	private void removeChildWithId(JSONObject planData, String childId) {
		// We can't remove planCostShares (required field), so we'd just update it
		if (planData.has("planCostShares")) {
			JSONObject costShares = planData.getJSONObject("planCostShares");
			if (childId.equals(costShares.optString("objectId"))) {
				// We could reset values but keep the object
				costShares.put("deductible", 0);
				costShares.put("copay", 0);
				return;
			}
		}

		// Check linkedPlanServices
		if (planData.has("linkedPlanServices")) {
			JSONArray services = planData.getJSONArray("linkedPlanServices");
			JSONArray updatedServices = new JSONArray();

			for (int i = 0; i < services.length(); i++) {
				JSONObject service = services.getJSONObject(i);

				// Skip if this is the service to remove
				if (childId.equals(service.optString("objectId"))) {
					continue;
				}

				// Check if we need to modify the service's children
				boolean addService = true;

				if (service.has("linkedService")) {
					JSONObject linkedService = service.getJSONObject("linkedService");
					if (childId.equals(linkedService.optString("objectId"))) {
						// If linkedService is the target, remove the whole service
						addService = false;
					}
				}

				if (service.has("planserviceCostShares")) {
					JSONObject serviceCostShares = service.getJSONObject("planserviceCostShares");
					if (childId.equals(serviceCostShares.optString("objectId"))) {
						// If planserviceCostShares is the target, remove the whole service
						addService = false;
					}
				}

				if (addService) {
					updatedServices.put(service);
				}
			}

			// Replace the services array
			planData.put("linkedPlanServices", updatedServices);
		}
	}

	@GetMapping("/search")
	public ResponseEntity<?> searchPlans(
			@RequestParam(required = false) String org,
			@RequestParam(required = false) String planType,
			@RequestParam(required = false) String serviceId) {
		try {
			List<PlanDocument> results;

			if (org != null) {
				results = elasticsearchService.searchByOrganization(org);
			} else if (planType != null) {
				results = elasticsearchService.searchByPlanType(planType);
			} else if (serviceId != null) {
				results = elasticsearchService.searchByServiceId(serviceId);
			} else {
				return ResponseEntity.badRequest()
						.body(new ErrorResponse("At least one search parameter is required", "INVALID_SEARCH"));
			}

			return ResponseEntity.ok(results);
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Search failed", "SEARCH_ERROR"));
		}
	}
}
