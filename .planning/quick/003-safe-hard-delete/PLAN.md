# Safe hard delete — PLAN

Status: in progress (Phases 1–2 done; Phases 3–8 planned)

## Incident that motivated this plan

`V300000245__hard_delete_orphan_soft_deleted_memory_trackers.sql` deletes from `memory_tracker`.
Three tables cascade off it (`session_item`, `question_generation_batch_request`, `recall_prompt`).
One table hangs off `recall_prompt` with **no** `ON DELETE` clause, so `RESTRICT`:

```
CONSTRAINT `conversation_ibfk_4` FOREIGN KEY (`recall_prompt_id`) REFERENCES `recall_prompt` (`id`)
```

The delete cascades into `recall_prompt` and is blocked there (MySQL 1451). Flyway runs on
`ApplicationReadyEvent` in every non-test profile, so startup aborts; `repair()` clears the failed
marker before each retry, so every restart re-fails. Both MIG instances are RUNNING with no JVM and
`/api/healthcheck` returns `503 no healthy upstream`.

Nothing in the pipeline could have caught it: `migrateTestDB` runs migrations against an **empty**
database (the `DELETE` matches zero rows), and the migration's unit test built a tracker with no
children, so it verified row *selection* rather than referential *fan-out*.

The same defect is live in product code: `MemoryTrackerService.delete()` is a bare
`entityPersister.remove(...)` with no JPA mapping to `recall_prompt`, so it relies on the same DB
cascade and hits the same wall. Any user deleting a property tracker whose recall prompt has a
conversation gets a 500.

## Intent

Restore service, then remove the *class* of mistake rather than defend against it, and add one
structural CI guard so a future blocking foreign key fails the build. Separately: make deploy
verification real without permanently inflating **CI duration** (a measured metric).

## Design decisions

| Decision | Choice | Why |
|---|---|---|
| Restore production | Delete the migration, not fix-forward | The cleanup was cosmetic (see below); `repair()` drops the failed row and marks the deleted migration, so `migrate()` has nothing to retry |
| Was the cleanup needed? | No | `user_note_spelling_active` is UNIQUE on `(user_id, note_id, type, property_key, (if(deleted_at is null,1,NULL)))`. The expression is NULL for soft-deleted rows and MySQL unique indexes ignore NULLs, so an orphan soft-deleted tracker never occupies an active slot. The 67 rows are inert. |
| `conversation.recall_prompt_id` policy | `ON DELETE SET NULL` | A conversation is user-authored content; a memory tracker is a scheduling artifact. Deleting the artifact must not destroy the chat. `CASCADE` is technically viable (`conversation_message` already cascades) but discards user history. **Needs developer confirmation.** |
| Autonomation shape | Schema-structural invariant, no fixtures | One `information_schema` query plus a graph walk. Nothing to rot, immune to unrelated feature work, fires only when a restricting FK enters a deletable subtree |
| Invariant scope | Declared delete roots only, starting with `memory_tracker` | Narrow and concrete first; broaden after a second root actually appears |
| Guard-test ordering | After the FK fix | The invariant is red against today's schema; keep at most one intentionally failing test, and never end a phase red in CI |
| Health probe vs CI time | Interim probe in CI Deploy (Phase 2); then move deploy out of CI (Phase 3) | Fire-and-forget kept CI short but recorded crash-looping releases as success. A real gate **must** wait on `/api/healthcheck`. **CI duration is a measured metric**, so that wait must not permanently sit on the CI critical path. |
| Deploy vs CI | Separate workflows; deploy consumes CI-built artifacts | Building the jar/SPA/CLI stays a CI check (and can stay parallel with tests). GCS + MIG + health wait belong to deploy. Deploy failure must not read as “CI failed.” |
| Superseded commits | Cancel in-progress CI on `main`; deploy only if SHA is still `origin/main` HEAD | Avoid stacked builds and deploys when commits land faster than the pipeline. Prefer a HEAD guard over cancelling mid–rolling-replace (half-updated MIG). |
| Artifact names | Capability names (`DeletableEntityFkClosureTest`) | Phase numbers stay under `.planning/` |

