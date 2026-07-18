---
name: database-erd
description: >-
  Regenerate docs/database-erd.md from MySQL information_schema (Mermaid ERD:
  foreign keys and key columns). Use after Flyway schema changes, new
  migrations, or when the diagram is stale. Triggers on: database ERD,
  database-erd.md, export_database_erd, schema diagram, ER diagram after
  migration.
---

<objective>
Regenerate `docs/database-erd.md` from the live MySQL catalog so the Mermaid
ERD matches the current migrated schema.

Purpose: Keep the schema diagram accurate after Flyway migrations — never
hand-edit the Mermaid block.

Output: Updated `docs/database-erd.md` + summary ending with
`## ERD EXPORTED`.
</objective>

<context>
`docs/database-erd.md` is generated from MySQL `information_schema` (foreign
keys and PK/UK/FK columns). **Do not hand-edit the Mermaid block**; re-run the
exporter after schema changes.

**Run when:**
- A new or changed Flyway migration alters tables, columns, or constraints
- The ERD needs to match the current migrated schema

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …`

**Implementation:**
- Script: `scripts/export_database_erd.py`
- Output: `docs/database-erd.md`
- Omits **`flyway_schema_history`** from the diagram

**Overrides (optional):**

| Variable | Default |
|----------|---------|
| `DOUGHNUT_ERD_SCHEMA` | (auto: development → test → e2e) |
| `DOUGHNUT_ERD_MYSQL_HOST` | `127.0.0.1` |
| `DOUGHNUT_ERD_MYSQL_PORT` | `3309` |
| `DOUGHNUT_ERD_MYSQL_USER` | `doughnut` |
| `DOUGHNUT_ERD_MYSQL_PASSWORD` | `doughnut` |
</context>

<process>

<step name="ensure_mysql">
Ensure MySQL is reachable on **127.0.0.1:3309** with user **`doughnut`**
(local dev defaults from `scripts/shell_setup.sh` / `process-compose`).
</step>

<step name="ensure_migrated_schema">
Ensure at least one migrated schema exists (Flyway has run). The script picks,
in order: **`doughnut_development`**, then **`doughnut_test`**, then
**`doughnut_e2e_test`**, unless **`DOUGHNUT_ERD_SCHEMA`** is set to a
specific catalog.
</step>

<step name="export">
From the repo root:

```bash
CURSOR_DEV=true nix develop -c python3 scripts/export_database_erd.py
```

Or:

```bash
CURSOR_DEV=true nix develop -c pnpm export:database-erd
```
</step>

<step name="commit_with_migration">
Commit the updated `docs/database-erd.md` with the migration (or immediately
after).
</step>

</process>

<success_criteria>
- MySQL reachable on 127.0.0.1:3309 with user `doughnut`
- At least one migrated schema available (or `DOUGHNUT_ERD_SCHEMA` set)
- Exporter run with Nix prefix
- `docs/database-erd.md` updated and ready to commit
- Final output includes `## ERD EXPORTED`
</success_criteria>

<output>
Report a short summary to the caller, then the completion marker:

1. Schema catalog used (auto-selected or `DOUGHNUT_ERD_SCHEMA` override).
2. Whether `docs/database-erd.md` changed.

```
## ERD EXPORTED
```
</output>

<out_of_scope>
- Do not hand-edit the Mermaid block in `docs/database-erd.md`.
</out_of_scope>
