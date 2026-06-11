# Configuration Guidelines for This Project

This document describes the configuration conventions used in this backend project.

## 1) Overview
The project uses a Spring Boot-based configuration setup to manage:
- application startup behavior
- security configuration
- JWT authentication support
- environment-driven settings
- bean initialization and application wiring

Configuration should stay centralized, readable, and easy to maintain.

## 2) Main Configuration Areas

### Application Initialization
- Use dedicated configuration classes for startup-related logic.
- Keep initialization logic separate from controllers and services.
- Use this area for seeding data, preparing defaults, or boot-time setup when needed.

### Security Configuration
- Keep authentication and authorization rules in a dedicated security configuration.
- Define public and protected endpoints clearly.
- Preserve the existing token-based security flow.
- Avoid mixing security rules with business logic.

### Authentication Entry Point
- Handle unauthorized access through a dedicated authentication entry point.
- Return consistent responses for unauthenticated requests.
- Do not expose sensitive security internals.

### Environment Configuration
- Store externalized settings in configuration files.
- Prefer properties from the environment for:
  - database connection
  - JWT settings
  - server behavior
  - runtime-specific values
- Avoid hardcoding secrets or deployment-specific values in code.

## 3) Configuration File Rules
- Keep application settings in the project’s resource configuration files.
- Group related settings logically.
- Use clear, stable names for configuration keys.
- Prefer one source of truth for each setting.
- If a setting is used in multiple layers, document it clearly.

## 4) Security-Related Configuration Rules
- Keep JWT signing, expiration, and refresh settings configurable.
- Make security decisions in configuration classes, not in controllers.
- Ensure protected routes remain protected after refactors.
- If endpoint access changes, verify related auth and exception behavior.

## 5) Bean and Dependency Configuration
- Use configuration classes for framework-managed beans.
- Keep bean definitions small and focused.
- Reuse beans where appropriate instead of creating duplicate instances.
- Avoid placing business logic inside bean configuration methods.

## 6) Startup and Initialization Rules
- Initialization logic should be deterministic and safe to run.
- Do not perform destructive startup actions unless explicitly required.
- Keep startup code idempotent where possible.
- Make sure initialization does not break tests or local development.

## 7) Profiles and Environments
- Support different runtime environments when needed.
- Separate development, test, and production concerns clearly.
- Keep secrets out of source control.
- Use environment-specific values for external services and credentials.

## 8) Error Handling in Configuration
- Fail fast when a required configuration value is missing.
- Use meaningful messages for configuration problems.
- Do not silently ignore invalid setup.
- Keep configuration failures easy to diagnose.

## 9) Agent Rules for Configuration Changes
When modifying configuration, the AI agent should:
- check whether the change affects security, startup, or environment settings
- preserve existing configuration conventions
- avoid spreading config values across unrelated files
- keep changes minimal and intentional
- verify impacts on authentication, database access, and application startup

## 10) Good Practices
- Prefer centralized configuration over scattered constants.
- Keep configuration classes focused on infrastructure concerns.
- Document any new configuration key introduced by a feature.
- Review configuration changes for side effects on the full application flow.

## 11) Goal
Configuration in this project should remain:
- clear
- centralized
- secure
- environment-friendly
- easy for both humans and AI agents to maintain