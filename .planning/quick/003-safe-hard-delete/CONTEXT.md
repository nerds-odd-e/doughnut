# Safe hard delete — incident & prevention (shipped)

Shipped 2026-08-12. See `PLAN.md` for phase log. Permanent ops detail: `docs/gcp/conditional-backend-deploy.md`.

## Incident (2026-08-12)

Production API down: `GET https://doughnut.odd-e.com/api/healthcheck` returned `503 no healthy upstream`. Both MIG instances RUNNING with no JVM.

**Root cause:** Flyway migration `V300000245__hard_delete_orphan_soft_deleted_memory_trackers.sql` (from [remove re-assimilate](../001-remove-reassimilate/) phase 8) ran:

```sql
DELETE mt FROM memory_tracker mt
INNER JOIN note n ON n.id = mt.note_id
WHERE mt.deleted_at IS NOT NULL AND n.deleted_at IS NULL;
```

Cascade: `memory_tracker` → `recall_prompt` (CASCADE) → blocked at `conversation.recall_prompt_id` (`conversation_ibfk_4`, implicit RESTRICT). MySQL 1451. Startup runs `repair()` then `migrate()` on every boot, so instances crash-looped.

**Why CI missed it:** `migrateTestDB` uses an empty database (DELETE matches zero rows). Migration test built a tracker with no children — verified row selection, not FK fan-out.

**Was the cleanup needed?** No. Partial unique index `user_note_spelling_active` ignores NULL `deleted_at` expressions; orphan soft-deleted trackers never block active slots.

## Response

| Action | Result |
|---|---|
| Remove `V300000245` | Production recovers on deploy (`repair()` drops failed row) |
| `V300000246` — `conversation.recall_prompt_id ON DELETE SET NULL` | Hard-delete tracker legal when conversation references prompt |
| Deploy health gate | `last-successful-deploy.json` only after `/api/healthcheck` OK |
| Split CI / deploy | Health wait off measured CI duration |
| `DeletableEntityFkClosureTest` | CI fails on RESTRICT in hard-deletable subtree |
| ERD + rules | Delete rules on edges; `db-migration.mdc` / `unit-testing.mdc` carve-outs |

## Autonomation

| Guard | Stops |
|---|---|
| Post-rollout health probe | Recording crash-looping release as successful |
| Separate deploy workflow | Inflating CI duration metric with health wait |
| CI concurrency + deploy HEAD guard | Older SHA deploying after newer commit |
| Delete controller test (conversation on prompt) | Product 500 on real FK data |
| `DeletableEntityFkClosureTest` | New blocking FK without CI red |

## Learnings

- Destructive DML in Flyway on fail-stop startup can take down prod for a cosmetic cleanup.
- "Deletes cascade in this schema" was false at one legacy `*_ibfk_*` edge among forty-four explicit `fk_*` cascades.
- Fire-and-forget deploy protected CI wall-clock but recorded unhealthy releases as successful.
- Fixture minimality (`unit-testing.mdc`) steered migration tests away from the blocking shape.
- "Fully executed" should mean verified in production, not merely committed (001 plan pruned 35s after migration commit).

## Deferred

- Post-deploy API paging (`mig_status_check.yml` checks MIG stability only)
- Normalising all six legacy `*_ibfk_*` constraints
- Re-attempting orphan soft-deleted tracker cleanup
