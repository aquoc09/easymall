---
trigger: always_on
---

# AI Agent Rules for This Project

## 1) Project Overview
- This is a Java backend project built with Spring Boot, Spring MVC, Spring Data JPA, Spring Security, and Jakarta EE APIs.
- The codebase follows a layered backend architecture with common packages such as:
  - `controller`
  - `service`
  - `repository`
  - `entity`
  - `dto`
  - `mapper`
  - `configuration`
  - `validator`
  - `exception`
  - `util`
- The project uses Lombok, MapStruct, JWT-based security, and a MySQL database.

## 2) Core Engineering Principles
- Prefer small, targeted changes over broad refactors.
- Preserve the existing package structure and naming conventions.
- Keep business logic in service classes, not in controllers.
- Use DTOs for API input and output; avoid exposing entities directly unless the project already does so consistently.
- Respect existing mapper usage when converting between entities and DTOs.
- Follow Spring conventions for dependency injection, transaction boundaries, and validation.

## 3) Code Style Expectations
- Use clear, descriptive names for classes, methods, fields, and variables.
- Keep methods focused on a single responsibility.
- Avoid duplicated logic; extract shared behavior into services, mappers, helpers, or validators where appropriate.
- Maintain consistency with the project’s current use of Lombok and Jakarta imports.
- Prefer constructor injection unless the surrounding codebase clearly uses another established pattern.

## 4) API and Controller Rules
- Controllers should handle request/response orchestration only.
- Validate incoming request data before processing.
- Return consistent response structures aligned with the project’s existing API style.
- Do not place persistence or complex business rules inside controllers.
- Keep endpoint paths, status codes, and response payloads compatible with existing API behavior.

## 5) Service Layer Rules
- Implement business logic in services.
- Keep services testable and decoupled from web-layer details.
- Use transactions where data consistency matters.
- If a service already has an implementation pattern, follow it instead of introducing a new one.

## 6) Repository and Database Rules
- Use Spring Data JPA repositories for persistence access.
- Keep repository methods concise and domain-focused.
- Prefer derived queries or well-named custom queries when needed.
- Do not bypass existing repository abstractions without a strong reason.

## 7) Entity and DTO Rules
- Treat entities as persistence models, not API models.
- Use DTOs for request and response contracts.
- Keep entity relationships and database mappings consistent with current design.
- If a field is added or changed in an entity, check all affected DTOs, mappers, services, and validations.

## 8) Mapper Rules
- Use mapper classes to convert between entities and DTOs.
- Keep mapping logic explicit and easy to review.
- When adding fields, update all relevant mappings in the same change set.
- Avoid manual mapping duplication across controllers and services.

## 9) Security Rules
- Respect the existing Spring Security configuration.
- Preserve JWT/authentication flows and security entry points already in place.
- Do not weaken authorization rules unless explicitly required.
- Any security-related change should be reviewed for side effects on public and protected endpoints.

## 10) Validation and Error Handling
- Use validation annotations and validator classes where applicable.
- Keep exception handling centralized and predictable.
- Prefer meaningful error messages and consistent error response formats.
- Do not leak internal stack traces or sensitive internal details to API consumers.

## 11) Testing Rules
- Add or update tests when behavior changes.
- Prefer focused tests for controllers, services, repositories, and mappings.
- Keep tests readable and aligned with the project’s architecture.
- When fixing a bug, add a regression test if possible.

## 12) Dependency and Framework Rules
- Use the existing project dependencies and framework versions unless there is a clear reason to change them.
- Do not introduce new libraries lightly.
- Prefer built-in Spring/Jakarta capabilities before adding external dependencies.

## 13) Change Safety Rules
- Before making changes, inspect related classes in controller, service, repository, DTO, mapper, and configuration layers.
- Check for indirect impacts on API contracts, security, and database mappings.
- Make the smallest safe change that solves the problem.
- Avoid “cleanup” changes unrelated to the task.

## 14) Documentation Rules
- Keep documentation short, accurate, and aligned with the current code.
- If a rule conflicts with actual project code, follow the project code unless the user explicitly asks to modernize it.
- Update README or inline comments only when they improve maintainability.

## 15) AI Agent Behavior Rules
- Infer project conventions from existing code before proposing new patterns.
- Match the surrounding code style and architecture.
- If the project already uses a specific pattern, continue using it consistently.
- Prefer practical, production-safe suggestions over theoretical redesigns.

## 16) What to Avoid
- Do not rewrite the whole project for a single issue.
- Do not introduce framework migrations unless requested.
- Do not expose secrets, credentials, tokens, or environment-specific sensitive values.
- Do not break backward compatibility without a strong reason and explicit approval.

## 17) Priority Rule
When rules conflict, follow this order:
1. User request
2. Existing project code and conventions
3. These AI agent rules
4. General best practices

## 18) Final Goal
Your job is to help evolve this backend safely, consistently, and with minimal disruption to the existing architecture.