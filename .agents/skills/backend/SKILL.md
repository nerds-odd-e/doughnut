---
name: backend
description: Backend Java — Spring Boot controllers/services, JUnit controller tests, MakeMe. Read this first for any backend work. Use when working on the Java Spring backend.
paths:
  - "backend/**"
---
# Backend

Java/Spring in `backend/src/main/java/com/odde/donut/`. Tests under `backend/src/test/`. Schema changes: `db-migration` skill.

Matching `backend-code` (production Java) and `backend-testing` (tests) skills attach via `paths` when those files are in context — do not fetch them up front.

## Production

- Controllers: return entities (or existing API body types). Narrow with `@JsonIgnore` / `@JsonView`. Introduce a response DTO only when the wire shape differs.
- Top-of-file imports. No inline fully qualified class names except when two types share a simple name.
- After Flyway schema changes, regenerate `docs/database-erd.md` with the `database-erd` skill (`pnpm export:database-erd`).
- Note collections name their trash semantics. `NoteRepository.findAllByNotebookIdOrderByIdAsc` is the complete stored content, trash included (export, Git snapshots, learning-session title recognition); `findAvailableNotesByNotebookIdOrderByIdAsc` and the `Note.JPA_AVAILABLE` / `NATIVE_AVAILABLE` predicates select participating notes (search, recall, health rules); `findOccupiedFolderIdsByNotebookId` counts stored content for folder occupancy. On a loaded `Note`, `isTrashed()` follows current folder ancestry and `isTrashedInDatabase()` is the persisted membership as loaded. Root `_trash` membership is owned by `Folder.isTrashed` and the `trashed_folder` view; do not recreate the rule in callers.

## Tests

See [backend testing](../backend-testing/SKILL.md) for verification commands,
controller and database setup, and Java fixtures.
