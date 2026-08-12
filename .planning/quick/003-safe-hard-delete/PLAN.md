# Safe hard delete — PLAN

Status: in progress (Phase 1 done; Phases 2–7 planned)

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
structural CI guard so a future blocking foreign key fails the build.

## Design decisions

| Decision | Choice | Why |
|---|---|---|
| Restore production | Delete the migration, not fix-forward | The cleanup was cosmetic (see below); `repair()` drops the failed row and marks the deleted migration, so `migrate()` has nothing to retry |
| Was the cleanup needed? | No | `user_note_spelling_active` is UNIQUE on `(user_id, note_id, type, property_key, (if(deleted_at is null,1,NULL)))`. The expression is NULL for soft-deleted rows and MySQL unique indexes ignore NULLs, so an orphan soft-deleted tracker never occupies an active slot. The 67 rows are inert. |
| `conversation.recall_prompt_id` policy | `ON DELETE SET NULL` | A conversation is user-authored content; a memory tracker is a scheduling artifact. Deleting the artifact must not destroy the chat. `CASCADE` is technically viable (`conversation_message` already cascades) but discards user history. **Needs developer confirmation.** |
| Autonomation shape | Schema-structural invariant, no fixtures | One `information_schema` query plus a graph walk. Nothing to rot, immune to unrelated feature work, fires only when a restricting FK enters a deletable subtree |
| Invariant scope | Declared delete roots only, starting with `memory_tracker` | Narrow and concrete first; broaden after a second root actually appears |
| Guard-test ordering | After the FK fix | The invariant is red against today's schema; keep at most one intentionally failing test, and never end a phase red in CI |
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

## Phase sizing notes

- Target ~5 minutes wall-clock per phase including targeted tests.
- Jidoka stop before Phase 1 (restore strategy) and Phase 3 (user-data policy).
- Phase 2 lands before Phase 3 so every later deploy is verified before it is recorded as good.

## Phases

### Phase 1 — Behavior: Production API serves again — **done**

**Done:** Deleted `V300000245__hard_delete_orphan_soft_deleted_memory_trackers.sql` and
`OrphanSoftDeletedMemoryTrackerCleanupMigrationTest`. Backend unit tests green. Production
`/api/healthcheck` still 503 until this commit deploys (expected).

**Learning:** Removal confirmed over fix-forward (execute-plan go-ahead). Orphan soft-deleted
trackers remain; inert under the unique index (see Design decisions).

---

### Phase 2 — Behavior: An unhealthy release fails the deploy and is not recorded as successful — **planned**

**Pre:** `deploy-backend-jar-to-gcp-mig.sh` writes `last-successful-deploy.json` immediately after
issuing the rolling replace, with no health probe in between. That record is also the skip condition
for later deploys, so a crash-looping release becomes the baseline and blocks redeploying the same jar.

**Trigger:** The Deploy job runs.

**Post:** The job probes `/api/healthcheck` after the rollout, goes red if it never returns `OK`, and
writes the success record only after a verified probe.

**Change:** Wire a health probe into the Deploy job after the rolling replace; move the record write
behind it. Fix `app-instance-healthcheck.sh`: it points at `dough.odd-e.com`, which does not resolve,
so the existing recovery tool always reports NOT RESPONDING after a 30 × 20s (10 minute) loop. Point
it at `doughnut.odd-e.com` and shorten the loop.

**Verify:** Run the script against production (expect `OK` after Phase 1) and against a bogus host
(expect non-zero exit).

**Stop-safe:** No product change. Protects the deploys in every later phase.

---

### Phase 3 — Behavior: Deleting a tracker whose recall prompt has a conversation succeeds — **planned**

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

### Phase 4 — Structure: CI fails when a foreign key blocks deleting a hard-deletable entity — **planned**

**Justified by:** Locks in the invariant Phase 3 establishes, for the tables Phase 3 did not touch and
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

**Verify:** Green against the Phase 3 schema. Confirm it genuinely catches the incident by reverting
Phase 3's migration locally and watching it go red — do not commit that revert.

**Stop-safe:** Test-only; no external behavior change.

---

### Phase 5 — Behavior: The ERD distinguishes blocking foreign keys from cascading ones — **planned**

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

### Phase 6 — Structure: Rules steer developers and agents away from the mistake — **planned**

**Justified by:** Phases 1–5 fix this instance and guard this subtree. The rules are what stop the next
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

---

### Phase 7 — Behavior: A dead API pages within minutes — **planned**

**Pre:** `mig_status_check.yml` asserts `status.isStable` on a 6-hourly schedule. A dead JVM on a
running VM is perfectly stable, so this outage was silent. `add-mig-autohealing.sh` documents the trap
verbatim — *"a dead JVM leaves the VM running and isStable:true, causing silent 502s"* — and is a
manual one-shot that the observed instance state says is not attached.

**Trigger:** The API stops serving.

**Post:** The scheduled check probes `/api/healthcheck`, runs on a short interval, and alerts.

**Verify:** Run the check body against production (green) and against a bogus host (red, and the
notification path fires).

**Stop-safe:** Monitoring only.

## Deferred, with reasons

- **Broadening the FK invariant to the whole schema** (normalising the six implicit `*_ibfk_*`
  constraints). Valuable, but speculative until a second delete root exists — generalize after real
  repetition, not before.
- **Attaching MIG autohealing.** It would have recreated crash-looping instances indefinitely without
  paging anyone. Phase 7 is the honest fix.
- **Re-attempting the orphan cleanup.** Evidence says the rows are inert. If a concrete harm appears,
  it is trivial after Phase 3.
- **Splitting data cleanup out of fail-stop startup migration.** Subsumed by the Phase 6 rule: do not
  ship one-off cleanup as a permanent migration. No code change needed.

## Process learning

The planning history for the original work was pruned 35 seconds after the migration commit, before
production had confirmed anything. "Fully executed into code" should mean verified in production, not
committed.
