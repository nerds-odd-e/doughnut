# Milestones

## v1.3 Commissioned Learning Session (Shipped: 2026-08-08)

**Phases:** 7 | **Plans:** 14 | **Closeout:** verified (E2E `commissioned_learning_session.feature` green)

**Delivered:** Offline commissioned learning loop — assimilate as commissioned, potential sessions on recall progress bar, commission dialog with Request markdown, record Report with ADR 0003 scheduling and tutor feedback visibility. Request is ephemeral from due trackers.

**Key accomplishments:**

- `type=COMMISSIONED` memory trackers coexist with ordinary trackers on the same note and are excluded from ordinary due-recall
- Caret assimilate-as-commissioned; potential learning session rows per notebook on recall progress bar
- Copyable Request markdown; record Report applies Grades to commissioned trackers
- E2E: assimilate, potential sessions, commission, record (`commissioned_learning_session.feature`)

---

## Production hard-delete incident & prevention (Shipped: 2026-08-12)

**Incident:** `V300000245` (remove re-assimilate phase 8) ran a cascading `DELETE` on `memory_tracker`; blocked at `conversation_ibfk_4` (RESTRICT on `recall_prompt_id`). Flyway fail-stop startup crash-looped both MIG instances (`503 no healthy upstream`).

**Delivered:**

- Removed failed migration; `conversation.recall_prompt_id` → `ON DELETE SET NULL` (`V300000246`)
- Deploy gated on `/api/healthcheck`; success record only after verified probe (`docs/gcp/conditional-backend-deploy.md`)
- CI and deploy split (`deploy.yml` on green CI); deploy only latest `main` HEAD
- `DeletableEntityFkClosureTest`, ERD delete-rule labels, `db-migration.mdc` / `unit-testing.mdc` updates

**Learnings:** `migrateTestDB` on empty DB cannot validate destructive DML; orphan cleanup was unnecessary (partial unique index ignores soft-deleted rows); fire-and-forget deploy recorded unhealthy releases as successful.

**Deferred:** post-deploy API paging; normalising legacy `*_ibfk_*` constraints; re-attempting orphan cleanup.

---
