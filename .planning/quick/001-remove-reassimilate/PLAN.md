# Remove re-assimilate — PLAN

Status: planned  
Context: [CONTEXT.md](./CONTEXT.md)

## Intent

Replace re-assimilate (confirm → soft-delete tracker) with a frequent-failure **warning**. Remove remember-spelling-later wipe/re-queue (and its tests) without a spelling queue rule. Hard-delete property trackers on property removal. Remove user-facing tracker soft-delete; keep note-delete cascade. Clean orphan soft-deleted trackers on live notes. Update ADR wording.

## Design decisions

| Decision | Choice | Why |
|----------|--------|-----|
| Failure UX | Alert with live counts from API | Rule is useful; reset-learning offer is not |
| Threshold DTO | `thresholdExceeded`, `wrongCount`, `threshold`, `periodDays` | Single source of truth for copy |
| Warning copy | Live `wrongCount` + `periodDays`; property-aware | See CONTEXT O5 |
| Remember spelling later | Remove wipe + reappears tests; no queue rule; no negation test | Product drops that side effect |
| Property tracker on property delete | Hard-delete after cancellable confirm | No user-facing soft-delete |
| Tracker `deletedAt` | Keep for note delete/restore only | Cascade already works |
| Orphan soft-deleted trackers | Hard-delete when note not soft-deleted | Match new rules |
| Artifact names | Capability names (e.g. `frequent_failure_warning.feature`) | No phase numbers in product code |

## Phase sizing notes

- Target ~5 minutes wall-clock per phase (implement + targeted tests).
- E2E-led for the warning: **red** then **green** as separate Behavior phases.
- Structure only when it enables the **immediate next** Behavior (or removes dead code left by the previous Behavior).

## Phases

### Phase 1 — Structure: Enrich threshold API — **done**

**Enables:** Phase 2–3 warning copy from API fields.

**Change:** Extend `ThresholdExceededResult` / `GET …/threshold-exceeded` with `wrongCount`, `threshold`, `periodDays` (keep `thresholdExceeded`; constants 5 / 14). `MemoryTrackerService.getThresholdExceededResult()` is single source. Regenerated TS client.

**Verify:** `MemoryTrackerThresholdControllerTest` + related recall tests green; `pnpm generateTypeScript`; frontend build OK.

**Stop-safe:** Richer API only; UX unchanged.

---

### Phase 2 — Behavior: Frequent-failure warning E2E (red) — **done**

**Pre:** Learner hits ≥5 wrong answers in 14 days on a recall item.  
**Trigger:** Another wrong answer while over threshold.  
**Post:** `@wip` `frequent_failure_warning.feature` documents desired alert; fails on old re-assimilate confirm (correct red).

**Work done:** Replaced `re_assimilate.feature`; added warning step/page object; removed confirm re-assimilation helpers.

---

### Phase 3 — Behavior: Frequent-failure warning (green) — **done**

**Post:** Alert with live API counts; no softDelete/confirm. E2E `frequent_failure_warning.feature` green (no `@wip`). Unused `dueCount` deps removed from recall answer handling.

---

### Phase 4 — Behavior: Drop remember-spelling wipe — **done**

Removed `removeMemoryTrackersForReassimilation`, reappears E2E scenario, unit test, and orphaned step/page-object helpers. No negation tests.

---

### Phase 5 — Structure: Hard-delete memory tracker API — **done**

`DELETE /api/memory-trackers/{id}` hard-deletes row; `MemoryTrackerDeleteControllerTest` green; client regenerated.

---

### Phase 6 — Behavior: Property removal hard-deletes tracker — **planned**

**Pre:** Note has a property with a memory tracker.  
**Trigger:** Learner removes the property (rich or markdown) and confirms the warning.  
**Post:** Property saved without that key; tracker row hard-deleted; cancel leaves both intact.

**Work:**

- Point `usePropertyMemoryTrackerGuard` at hard-delete (shared by rich + markdown).
- Adjust confirm copy if needed (“delete that tracker” OK if accurate).
- Update guard / markdown / rich unit tests (spies → hard-delete).

**Verify:** Frontend unit tests for confirm, cancel, and hard-delete call.

**Stop-safe:** Property path no longer uses soft-delete. Soft-delete endpoint may remain until Phase 7.

---

### Phase 7 — Structure: Remove user-facing tracker soft-delete — **planned**

**Justified by:** Phase 6 removed the last product caller.

**Change:** Remove `POST …/soft-delete`, service method, and its tests. Regenerate TS client. Scrub any remaining product-code “re-assimilate” / “reassimilation” identifiers left after Phases 2–4.

**Verify:** Grep clean for soft-delete operation and re-assimilate product strings; backend/frontend tests green.

**Stop-safe:** No UX change vs Phase 6; dead API gone. Note→tracker `deletedAt` cascade untouched.

---

### Phase 8 — Behavior: Clean orphan soft-deleted trackers — **planned**

**Pre:** Live notes may still own trackers with `deleted_at` set.  
**Trigger:** Migration applies.  
**Post:** Those tracker rows hard-deleted; trackers on soft-deleted notes remain for restore.

**Work:** Flyway data migration (`DELETE` … note `deleted_at IS NULL`).

**Verify:** Pattern used elsewhere for data migrations; note restore still restores cascaded trackers.

---

### Phase 9 — Structure: ADR wording — **planned**

**Change:**

- ADR 0001: drop **Re-assimilate** from vocabulary lists.
- ADR 0003: concise frequent-failure **warning** policy; overlap excluded from the count (no “re-assimilation” wording).

**Verify:** Doc review only; no product behavior change.

*(Single Structure phase: both ADRs are terminal doc alignment, ~one edit pass.)*

---

## Testing notes

- E2E: capability-named; `@wip` only while red; `--spec` for touched features.
- Unit: high-level boundaries; small-test style.
- After each phase: cleanup, targeted tests green, plan status, commit+push (execute-plan wrap-up).

## Out of scope

- Changing 5 / 14 constants
- Skip/revive UX redesign
- Dropping `memory_tracker.deleted_at` / note-cascade redesign
- Missing-spelling assimilation queue
- Negation tests for assimilation count after enabling remember spelling

## Refinement summary (vs prior 7-phase draft)

| Was | Problem | Now |
|-----|---------|-----|
| Phase 2 (warning + E2E + units together) | >1 behavior / likely >5–10 min | Red E2E → green implement |
| Phase 4 (hard-delete API + UI) | Structure mixed into Behavior | API Structure → UI Behavior |
| Phase 5 (soft-delete + name scrub + regen) | Bundled after multiple concerns | Soft-delete removal right after last caller; light scrub only |
| Phase 3 (wipe) | OK size | Unchanged intent; clearer “delete tests + wipe together” |
| ADRs | OK | Kept as one terminal Structure phase |
