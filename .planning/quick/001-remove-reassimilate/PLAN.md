# Remove re-assimilate — PLAN

Status: complete  
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

### Phase 6 — Behavior: Property removal hard-deletes tracker — **done**

Property guard uses `MemoryTrackerController.delete`; markdown + rich unit tests updated (shipped with phase 5 API commit).

---

### Phase 7 — Structure: Remove user-facing tracker soft-delete — **done**

Removed `/soft-delete` endpoint, service method, and related tests; regenerated client.

---

### Phase 8 — Behavior: Clean orphan soft-deleted trackers — **done**

`V300000245` hard-deletes orphan soft-deleted trackers on live notes; migration test added.

---

### Phase 9 — Structure: ADR wording — **done**

ADR 0001 drops Re-assimilate; ADR 0003 documents frequent-failure warning policy.

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
