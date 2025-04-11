# Project Documentation

## 1. Project Overview
This is a robust healthcare plan management API built using Spring Boot that implements a RESTful service with various modern architectural components and features.

## 2. Technology Stack
- **Framework**: Spring Boot
- **Database/Cache**: Redis (for plan storage and caching)
- **Search Engine**: Elasticsearch (for plan search capabilities)
- **Message Queue**: RabbitMQ (for asynchronous processing)
- **Security**: Spring Security (for authentication and authorization)
- **Data Validation**: JSON Schema validation

## 3. Core Features

### 3.1 Plan Management
The API provides complete CRUD (Create, Read, Update, Delete) operations for healthcare plans:

1. **Create Plan** (`POST /api/v1/plans`)
   - Validates incoming plan data against a JSON schema
   - Generates ETag for caching
   - Stores plan data in Redis
   - Creates metadata (created_by, created_at)
   - Sends plan to RabbitMQ for async processing

2. **Retrieve Plan** (`GET /api/v1/plans/{id}`)
   - Supports ETag-based caching
   - Returns 304 Not Modified if plan hasn't changed
   - Includes full plan data with metadata

3. **Update Plan** (`PUT /api/v1/plans/{id}`)
   - Supports optimistic concurrency with If-Match header
   - Validates updated plan against schema
   - Updates metadata (updated_by, updated_at)
   - Generates new ETag

4. **Patch Plan** (`PATCH /api/v1/plans/{id}`)
   - Supports partial updates
   - Implements deep merging of nested objects
   - Special handling for array updates

5. **Delete Plan** (`DELETE /api/v1/plans/{id}`)
   - Removes plan from Redis storage

6. **Search Plans** (`GET /api/v1/plans/search`)
   - Supports searching by:
     - Organization
     - Plan Type
     - Service ID
   - Utilizes Elasticsearch for efficient searching

### 3.2 Data Validation
- Uses JSON Schema validation for all incoming plan data
- Schema is loaded from `plan-schema.json` resource file
- Provides detailed validation error messages

### 3.3 Caching Strategy
- Implements ETag-based caching
- Supports conditional requests (If-None-Match, If-Match headers)
- Uses Redis as the primary storage and caching solution

### 3.4 Security Features
- Authentication required for all endpoints
- User information tracked in metadata
- Secure handling of plan modifications

### 3.5 Asynchronous Processing
- RabbitMQ integration for async operations
- Plan changes are queued for background processing

## 4. Project Structure
```
src/main/java/com/example/demo/
├── PlanAPIApplication.java (Main application file)
├── config/         (Configuration classes)
├── model/          (Data models)
├── repository/     (Data access layer)
├── service/        (Business logic)
    ├── ElasticsearchService
    └── RabbitMQProducerService
├── security/       (Security configurations)
└── interceptor/    (Request interceptors)
```

## 5. Error Handling
The API implements comprehensive error handling:
- Validation errors (400 Bad Request)
- Not Found errors (404)
- Concurrency conflicts (412 Precondition Failed)
- Server errors (500)

## 6. Data Model
Plans are stored with:
- Core plan data
- Metadata (created_by, created_at, updated_by, updated_at)
- ETag for version control
- Object ID for unique identification

This project implements a sophisticated healthcare plan management system with modern web architecture practices, including caching, validation, security, and asynchronous processing. It's designed to be scalable, maintainable, and follows REST best practices.
