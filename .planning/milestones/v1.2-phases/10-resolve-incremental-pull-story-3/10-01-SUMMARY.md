---
phase: 10-resolve-incremental-pull-story-3
plan: 01
subsystem: cli
tags: [sync, applyPull, baseline, classify, vitest, cypress]

requires:
  - phase: 09-resolve-preview-before-pull-story-2
    provides: classifyPreviewPullNotes create/update/move/reject taxonomy
provides:
  - Mutating applyPull create/update/move via shared classify
  - Gated .doughnut-sync/baseline.json after successful mutations only
  - Inverted create unit + cli_sync_pull E2E proofs (EXP-03)
affects:
  - 12-resolve-preview-before-push-story-5
  - 13-resolve-push-story-6

actuals:
  tokens: 3352
  tasks: 3
  commits: 2

tech-stack:
  added: []
  patterns:
    - classify-then-apply (previewPull align)
    - baseline merge patch after mutate ≥1

key-files:
  created: []
  modified:
    - cli/src/sync/applyPull.ts
    - cli/tests/applyPull.test.ts
    - e2e_test/features/cli/cli_sync_pull.feature
    - cli/src/commands/notebook/syncSlashCommand.tsx

key-decisions:
  - "Auto-selected invert-create for D-08 (CONTEXT lock; --auto)"
  - "Baseline merge = load prior map + patch applied paths (A1)"
  - "Move = write new path then unlinkSync(fromPath) only (A2)"
  - "E2E move deferred; unit move is proof (A3)"

patterns-established:
  - "Pattern: applyPull imports classifyPreviewPullNotes; never edits previewPullActions (HYG-02)"
  - "Pattern: savePushBaseline only when mutation count ≥ 1"

requirements-completed: [EXP-03]

coverage:
  - id: D1
    description: "Remote-only notes are created on mutating pull; anti-create inverted"
    requirement: EXP-03
    verification:
      - kind: unit
        ref: "cli/tests/applyPull.test.ts#creates a file for a remote-only note"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/cli/cli_sync_pull.feature#Pull creates a remote-only note"
        status: pass
    human_judgment: false
  - id: D2
    description: "Identity move writes destination, removes fromPath, preserves local-only"
    requirement: EXP-03
    verification:
      - kind: unit
        ref: "cli/tests/applyPull.test.ts#applies a move when the same doughnut_id is at a different path"
        status: pass
    human_judgment: false
  - id: D3
    description: "Rejects reported and never written; rejects-only skips baseline"
    requirement: EXP-03
    verification:
      - kind: unit
        ref: "cli/tests/applyPull.test.ts#rejects a reserved log.md without writing it"
        status: pass
    human_judgment: false
  - id: D4
    description: "Baseline written after mutate; no baseline on no-op"
    requirement: EXP-03
    verification:
      - kind: unit
        ref: "cli/tests/applyPull.test.ts#writes baseline after a mutating create"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/cli/cli_sync_pull.feature#Pull updates one remote change (baseline hold-only)"
        status: pass
      - kind: e2e
        ref: "e2e_test/features/cli/cli_sync_pull.feature#No-op when already in sync (no baseline)"
        status: pass
    human_judgment: false
  - id: D5
    description: "Update, local-only, and @perfSync stay green"
    requirement: EXP-03
    verification:
      - kind: e2e
        ref: "CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_pull.feature"
        status: pass
    human_judgment: false

duration: 3min
completed: 2026-08-03
status: complete
---

# Phase 10 Plan 01: Strengthen applyPull Summary

**Mutating `/sync` applies create/update/move via Phase 9 classify and writes `.doughnut-sync/baseline.json` only after successful mutations — proven by 12 units + 5 targeted E2E scenarios.**

## Performance

- **Duration:** 3min
- **Started:** 2026-08-03T07:31:35Z
- **Completed:** 2026-08-03T07:34:23Z
- **Tasks:** 3/3
- **Files modified:** 4

## Accomplishments

- Rewrote `applyPull` to classify via `classifyPreviewPullNotes` (import-only; HYG-02) then apply create/update/move
- Gated `savePushBaseline` on mutation count ≥ 1; rejects-only and matching no-op leave sync metadata untouched
- Inverted anti-create unit + E2E into create proofs; update, local-only, no-op, and `@perfSync` stay green

## Task Commits

1. **Task 1: Confirm invert-create (D-08)** — auto-selected `invert-create` under `--auto` / `_auto_chain_active` (no separate commit; recorded here)
2. **Task 2: classify→apply + units** — `23403045ea` (feat)
3. **Task 3: E2E create flip + baseline** — `d3352c87af` (feat)

**Plan metadata:** (docs commit follows)

## Files Created/Modified

- `cli/src/sync/applyPull.ts` — classify → disk writes → gated baseline; reject report lines
- `cli/tests/applyPull.test.ts` — create, move, reject, baseline, update, local-only, perf
- `e2e_test/features/cli/cli_sync_pull.feature` — create flip + baseline assert + no-op no-baseline
- `cli/src/commands/notebook/syncSlashCommand.tsx` — help text covers create/update/move

## Decisions Made

- **invert-create** auto-selected for D-08 (CONTEXT already locked; `--auto` checkpoint handling)
- Baseline merge patches prior map for applied paths only (discretion A1)
- Move = write destination + `unlinkSync(fromPath)` only (A2); E2E move skipped (A3)

## Deviations from Plan

None - plan executed exactly as written (auto-selected recommended checkpoint option).

### Auto-mode checkpoint

**Task 1 checkpoint:decision** — Auto-selected `invert-create` per locked CONTEXT D-08 and executor `--auto` instructions. Did not wait for human.

## Verification Evidence

- Units: `CURSOR_DEV=true nix develop -c pnpm -C cli exec vitest run tests/applyPull.test.ts` → **12 passed**
- E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_sync_pull.feature` → **5 passed** (19s)
- HYG-02: `git diff` excludes `cli/src/sync/previewPullActions.ts`
- No `@wip` left on pull scenarios
- Post-change-refactor: already clean (files ≤250 lines; no new duplication)

## Self-Check: PASSED

- FOUND: `cli/src/sync/applyPull.ts`
- FOUND: `cli/tests/applyPull.test.ts`
- FOUND: `e2e_test/features/cli/cli_sync_pull.feature`
- FOUND: `23403045ea`
- FOUND: `d3352c87af`
