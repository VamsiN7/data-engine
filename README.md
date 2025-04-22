# Project Documentation

## Project Overview
This is a robust healthcare plan management API built using Spring Boot that implements a RESTful service with various modern architectural components and features.

## Technology Stack
- **Framework**: Spring Boot
- **Database/Cache**: Redis (for plan storage and caching)
- **Search Engine**: Elasticsearch (for plan search capabilities)
- **Message Queue**: RabbitMQ (for asynchronous processing)
- **Security**: Spring Security (for authentication and authorization)
- **Data Validation**: JSON Schema validation

# System Architecture and Workflow

## 1. Core Components

### 1.1 Redis Cache/Database
- Acts as the primary storage system
- Stores plan data in JSON format
- Enables fast retrieval and caching
- Structure of stored data:
  ```json
  {
    "data": {
      // actual plan data
    },
    "metadata": {
      "created_by": "username",
      "created_at": "timestamp",
      "updated_by": "username",
      "updated_at": "timestamp"
    },
    "etag": "md5hash"
  }
  ```

### 1.2 Elasticsearch
- Powers the search functionality
- Indexes plan data for efficient searching
- Enables complex queries on plan attributes

### 1.3 RabbitMQ
- Message queue for asynchronous operations
- Handles background processing of plan data
- Ensures system scalability

## 2. Request Flow

### 2.1 Creating a Plan (POST Request)
```mermaid
sequenceDiagram
    Client->>API: POST /api/v1/plans
    API->>Validator: Validate JSON Schema
    API->>Redis: Store Plan Data
    API->>RabbitMQ: Queue for Processing
    API->>Client: Return 201 Created
    RabbitMQ->>Elasticsearch: Index Plan Data
```

1. Client sends a POST request with plan data
2. System validates the JSON against schema
3. Generates an ETag (MD5 hash of plan data)
4. Creates metadata with timestamp and user info
5. Stores in Redis
6. Queues to RabbitMQ for background processing
7. Returns success response with location header

### 2.2 Retrieving a Plan (GET Request)
```mermaid
sequenceDiagram
    Client->>API: GET /api/v1/plans/{id}
    API->>Redis: Fetch Plan
    Redis->>API: Return Plan Data
    API->>Client: Return Plan (or 304 Not Modified)
```

1. Client requests plan with ID
2. System checks Redis for plan
3. If ETag matches (If-None-Match header), returns 304
4. Otherwise returns full plan data

### 2.3 Updating a Plan (PUT Request)
```mermaid
sequenceDiagram
    Client->>API: PUT /api/v1/plans/{id}
    API->>Redis: Check Current Version
    API->>Validator: Validate New Data
    API->>Redis: Update Plan
    API->>RabbitMQ: Queue Updates
    API->>Client: Return Updated Plan
```

1. Client sends PUT request with new plan data
2. System validates If-Match header against current ETag
3. Validates new JSON data
4. Updates plan in Redis with new ETag
5. Queues changes for background processing
6. Returns updated plan

### 2.4 Searching Plans
```mermaid
sequenceDiagram
    Client->>API: GET /api/v1/plans/search
    API->>Elasticsearch: Query Plans
    Elasticsearch->>API: Return Matching Plans
    API->>Client: Return Search Results
```

1. Client sends search query with parameters
2. System queries Elasticsearch
3. Returns matching plans

## 3. Data Validation and Security

### 3.1 JSON Schema Validation
- Every plan must conform to predefined schema
- Validates:
  - Required fields
  - Data types
  - Field formats
  - Nested object structure

### 3.2 Security Measures
```mermaid
flowchart LR
    Client --> Authentication
    Authentication --> Authorization
    Authorization --> API
```

- Authentication required for all endpoints
- User information tracked in metadata
- ETag-based concurrency control
- Secure headers and responses

## 4. Error Handling

The system handles various error scenarios:

1. **Validation Errors** (400)
   - Invalid JSON format
   - Schema validation failures

2. **Authentication Errors** (401)
   - Missing or invalid credentials

3. **Not Found Errors** (404)
   - Plan ID doesn't exist

4. **Concurrency Errors** (412)
   - ETag mismatch during updates

5. **Server Errors** (500)
   - Internal processing failures

## 5. Asynchronous Processing

The RabbitMQ integration enables:
1. Background indexing in Elasticsearch
2. Async data processing
3. System scalability
4. Reduced response times

## 6. Caching Strategy

The system implements a sophisticated caching strategy:
1. ETag generation for all plans
2. Conditional requests support
3. Redis as cache layer
4. Cache invalidation on updates

## 7. API Endpoints Summary

1. `POST /api/v1/plans`
   - Create new plans
   - Returns Location header and ETag

2. `GET /api/v1/plans/{id}`
   - Retrieve plans
   - Supports conditional requests

3. `PUT /api/v1/plans/{id}`
   - Full plan updates
   - Requires If-Match header

4. `PATCH /api/v1/plans/{id}`
   - Partial plan updates
   - Deep merging support

5. `DELETE /api/v1/plans/{id}`
   - Remove plans
   - Cascading deletions

6. `GET /api/v1/plans/search`
   - Search functionality
   - Multiple search parameters

This system is designed with scalability, reliability, and performance in mind. It uses modern architectural patterns and best practices to ensure robust plan data management while maintaining data consistency and providing fast access to information.

## 1. Core Features

### 1.1 Plan Management
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

### 1.2 Data Validation
- Uses JSON Schema validation for all incoming plan data
- Schema is loaded from `plan-schema.json` resource file
- Provides detailed validation error messages

### 1.3 Caching Strategy
- Implements ETag-based caching
- Supports conditional requests (If-None-Match, If-Match headers)
- Uses Redis as the primary storage and caching solution

### 1.4 Security Features
- Authentication required for all endpoints
- User information tracked in metadata
- Secure handling of plan modifications

### 1.5 Asynchronous Processing
- RabbitMQ integration for async operations
- Plan changes are queued for background processing

## 2. Project Structure
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

## 3. Error Handling
The API implements comprehensive error handling:
- Validation errors (400 Bad Request)
- Not Found errors (404)
- Concurrency conflicts (412 Precondition Failed)
- Server errors (500)

## 4. Data Model
Plans are stored with:
- Core plan data
- Metadata (created_by, created_at, updated_by, updated_at)
- ETag for version control
- Object ID for unique identification

This project implements a sophisticated healthcare plan management system with modern web architecture practices, including caching, validation, security, and asynchronous processing. It's designed to be scalable, maintainable, and follows REST best practices.
