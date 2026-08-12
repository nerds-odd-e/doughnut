# Safe hard delete — PLAN

Status: **complete** (8/8 phases)

## Outcome

Production outage from `V300000245` (cascading `DELETE` blocked by `conversation_ibfk_4`) is resolved and the class of mistake is prevented:

- Failed migration removed; `conversation.recall_prompt_id` is `ON DELETE SET NULL`
- Deploy gated on `/api/healthcheck`; success record only after OK
- CI and deploy split; deploy only latest `main` HEAD
- `DeletableEntityFkClosureTest` + ERD delete-rule labels + rule updates

## Autonomation shipped

| Mechanism | What it stops |
|---|---|
| Post-rollout health probe + success record | Crash-looping release recorded as successful |
| Separate deploy workflow | Health wait inflating CI duration metric |
| CI cancel-in-progress + deploy HEAD guard | Older SHA deploying after newer commit |
| `MemoryTrackerDeleteControllerTest` (conversation on prompt) | Product hard-delete 500 on real FK data |
| `DeletableEntityFkClosureTest` | New `RESTRICT`/`NO ACTION` in hard-deletable subtree |

## Phases (all done)

1. **Production API serves again** — removed `V300000245` and its ClassPathResource test
2. **Unhealthy release fails deploy** — health probe before `last-successful-deploy.json`
3. **Separate deploy workflow** — `deploy.yml`; CI packages artifacts only
4. **Deploy only latest** — CI concurrency; deploy serialized + `main-head-guard`
5. **Tracker delete with conversation** — `V300000246` SET NULL + controller regression test
6. **FK closure CI guard** — `DeletableEntityFkClosureTest`
7. **ERD delete rules** — `export_database_erd.py` labels edges with `ON DELETE`
8. **Rule updates** — `db-migration.mdc`, `unit-testing.mdc`

## Deferred

- Post-deploy API paging (`mig_status_check.yml` still checks MIG stability only)
- Normalising legacy `*_ibfk_*` constraints across whole schema
- Re-attempting orphan soft-deleted tracker cleanup (rows are inert under unique index)

## Learnings

- `migrateTestDB` on empty DB cannot validate destructive DML; structural FK closure test + complete fixtures required
- Fire-and-forget deploy protected CI duration but lied about success — gate must live in deploy, not be dropped
- "Fully executed" means verified in production, not merely committed