## FK closure under `memory_tracker` (verified against the migrations)

| Edge | Delete rule | Effect |
|---|---|---|
| `session_item` → `memory_tracker` | CASCADE | leaf, nothing references it |
| `question_generation_batch_request` → `memory_tracker` | CASCADE | leaf |
| `recall_prompt` → `memory_tracker` | CASCADE | extends the closure |
| `conversation` → `recall_prompt` | **NO ACTION** | **the only blocker** |
| `conversation_message` → `conversation` | CASCADE | only reachable if `conversation` cascaded |

Six FKs in the schema are auto-named `*_ibfk_N` (created without a delete rule, so `RESTRICT`);
forty-four are explicitly named `fk_*` and mostly `ON DELETE CASCADE`. The trap sits on one of the
six implicit ones, which is why "deletes cascade in this schema" felt true and wasn't.

## Autonomation (jidoka) in this plan

Machine stops that fail the pipeline or refuse a bad release — not docs, not manual checks:

| Phase | Autonomation | What it stops |
|---|---|---|
| **2** | Post-rollout `/api/healthcheck` probe; `last-successful-deploy.json` only after OK | Recording a crash-looping release as successful; skipping redeploy of the same jar |
| **3** | Same probe, owned by a **deploy** workflow (not CI) | Same gate without permanently inflating measured **CI duration** |
| **4** | CI `concurrency` cancel-in-progress; deploy HEAD guard + serialized deploy | Deploying an older SHA after a newer `main` commit; stacked overlapping deploys |
| **5** | Controller regression test: hard-delete tracker with conversation on its recall prompt | Shipping a delete path that 500s on real FK data (this incident’s product bug) |
| **6** | `DeletableEntityFkClosureTest` — `information_schema` FK closure over declared delete roots | Adding a `RESTRICT`/`NO ACTION` FK into a hard-deletable subtree (or a new delete root) without CI going red |

Not autonomation (steer humans/agents, no pipeline stop): Phase 7 ERD delete-rule labels; Phase 8 rule updates.

## Phase sizing notes

- Target ~5 minutes wall-clock per phase including targeted tests (workflow phases may run longer in CI).
- Jidoka stop before Phase 1 (restore strategy) and Phase 5 (user-data policy).
- Phase 2 lands before product FK work so later deploys are not recorded successful while unhealthy.
- Phase 3 exists so Phase 2’s health wait does not permanently inflate the CI-duration metric.

## Phases

### Phase 1 — Behavior: Production API serves again — **done**

**Done:** Deleted `V300000245__hard_delete_orphan_soft_deleted_memory_trackers.sql` and
`OrphanSoftDeletedMemoryTrackerCleanupMigrationTest`. Backend unit tests green. Production
`/api/healthcheck` still 503 until this commit deploys (expected).

**Learning:** Removal confirmed over fix-forward (execute-plan go-ahead). Orphan soft-deleted
trackers remain; inert under the unique index (see Design decisions).

---

### Phase 2 — Behavior: An unhealthy release fails the deploy and is not recorded as successful — **done**

**Done:** `deploy-backend-jar-to-gcp-mig.sh` calls `app-instance-healthcheck.sh` after rollout; success record only after OK. Healthcheck URL fixed (`doughnut.odd-e.com`), loop shortened (~3 min), env overrides for tests. Shell tests added/extended.

---

### Phase 3 — Structure: Deploy is a separate workflow; CI no longer waits on production health — **planned**

**Justified by:** Phase 2’s probe must stay. Leaving it inside `ci.yml` permanently inflates **CI
duration**, which we measure. Build/package must remain a CI check; the health wait must not.

**Pre:** `Deploy` is a job in `ci.yml` that `needs` tests + `Package-artifacts`. Frontend/CLI are
uploaded to GCS inside `Package-artifacts` (before Deploy). Backend jar already flows as a GitHub
Actions artifact into Deploy.

**Trigger:** CI succeeds on `main`.

**Post:**

