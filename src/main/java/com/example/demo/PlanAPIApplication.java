package com.example.demo;

import java.io.InputStream;
import java.net.URI;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.everit.json.schema.Schema;
import org.everit.json.schema.ValidationException;
import org.everit.json.schema.loader.SchemaLoader;
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

@SpringBootApplication
public class PlanAPIApplication {

	public static void main(String[] args) {
		SpringApplication.run(PlanAPIApplication.class, args);
	}

	// Configure Redis Connection Factory (using Lettuce)
	// @Bean
	// public LettuceConnectionFactory redisConnectionFactory() {
	// 	RedisStandaloneConfiguration config = new RedisStandaloneConfiguration();
	// 	config.setHostName("localhost"); 
	// 	config.setPort(6379); 
	// 	return new LettuceConnectionFactory(config);
	// }

	// // Configure RedisTemplate to use String keys and values
	// @Bean
	// public RedisTemplate<String, String> redisTemplate() {
	// 	RedisTemplate<String, String> template = new RedisTemplate<>();
	// 	template.setConnectionFactory(redisConnectionFactory());
	// 	return template;
	// }
}

@RestController
@RequestMapping("/api/v1/plans")
class PlanController {

	private Schema planSchema;

	@Autowired
	private RedisTemplate<String, String> redisTemplate;

	public PlanController() {
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
					(Map<String, Object>) patchValue
				));
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
			Authentication authentication) {
		try {
			String existingJson = redisTemplate.opsForValue().get(id);
			if (existingJson == null) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Plan not found", "NOT_FOUND"));
			}
			
			JSONObject existing = new JSONObject(existingJson);
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
				patchData.toMap()
			);
			
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
	@DeleteMapping("/{id}")
	public ResponseEntity<?> deletePlan(@PathVariable("id") String id) {
		try {
			Boolean deleted = redisTemplate.delete(id);
			if (deleted == null || !deleted) {
				return ResponseEntity.status(HttpStatus.NOT_FOUND)
						.body(new ErrorResponse("Plan not found", "NOT_FOUND"));
			}
			return ResponseEntity.noContent().build();
		} catch (Exception e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
					.body(new ErrorResponse("Failed to delete plan", "INTERNAL_ERROR"));
		}
	}
}
