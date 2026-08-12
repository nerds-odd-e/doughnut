# Safe hard delete — PLAN

Status: complete  
Context: [CONTEXT.md](./CONTEXT.md)  
Historical summary: [MILESTONES.md](../../MILESTONES.md) (2026-08-12 entry)

## Phases (all done)

1. Production API serves again — removed `V300000245`
2. Unhealthy release fails deploy — health probe before success record
3. Separate deploy workflow — `deploy.yml`
4. Deploy only latest — CI concurrency + `main-head-guard`
5. Tracker delete with conversation — `V300000246` SET NULL + regression test
6. FK closure CI guard — `DeletableEntityFkClosureTest`
7. ERD delete rules — `export_database_erd.py`
8. Rule updates — `db-migration.mdc`, `unit-testing.mdc`