- CI ends when checks + artifact packaging succeed (no health wait on the CI critical path).
- A separate deploy workflow downloads those artifacts, does GCS + MIG rolling replace, runs the
  Phase 2 health probe, then writes `last-successful-deploy.json`.
- A failed health probe fails **deploy**, not CI.

**Change:** New workflow (e.g. `deploy.yml`) triggered by successful CI on `main` (`workflow_run` or
equivalent). Move GCS uploads + MIG + probe + success-record into it. CI `Package-artifacts` keeps
building jar/SPA/CLI and uploading **GitHub Actions artifacts** (build remains the check; stays
parallel with tests). Remove the in-CI Deploy job (and any GCS side-effects that force deploy work
into the CI metric).

**Verify:** A green CI run does not include the probe wait in its wall-clock; a deliberate bad jar
(or probe against a bogus host in a dry path) fails the deploy workflow only.

**Stop-safe:** Same deploy gate as Phase 2; CI duration returns to “checks + package” only.

---

### Phase 4 — Behavior: Only the latest `main` commit is deployed — **planned**

**Pre:** Rapid pushes to `main` can stack overlapping CI/deploy runs; an older successful CI may still
deploy after a newer commit landed.

**Trigger:** Multiple commits land on `main` before earlier pipelines finish.

**Post:** Superseded CI runs are cancelled; a deploy starts only if its SHA is still `origin/main`
HEAD (otherwise exits 0 / skips). At most one deploy runs at a time.

**Change:**

- CI `concurrency` on `main` with `cancel-in-progress: true`.
- Deploy concurrency (serialize; do **not** cancel mid–rolling-replace).
- Deploy-start HEAD guard: if `git rev-parse origin/main` ≠ this run’s SHA, skip.

**Verify:** Two quick commits to `main` — only the newer CI finishes; deploy for an older SHA skips
when HEAD has moved.

**Stop-safe:** No product change; less wasted runner time; avoids older jars overwriting newer ones.

---

### Phase 5 — Behavior: Deleting a tracker whose recall prompt has a conversation succeeds — **planned**

**Pre:** `DELETE /api/memory-trackers/{id}` returns 500 (MySQL 1451) when the tracker's recall prompt
is referenced by a conversation. Shipped live in `a74a00028a`.

**Trigger:** A user removes such a property tracker.

**Post:** Tracker and its recall prompts are gone; the conversation survives with
`recall_prompt_id` NULL.

**Change:** New migration replacing `conversation_ibfk_4` with an explicitly named constraint using
`ON DELETE SET NULL`. Regression test in `MemoryTrackerDeleteControllerTest` built with
`makeMe.aConversation().forARecallPrompt(...)` — that builder already exists and currently has **zero**
callers in the whole backend suite, which is why nothing demonstrated the blocking shape.

**Verify:** Targeted backend test; confirm a conversation with neither `note_id` nor `recall_prompt_id`
still renders (`Conversation` already null-checks `getRecallPrompt()`; check the `getSubject().isEmpty()`
paths and any frontend assumption that a conversation always has a subject).

**Jidoka before:** `SET NULL` versus `CASCADE` is a product judgment about user chat history.

**Stop-safe:** Strictly widens which deletes are legal; no existing delete changes outcome.

---

### Phase 6 — Structure: CI fails when a foreign key blocks deleting a hard-deletable entity — **planned**

**Justified by:** Locks in the invariant Phase 5 establishes, for the tables Phase 5 did not touch and
for tables that do not exist yet.

**Change:** `DeletableEntityFkClosureTest`. Declares the entities the product hard-deletes (start:
`memory_tracker`), reads `information_schema.REFERENTIAL_CONSTRAINTS` from the already-migrated test
DB, walks the transitive closure (CASCADE edges extend it, SET NULL edges terminate it), and asserts
no edge in the closure is `NO ACTION` / `RESTRICT`.

Failure message names the path, e.g.
`memory_tracker -> recall_prompt (CASCADE) -> conversation.recall_prompt_id [conversation_ibfk_4] NO ACTION`.

