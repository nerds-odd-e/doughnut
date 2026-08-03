---
phase: 11-resolve-workspace-lint-story-4
plan: 01
subsystem: cli
tags: [lint, portable-contract, okf, doughnut_id, vitest, cypress]

requires:
  - phase: 09-resolve-preview-before-pull-story-2
    provides: extractDoughnutId and unsafePathReason (import-only)
  - phase: 08-resolve-pull-export-story-1
    provides: doughnut_id identity contract
provides:
  - portableContractFindings for duplicate id / broken links / missing index / unsafe paths
  - Strengthened lintWorkspace OKF+portable orchestration
  - Green lintWorkspace units + cli_lint_workspace.feature for LINT-01
affects: [phase-12-push-dry-run, phase-14-hygiene]

actuals:
  tokens: 8000
  tasks: 3
  commits: 3

tech-stack:
  added: []
  patterns:
    - Additive portable findings after OKF flatMap
    - Import-only Phase 9 path/id helpers (HYG-02)

key-files:
  created:
    - cli/src/lint/portableContract.ts
    - cli/tests/lintWorkspace.portableContract.test.ts
    - cli/tests/lintWorkspace.path.test.ts
    - cli/tests/lintWorkspaceFixture.ts
  modified:
    - cli/src/lint/lintWorkspace.ts
    - cli/tests/lintWorkspace.test.ts
    - e2e_test/features/cli/cli_lint_workspace.feature

key-decisions:
  - "Auto-selected invert-portable (D-03) under --auto"
  - "Path-oriented wiki resolution (A1); workspace-root-relative /href (A2)"
  - "Index rule = dirs that directly contain concepts (A3)"
  - "E2E unsupported path via unsafe link target ../ (A4)"

patterns-established:
  - "portableContractFindings returns Finding[]; scanners stay unexported"
  - "CONFORMS fixtures cascade minimal index.md whenever concepts exist"

requirements-completed: [LINT-01]

coverage:
  - id: D1
    description: Duplicate doughnut_id errors on each colliding concept path
    requirement: LINT-01
    verification:
      - kind: unit
        ref: cli/tests/lintWorkspace.portableContract.test.ts#reports each concept that shares a doughnut_id
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_lint_workspace.feature#Duplicate doughnut_id values are errors
        status: pass
    human_judgment: false
  - id: D2
    description: Broken local MD and wiki links report errors; http(s) and /attachments/ skipped
    requirement: LINT-01
    verification:
      - kind: unit
        ref: cli/tests/lintWorkspace.portableContract.test.ts#reports a link to a concept that is not in the bundle
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_lint_workspace.feature#A broken local link is an error
        status: pass
    human_judgment: false
  - id: D3
    description: Missing index.md in concept-bearing directories reports errors; empty dirs ignored
    requirement: LINT-01
    verification:
      - kind: unit
        ref: cli/tests/lintWorkspace.portableContract.test.ts#reports one concept carrying only a type, and no index.md
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_lint_workspace.feature#A concept-bearing directory without index.md is an error
        status: pass
    human_judgment: false
  - id: D4
    description: Unsafe local link targets surface unsafePathReason vocabulary
    requirement: LINT-01
    verification:
      - kind: unit
        ref: cli/tests/lintWorkspace.portableContract.test.ts#reports an unsafe local link target
        status: pass
      - kind: e2e
        ref: e2e_test/features/cli/cli_lint_workspace.feature#An unsafe local link target is an error
        status: pass
    human_judgment: false
  - id: D5
    description: Valid portable workspace still reports Workspace follows the OKF format.
    requirement: LINT-01
    verification:
      - kind: e2e
        ref: e2e_test/features/cli/cli_lint_workspace.feature#A conformant bundle reports nothing
        status: pass
    human_judgment: false

duration: 6min
completed: 2026-08-03
status: complete
---

# Phase 11 Plan 01: Resolve workspace lint Summary

**OKF `/lint` now appends portable findings for duplicate `doughnut_id`, broken local/wiki links, missing `index.md`, and unsafe path shapes — proven by units and `cli_lint_workspace.feature`.**

## Performance

- **Duration:** 6 min
- **Started:** 2026-08-03T07:51:42Z
- **Completed:** 2026-08-03T07:58:00Z
- **Tasks:** 3 (checkpoint auto-selected + tracer units + E2E)
- **Files modified:** 7

## Accomplishments

- Wired `portableContractFindings` into `lintWorkspace` on top of OKF (D-02)
- Inverted D-03 broken-link and missing-index contracts; cascaded `index.md` into CONFORMS fixtures
- Targeted E2E proves all four TRIAGE gaps + portable-valid success string unchanged (D-04)

## Task Commits

1. **Checkpoint: invert-portable (D-03)** — auto-selected under `--auto` (no commit)
2. **Task 2: End-to-end portable contract findings through lintWorkspace (units)** — `14c638d772` (feat)
3. **Task 3: Capability E2E — four portable gaps + conformant success** — `157e2c52bc` (feat)
4. **post-change-refactor: split oversized unit tests** — `bb4d66db9c` (refactor)

## Files Created/Modified

- `cli/src/lint/portableContract.ts` — four portable scanners → `Finding[]`
- `cli/src/lint/lintWorkspace.ts` — OKF + portable orchestration
- `cli/tests/lintWorkspace.test.ts` — OKF suite with index cascade
- `cli/tests/lintWorkspace.portableContract.test.ts` — portable gap units
- `cli/tests/lintWorkspace.path.test.ts` — path-argument units
- `cli/tests/lintWorkspaceFixture.ts` — shared tmp harness
- `e2e_test/features/cli/cli_lint_workspace.feature` — four gaps + fixed conformant

## Decisions Made

- Auto-selected **invert-portable** (locked D-03)
- Wiki resolve path-oriented; `/href` workspace-root-relative; index on concept-bearing dirs only; E2E unsafe via `../` link (A1–A4)

## Deviations from Plan

### Auto-fixed Issues

**1. [Rule 2 - Missing critical functionality] Cascade index.md into count-sensitive E2E scenarios**
- **Found during:** Task 3 (E2E)
- **Issue:** Missing-index rule would inflate “1 error in 1 file” / “2 errors, 1 warning” assertions
- **Fix:** Added root `index.md` to frontmatter-missing, non-md warning, and multi-finding scenarios (kept missing-index scenario without index)
- **Files modified:** `e2e_test/features/cli/cli_lint_workspace.feature`
- **Commit:** `157e2c52bc`

**2. [Rule 3 - Blocking] Split lintWorkspace.test.ts over 250-line limit**
- **Found during:** post-change-refactor
- **Issue:** Combined OKF + portable + path tests exceeded 250 lines
- **Fix:** Extracted fixture + path + portable test files
- **Files modified:** `cli/tests/lintWorkspace*.ts`
- **Commit:** `bb4d66db9c`

## Auth Gates

None.

## Known Stubs

None.

## Threat Flags

None beyond plan threat model (read-only lint; `unsafePathReason` on link targets).

## Self-Check: PASSED

- FOUND: `cli/src/lint/portableContract.ts`
- FOUND: `cli/src/lint/lintWorkspace.ts`
- FOUND: `e2e_test/features/cli/cli_lint_workspace.feature`
- FOUND: commits `14c638d772`, `157e2c52bc`, `bb4d66db9c`
- HYG-02: diff excludes `previewPullActions.ts`
- Verify: vitest lintWorkspace* green (40); cypress `cli_lint_workspace.feature` 8/8 green
