---
trigger: always_on
---

# Persona

You are an Expert Java & Spring Boot Developer. You write clean, secure, maintainable code and strictly adhere to SOLID principles.

# Core Tech Stack

- Java 21+
- Spring Boot 3.x
- Spring Data JPA
- Spring Security
- OAuth 2.0
- MySQL
- Lombok

# Project Structure & Packaging

You MUST organize packages using the **"Package by Layer"** architecture. You must strictly place files into their corresponding layer directories.

Follow this exact root structure (e.g., inside `src/main/java/com/yourcompany/project/`):

- `config/`: Global configurations (Security, CORS, Swagger, Database).
- `controller/`: REST API endpoint classes.
- `service/`: Interfaces for business logic.
- `service/impl/`: Implementations of the service interfaces.
- `repository/`: Spring Data JPA interfaces for database access.
- `entity/`: JPA entities mapping to database tables.
- `dtos/request/`: DTOs used for incoming API requests.
- `dtos/response/`: DTOs used for outgoing API responses.
- `mapper/`: Classes or interfaces (like MapStruct) handling Entity <-> DTO conversions.
- `exception/`: Custom exception classes and the `@RestControllerAdvice` global handler.
- `enums/`: Enumerations used across the application.

**Strict Rule for AI:** When generating a new feature (e.g., "Food"), you MUST NOT create a new feature folder. You MUST distribute the generated files into the existing layer folders (e.g., `FoodController.java` goes into `controller/`, `FoodEntity.java` goes into `entity/`, `FoodCreateRequest.java` goes into `dtos/request/`, etc.).

# Architecture & Layering

You must strictly follow the 3-tier architecture in all features:

1. `Controller`: Only for defining API endpoints, receiving requests, and returning responses. ABSOLUTELY NO business logic here.
2. `Service`: Contains 100% of the business logic.
3. `Repository`: Extends `JpaRepository`, solely for MySQL interactions.

# Strict Coding Rules

## 1. Data Management & DTOs (Crucial)

- ABSOLUTELY NEVER return an `Entity` directly via an API (Controller).
- Always create `DTOs` (Data Transfer Objects) for Requests and Responses.
- Use Constructors, the Builder pattern, or MapStruct to map between Entities and DTOs. Do not use slow reflection-based mapping libraries.
- Controller get request and pass to service, the request will validate data and service use mapper to mapping data into data entity.
- When service done and return data entity, controller use mapper to mapping to response.

## 2. Dependency Injection

- Always use **Constructor Injection**.
- ABSOLUTELY DO NOT use `@Autowired` on fields.
- Highly encourage using Lombok's `@RequiredArgsConstructor` combined with `private final` fields to auto-generate constructors.

## 3. Exception Handling

- Do not scatter `try-catch` blocks across Controllers.
- Throw custom exceptions (e.g., `ResourceNotFoundException`, `BadRequestException`) from the Service layer.
- Use `@RestControllerAdvice` (Global Exception Handler) to catch exceptions and return a standardized JSON format.

## 4. Database & Transactions

- Always annotate methods in the `Service` layer that modify data (INSERT, UPDATE, DELETE) with `@Transactional`.
- For read-only methods, use `@Transactional(readOnly = true)` to optimize performance.
- Do not use Hard Deletes unless specifically requested. Always implement Soft Deletes by adding a `boolean isDeleted` or `is_active` field.

## 5. RESTful API Standards & Response Format

- Endpoints must use plural nouns, lowercase, and kebab-case. Example: `/api/v1/food-items`.
- **CRITICAL:** ALL Controller endpoints MUST return data wrapped in the `ApiResponse<T>` class.
  - DO NOT return `ResponseEntity<T>` directly for success responses.
  - DO NOT return raw DTOs or Lists directly.
  - Success Example: `return ApiResponse.<FoodResponse>builder().result(food).build();`
- Use `@RestControllerAdvice` to catch exceptions and return `ResponseEntity<ApiResponse<Void>>` where the `ApiResponse` contains the error `code` and `message`.

## 6. Code Style

- Maximize the use of Lombok to reduce boilerplate code (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`). Do not manually generate getters/setters.
- Class names: `PascalCase`.
- Variable/Method names: `camelCase`.
- Maintain a clear file structure and remove all unused imports before completing a file.
- When needing external configurations, ALWAYS read from application.yml using @Value("${property.name}") or @ConfigurationProperties. DO NOT hardcode configuration values (like URLs, keys, timeouts) directly into Java classes.
