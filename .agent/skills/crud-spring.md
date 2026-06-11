## CRUD Skill Rules for the AI Agent

### CRUD Understanding
- When implementing CRUD features, always identify the full flow:
  - request DTO
  - validation
  - controller endpoint
  - service logic
  - repository access
  - entity mapping
  - response DTO
- Do not implement CRUD in a single layer unless the project already does so.
- Follow the project’s existing architecture and naming style.

### Create
- Validate incoming data before saving.
- Check required fields, business constraints, and uniqueness rules before insert.
- Use request DTOs for create operations.
- Map request data to the entity in a dedicated mapper or service layer, depending on the project pattern.
- Return a clean response DTO after creation.

### Read
- Support both single-item read and list read patterns when needed.
- Prefer response DTOs instead of returning entities directly.
- If pagination or filtering already exists in the project, follow the same style.
- Keep read queries efficient and avoid unnecessary loading of related data.

### Update
- Fetch the existing record first before updating.
- Update only allowed fields.
- Preserve immutable fields such as identifiers and audit-related values.
- Validate update data before applying changes.
- If partial update is used in the project, follow the same approach consistently.

### Delete
- Before deleting, check whether the record is allowed to be removed.
- Respect business rules such as dependent records, status restrictions, or soft-delete behavior if already used in the project.
- Use hard delete only if the project already follows that pattern.
- After delete, return a consistent success response.

### CRUD Safety Rules
- Never skip validation just to make CRUD faster.
- Never expose database entities directly unless the project already relies on that pattern.
- Always update related mapper, DTO, service, and repository code together.
- Review security permissions for every create, update, and delete endpoint.
- Keep error handling consistent with existing exception patterns.

### CRUD Implementation Checklist
Before finalizing any CRUD feature, confirm:
- [ ] Request DTO exists
- [ ] Validation is applied
- [ ] Controller endpoint follows existing route style
- [ ] Service contains the business logic
- [ ] Repository supports the required query
- [ ] Entity mapping is correct
- [ ] Response DTO is returned
- [ ] Exceptions and edge cases are handled
- [ ] Tests are added or updated

### CRUD Behavior Priority
When designing CRUD behavior, prefer:
1. Existing project conventions
2. Business correctness
3. API consistency
4. Minimal and safe code changes

### CRUD Goal
The AI agent should implement CRUD features that are clean, consistent, secure, and easy to maintain within this backend project.