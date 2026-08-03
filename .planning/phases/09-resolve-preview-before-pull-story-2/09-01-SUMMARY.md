---
phase: 09-resolve-preview-before-pull-story-2
plan: 01
subsystem: cli
tags: [sync, dry-run, previewPull, taxonomy, diagnostics, vitest]

requires:
  - phase: 08-resolve-pull-export-story-1
    provides: doughnut_id frontmatter in notebook export zip
provides:
  - Unit-proven create/update/move/reject taxonomy on previewPull
  - Reserved/duplicate/unsafe/rejects-only diagnostics without workspace writes
  - PreviewPullAction parallel to push NoteDiffStatus
affects:
  - 09-02 (cli_sync_dry_run E2E integration)
  - Phase 10 applyPull (frozen here; consume taxonomy later)

actuals:
  tokens: 7386
  tasks: 3
  commits: 7

tech-stack:
  added: []
  patterns:
    - PreviewPullAction separate from NoteDiffStatus
    - listZipFileNames before Map collapse for duplicate detection
    - Non-throwing reject rows for unsafe/reserved paths

key-files:
  created:
    - cli/src/sync/previewPullActions.ts
    - cli/tests/previewPullDiagnostics.test.ts
    - cli/tests/previewPullHarness.ts
  modified:
    - cli/src/sync/previewPull.ts
    - cli/src/sync/diffReport.ts
    - cli/src/sync/unzip.ts
    - cli/tests/previewPull.test.ts

key-decisions:
  - "Pull actions use PreviewPullAction; NoteDiffStatus stays pull|push|conflict"
  - "Move only via doughnut_id path mismatch; missing id stays path-keyed"
  - "Rejects counted apart from 'notes would change'; rejects-only ≠ no-op sentinel"
  - "Labels as path (create|update|move|reject); E2E substrings less.md and 1 note would change. preserved"

patterns-established:
  - "Classify order: duplicate/unsafe/sync-metadata/reserved → move → create/update"
  - "listZipFileNames preserves duplicates without changing unzipToEntries Map semantics"

requirements-completed: []  # EXP-02 remains open until 09-02 E2E; Plan 01 delivered unit portion only

coverage:
  - id: D1
    description: Dry-run labels create and update on path headings
    requirement: EXP-02
    verification:
      - kind: unit
        ref: cli/tests/previewPull.test.ts#reports a note the pull would create with an explicit create label
        status: pass
      - kind: unit
        ref: cli/tests/previewPull.test.ts#reports a changed note as a labeled update
        status: pass
    human_judgment: false
  - id: D2
    description: Identity move when same doughnut_id at different paths; no move without id
    requirement: EXP-02
    verification:
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#reports a move when the same doughnut_id is at a different path
        status: pass
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#does not infer a move when the export note lacks doughnut_id
        status: pass
    human_judgment: false
  - id: D3
    description: Reserved, duplicate, unsafe, sync-metadata, and rejects-only diagnostics
    requirement: EXP-02
    verification:
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#rejects a reserved log.md basename with a short reason
        status: pass
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#rejects duplicate export paths
        status: pass
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#rejects an unsafe path without writing the workspace
        status: pass
      - kind: unit
        ref: cli/tests/previewPullDiagnostics.test.ts#reports rejects-only instead of the clean no-op sentinel
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-03
status: complete
---

# Phase 9 Plan 01: Strengthen previewPull taxonomy and diagnostics Summary

**Unit-proven `/sync --dry-run` classify+report with create/update/move/reject labels and reserved/duplicate/unsafe diagnostics; applyPull left frozen.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-03T07:04:21Z
- **Completed:** 2026-08-03T07:10:00Z
- **Tasks:** 3
- **Files modified:** 7

## Accomplishments

- Dry-run report labels create and update explicitly while keeping `1 note would change.` and `less.md` substrings E2E-ready
- Identity move inferred only from `doughnut_id` path mismatch; missing id stays path-keyed create/update
- Reserved `index.md`/`log.md`, `.doughnut-sync/**`, duplicate zip names, and unsafe paths emit reject findings; rejects-only never collapses to `No changes to pull.`

## Task Commits

Each task was committed atomically (TDD red → green):

1. **Task 1: End-to-end dry-run create + update labels** — `00cce0d63d` (test) + `c9651d21dc` (feat)
2. **Task 2: Identity move and missing-id path-keyed only** — `b45b6803bb` (test) + `a639ea22a1` (feat)
3. **Task 3: Reserved/duplicate/invalid/rejects-only diagnostics** — `10aadb1adf` (test) + `b17f517e42` (feat)

**Post-change refactor:** `79536cc641` — split oversized previewPull tests + shared harness

**Plan metadata:** `611d691ef0` (docs: complete plan)

## Files Created/Modified

- `cli/src/sync/previewPullActions.ts` — PreviewPullAction, doughnut_id extract, classify + reject reasons
- `cli/src/sync/previewPull.ts` — Orchestrate list names → classify → report
- `cli/src/sync/diffReport.ts` — Optional pull action label; reject findings; reject summary counts
- `cli/src/sync/unzip.ts` — `listZipFileNames` for duplicate detection
- `cli/tests/previewPull.test.ts` — Create/update/non-write/error cases
- `cli/tests/previewPullDiagnostics.test.ts` — Move + reject diagnostics
- `cli/tests/previewPullHarness.ts` — Shared temp workspace helpers

## Decisions Made

- Keep `NoteDiffStatus` push-only; pass `PreviewPullAction` as a parallel heading label on `renderNoteDiff`
- Prefer reject over update for reserved basenames when content differs
- Unsafe paths return reject rows (non-throwing) so dry-run still returns a string

## Deviations from Plan

### Auto-fixed Issues

None - plan executed as written.

**Post-plan refactor (local wrap-up):** Split `previewPull.test.ts` (>250 lines) into diagnostics suite + harness after Task 3.

## Auth Gates

None.

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (T-09-01/02 mitigated by reject + non-write units; apply frozen for T-09-04).

## TDD Gate Compliance

- RED commits present for each task (`test(09-01): …`)
- GREEN commits present after each RED (`feat(09-01): …`)
- Optional refactor commit after green suite

## Self-Check: PASSED

- `cli/src/sync/previewPullActions.ts` FOUND
- `cli/tests/previewPullDiagnostics.test.ts` FOUND
- Commits `00cce0d63d`, `c9651d21dc`, `a639ea22a1`, `b17f517e42`, `79536cc641` FOUND
- `applyPull.ts` not in plan diff
