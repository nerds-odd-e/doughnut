---
id: SEED-067
status: dormant
planted: 2026-10-11
planted_during: owner follow-up to the readiness-race reminder from the SEED-066 spoken-title preparation landed at 7d062d701f
trigger_when: starting an isolated E2E session against a fresh worktree database
scope: small
---

# SEED-067: Start isolated E2E sessions reliably on a fresh database

## Why This Matters

Donut contributors need the first `pnpm e2e:hold` or `pnpm cy:run` in a new
worktree to produce a usable session or trustworthy test result. The reported
startup race can interrupt database migration and leave the disposable database
unusable until the contributor manually drops and recreates it. Reliable E2E
startup supports the current voice-input work and other product development.

## Alternatives and Decision

The reminder identifies two alternatives: have the runner wait for completed
migration, or have the application healthcheck report not-ready until migration
finishes. Terry owns this decision. Neither alternative has been selected;
refinement should establish the intended readiness boundary before slice planning.

## Story Decomposition

<a id="start-e2e-after-migrations"></a>

### Start E2E sessions after database migration completes

**Identity:** SEED-067#start-e2e-after-migrations
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["Terry must choose migration-aware runner readiness or healthcheck readiness gated on completed migration.","Execution approach and focused reproduction proof have not been selected."],"basis":{"document":"ebb9f5799e3ec3f85e43712f6af499b83566d8e54613e2646ae665a3d85e7c21"}}
```

**Goal:** For Donut contributors using a new worktree, the first E2E invocation
waits for database migration to finish before resetting and seeding test data,
so manual database recreation is unnecessary for an otherwise successful startup.

**Scope:**

- Coordinate readiness and testability reset for `pnpm e2e:hold` and `pnpm cy:run`
  when their isolated database is fresh.
- Keep migration failure visible as startup failure; a failed migration cannot
  produce a usable E2E session or a successful test run.
- Preserve the runner's ownership and shutdown of its services and isolation
  from other worktrees and the primary development database.
- Automatic repair of databases already left partially migrated and reclamation
  of other retired worktree databases are deferred.

**Reported actual behavior and evidence:**

- Source: the owner-supplied reminder after spoken-title preparation landed on
  `origin/main` at `7d062d701f32fe487152d6414792f6b9df095039` and its worktree
  was removed.
- The reminder reports that, in non-test profiles, Flyway runs on the
  application-ready event after the healthcheck already answers. The E2E
  runner's testability reset can consequently reach a fresh database during
  migration.
- Stopping that failed stack can leave the database partially migrated. A later
  invocation refuses to rebuild it, requiring a manual drop and recreation.
- This follow-up records the supplied observation; it has not reproduced the
  race or repaired the runner. No original command transcript or failure log
  was included in the reminder.

**Key examples:**

- A new worktree has an empty isolated database and migration is still running
  when the application starts answering HTTP requests → `pnpm e2e:hold` waits
  for completed migration, resets and seeds the database, and opens a usable
  browser session without manual database recreation.
- Under the same delayed-migration setup → `pnpm cy:run --spec <feature>` waits
  for completed migration before reset and runs the selected feature against
  the fully migrated, seeded database.
- Migration fails during startup → the invocation reports the migration/startup
  failure and exits unsuccessfully instead of reporting a usable session or a
  passing test run.

- **Evaluation:** Observe the real invocation boundary with a fresh database
  and migration delayed past HTTP availability. Show the held session and the
  selected Cypress run both reach their promised outcome, and retain focused
  regression proof for the chosen readiness boundary.
- **Value / learning:** Establish what readiness must mean before the runner
  may reset test data, and remove manual recovery from ordinary first startup.
- **Effort hypothesis:** S (30–60 minutes), low confidence until the readiness
  decision and a deterministic reproduction establish the affected boundary.
- **Depends on:** No blocking story dependency.
- **Safe stopping point:** Reliable first startup is useful independently of
  broader runner improvements; preserve service ownership and database isolation.

**Remaining uncertainty / owner response:** Terry selects migration-aware
readiness in the runner or application healthcheck readiness gated on completed
migration. The implementation must first confirm the reported ordering with a
reproduction and determine the focused proof for that choice.

## Ordering and Scope Reduction

Queue this single bounded defect story first under dough-bug-fixing's
remaining-work rule. Preserve the other queued story's order and scope.

## Open Decisions

The readiness-boundary choice belongs to Terry, as recorded in the story above.

## When to Surface

Before the next fresh-worktree E2E startup or when refining this queued defect.

## Breadcrumbs

- Owner-supplied readiness-race reminder associated with
  [spoken-title preparation](SEED-066-voice-input.md#unobtrusive-selection-aware-spoken-title).
- [Isolated browser testing](../../docs/worktree-browser-tests.md).
- [Disposable database retirement](../../docs/worktree-retire-databases.md).
