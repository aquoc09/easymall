# Exception Handling in This Project

This document describes the exception handling strategy used in this backend project.

## 1) Overview
The project uses a centralized exception handling approach to keep error responses consistent across the API.

Main goals:
- return predictable error responses
- avoid repeating try/catch logic in controllers
- map domain and validation errors to proper HTTP responses
- keep error handling aligned with the project’s API response format

## 2) Main Exception Types

### App-level exceptions
Custom business exceptions are used for application-specific failures.

Use this type when:
- a business rule is violated
- a resource is missing
- authentication or authorization rules fail
- the request is valid syntactically but not acceptable logically

These exceptions should carry an application error code and message.

### Generic runtime exceptions
Unexpected runtime failures are handled separately and mapped to a generic error response.

Use this path when:
- the error is not mapped to a known business case
- the application needs to fail safely without exposing internal details

### Validation exceptions
Request validation errors are handled globally and translated into user-friendly API responses.

Use this path when:
- input data fails bean validation
- request payload fields violate annotations or constraints

## 3) Global Exception Handler Responsibilities
The global exception handler should:
- catch application exceptions
- catch authorization failures
- catch validation failures
- catch unexpected exceptions
- return a consistent response body format
- log errors for debugging and tracing

It should not:
- contain business logic
- perform recovery operations
- expose stack traces or sensitive internal details to clients

## 4) Error Response Style
The API should return a structured response with at least:
- error code
- message
- optional details or validation message

For application exceptions, the HTTP status should reflect the error type.
For generic exceptions, the response should remain safe and generic.

## 5) Application Exception Flow
When a business rule fails:
1. The service layer throws a custom application exception.
2. The global exception handler catches it.
3. The handler extracts the error code and message.
4. The handler returns a response using the matching HTTP status.

This keeps controller code clean and makes business failures predictable.

## 6) Validation Error Flow
When input validation fails:
1. The request is rejected before service execution.
2. The global handler catches the validation exception.
3. The handler reads the field error information.
4. The handler maps the validation key to a project error code.
5. The handler returns a user-friendly validation message.

If the validation rule contains attributes such as minimum value, the handler should include them in the returned message when appropriate.

## 7) Authorization Failure Flow
When access is denied:
1. The security layer raises an authorization-related exception.
2. The global handler maps it to an unauthorized response.
3. The API returns a consistent error payload.

This prevents security-related failures from leaking implementation details.

## 8) Logging Rules
- Log exceptions server-side for troubleshooting.
- Keep logs meaningful and concise.
- Include stack traces for unexpected failures.
- Do not return stack traces to the client.

## 9) Implementation Guidelines for Agents
When adding or changing exceptions:
- reuse the existing error response structure
- prefer custom application exceptions for business rules
- keep validation errors centralized
- avoid catching exceptions in controllers unless there is a special case
- update the error code catalog if new business cases are added

## 10) Best Practices for This Project
- Throw custom exceptions from service or validation-related layers.
- Let the global handler convert exceptions into API responses.
- Keep exception messages consistent with the project’s error code system.
- Do not duplicate exception handling logic in multiple places.
- Ensure new features integrate with the same response style.

## 11) Agent Goal
The AI agent should preserve a clean, centralized, and consistent exception handling flow so the API remains easy to maintain and safe to use.