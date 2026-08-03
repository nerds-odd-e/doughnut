---
phase: 12-resolve-push-dry-run-story-5
plan: 01
subsystem: cli
tags: [push-dry-run, previewPush, sync-metadata, create-update, e2e]

requires:
  - phase: 11-resolve-workspace-lint-story-4
    provides: Coarse sizing precedent; green CLI E2E harness
  - phase: 10-resolve-incremental-pull-story-3
    provides: Baseline written on successful mutate pull; export seed path
provides:
  - Load-only previewPush (no savePushBaseline on dry-run)
  - Path-union create reporting for local-only and remote-only Markdown
  - Green previewPush units + cli_push_dry_run.feature (PUSH-01)
affects:
  - 13-resolve-safe-push-story-6

actuals:
  tokens: 10229
  tasks: 1
  commits: 1

tech-stack:
  added: []
  patterns:
    - "Dry-run load-only baseline (mirror previewPull non-mutation)"
    - "Import-only classifyCreateOrUpdate; local reserved-path filter (HYG-02)"
    - "previewPushHarness + split create/conflict/directional unit suites"

key-files:
  created:
    - cli/tests/previewPushHarness.ts
    - cli/tests/previewPush.create.test.ts
    - cli/tests/previewPush.conflict.test.ts
    - cli/tests/previewPush.directional.test.ts
  modified:
    - cli/src/sync/previewPush.ts
    - cli/src/sync/pushBaseline.ts
    - cli/tests/previewPush.test.ts
    - e2e_test/features/cli/cli_push_dry_run.feature

key-decisions:
  - "D-02: previewPush never writes .doughnut-sync; loadPushBaseline only"
  - "D-04/A1: intersecting keep (push)/(pull)/(CONFLICT); creates use (create) heading"
  - "D-08: one implementation commit for code+units+E2E"

patterns-established:
  - "Push dry-run primes via export/savePushBaseline — never via prior dry-run"
  - "Compose create headings via renderNoteDiff action without changing diffReport XOR"

requirements-completed: [PUSH-01]

coverage:
  - id: D1
    description: Dry-run does not create or alter .doughnut-sync baseline
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: cli/tests/previewPush.test.ts#does not write sync metadata
        status: pass
      - kind: unit
        ref: cli/tests/previewPush.directional.test.ts#does not alter an existing baseline
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_push_dry_run.feature#The preview adds no files of its own
        status: pass
    human_judgment: false
  - id: D2
    description: Local-only and remote-only Markdown report as (create)
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: cli/tests/previewPush.create.test.ts
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_push_dry_run.feature#A note only in Doughnut/workspace is reported as a create
        status: pass
    human_judgment: false
  - id: D3
    description: Directional labels and conflicts remain correct when baseline is export-primed
    requirement: PUSH-01
    verification:
      - kind: unit
        ref: cli/tests/previewPush.directional.test.ts
        status: pass
      - kind: unit
        ref: cli/tests/previewPush.conflict.test.ts
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_push_dry_run.feature#A later preview says which side changed since export primed the baseline
        status: pass
    human_judgment: false

duration: 7min
completed: 2026-08-03
status: complete
---

# Phase 12 Plan 01: Strengthen push dry-run Summary

**`/push --dry-run` is load-only for sync metadata and reports local-/remote-only notes as creates while keeping directional conflict labels.**

## Performance

- **Duration:** 7min
- **Started:** 2026-08-03T08:16:21Z
- **Completed:** 2026-08-03T08:23:30Z
- **Tasks:** 1
- **Files modified:** 8

## Accomplishments

- Removed `savePushBaseline` / `nextBaseline` from `previewPush`; dry-run never writes `.doughnut-sync/`
- Path-union reporting: local-only → `(create)` push orientation; remote-only → `(create)` pull orientation; intersecting keep `(push)` / `(pull)` / `(CONFLICT)`
- Flipped units and `cli_push_dry_run.feature` (export-primed directional Rule; adds-no-files inventory; create scenarios)
- Split oversized unit suite via `previewPushHarness` (create / conflict / directional)

## Task Commits

1. **Task 1: End-to-end strengthen previewPush — load-only baseline + create/update (units + E2E)** - `d29f3b841b` (feat)

**Plan metadata:** `ea566e90df` (docs: complete plan)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 3 - Blocking] E2E create scenarios used nonexistent step phrases**
- **Found during:** Task 1
- **Issue:** Drafted `I create a note belonging to…` / `I add a Markdown file…` steps that do not exist
- **Fix:** Rewrote scenarios to existing steps (`extra file` + remove remote-only file pattern from `cli_sync_pull`)
- **Files modified:** `e2e_test/features/cli/cli_push_dry_run.feature`
- **Commit:** `d29f3b841b`

**2. [Rule 3 - Blocking] Unused import failed pre-commit Biome**
- **Found during:** Task 1 commit
- **Issue:** `join` left unused in `previewPush.conflict.test.ts` after split
- **Fix:** Removed unused import; new commit attempt succeeded
- **Files modified:** `cli/tests/previewPush.conflict.test.ts`
- **Commit:** `d29f3b841b`

## Verification Results

- Units: `CURSOR_DEV=true nix develop -c bash -c 'cd cli && pnpm exec vitest run tests/previewPush'` — **30/30 pass**
- E2E: `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/cli/cli_push_dry_run.feature` — **11/11 pass**
- HYG-02: `previewPullActions.ts` not in diff
- D-06: `cli_push.feature` not deleted; `parsePushArgument` still requires `--dry-run`
- D-02: no `savePushBaseline(` in `previewPush.ts`

## Self-Check: PASSED

- FOUND: `cli/src/sync/previewPush.ts`
- FOUND: `cli/tests/previewPush.test.ts`
- FOUND: `e2e_test/features/cli/cli_push_dry_run.feature`
- FOUND: commit `d29f3b841b`