**Why this shape:** no fixtures, no rows, one query. Unrelated feature work cannot move it; it fires
only when someone adds a restricting FK into a deletable subtree, or declares a new delete root. One
assertion covers both the migration and the runtime endpoint.

**Escape hatch:** a deliberate `RESTRICT` goes in a documented allowlist entry with a reason, so it is
a conscious stop rather than a silent one.

**Verify:** Green against the Phase 5 schema. Confirm it genuinely catches the incident by reverting
Phase 5's migration locally and watching it go red — do not commit that revert.

**Stop-safe:** Test-only; no external behavior change.

---

### Phase 7 — Behavior: The ERD distinguishes blocking foreign keys from cascading ones — **planned**

**Pre:** `docs/database-erd.md` renders every FK identically. The edge
`recall_prompt ||--o{ conversation : "recall_prompt_id"` was already in the diagram and gave no hint
that it would block a delete — the one artifact that shows the graph omitted the part that mattered.

**Trigger:** `CURSOR_DEV=true nix develop -c pnpm export:database-erd`.

**Post:** Every edge label carries its delete rule.

**Change:** `scripts/export_database_erd.py` already joins `information_schema.REFERENTIAL_CONSTRAINTS`;
add `r.DELETE_RULE` to the FK query and to the edge label. Regenerate the doc.

**Verify:** Generated diff shows the `conversation` edge as SET NULL and the cascades labelled;
Mermaid still renders.

**Stop-safe:** Generated doc plus generator; no product code.

---

### Phase 8 — Structure: Rules steer developers and agents away from the mistake — **planned**

**Justified by:** Phases 1–7 fix this instance and guard this subtree. The rules are what stop the next
one from being written, including by an agent that never sees this plan.

**Change:**

- `db-migration.mdc` — destructive-DML section. One-off data cleanup does not belong in the permanent
  Flyway chain. If it must ship as a migration, use the placeholder gate that `V300000233`–`V300000235`
  use (`${tz_repair}`: a no-op in every environment by default, enabled only via a system property on
  the production deploy) and record the production row count first. Before shipping any `DELETE`, walk
  the FK delete closure. Note that `migrateTestDB` runs against an empty database, so CI cannot
  validate DML on its own.
- `unit-testing.mdc` — carve-out for destructive operations: for hard deletes and delete migrations,
  fixture *completeness* beats minimality; build a row in every table holding an FK into the target.
  The existing concise-fixture guidance ("rely on builder defaults", "do not create fixtures only to
  satisfy wiring") is correct for behavior tests and actively steered this change away from the bug.

**Verify:** Doc review. Each rule names the concrete artifact a future agent would reach for.

**Stop-safe:** Docs only.

## Deferred, with reasons

- **Post-deploy / ongoing API paging** (probe `/api/healthcheck` on a short schedule; fix
  `mig_status_check.yml` which only asserts MIG `isStable`). Out of scope: this plan covers release
  gates and delete-safety, not continuous uptime monitoring.
- **Broadening the FK invariant to the whole schema** (normalising the six implicit `*_ibfk_*`
  constraints). Valuable, but speculative until a second delete root exists — generalize after real
  repetition, not before.
- **Attaching MIG autohealing.** It would have recreated crash-looping instances indefinitely without
  paging anyone; needs a real pager first.
- **Re-attempting the orphan cleanup.** Evidence says the rows are inert. If a concrete harm appears,
  it is trivial after Phase 5.
- **Splitting data cleanup out of fail-stop startup migration.** Subsumed by the Phase 8 rule: do not
  ship one-off cleanup as a permanent migration. No code change needed.

## Process learning

The planning history for the original work was pruned 35 seconds after the migration commit, before
production had confirmed anything. "Fully executed into code" should mean verified in production, not
committed.

Fire-and-forget deploy kept CI wall-clock short (a measured metric) at the cost of recording unhealthy
releases as successful. The fix is a real health gate **and** keeping that wait off the CI critical
path (Phases 2 then 3), not dropping the gate to protect the metric.
