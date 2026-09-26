# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.

## Review — 2026-09-26

Reviewed both finding logs at `7b05bb9df8`. Ownership follows the failing
responsibility, not the repository in which it was observed.

### Moved from DearDough

DD-121 moved here. The shared rule only permits concurrent slices with disjoint
file changes. The failing responsibility is Donut’s commit gate:
`scripts/git-hooks/pre-commit` runs `pnpm lint:changed`, and
`scripts/quality_changed.sh` picks components from staged files but then runs
each component’s whole-tree lint (for the frontend, Biome and `vue-tsc`), so
unstaged files from another slice decide whether a commit passes. Neither
script has changed since `1e6e69cc64` (2026-09-04), which predates the
occurrence, so the finding is unresolved.

### Shared findings retained in DearDough

| Responsibility | Findings | Ownership evidence |
| --- | --- | --- |
| Measurement scope | ODF-030 | Disposable profiling versus durable delivery scope is shared execution feedback. |
| CI observation and host/runtime integration | ODF-034, ODF-069, ODF-085, ODF-092, ODF-104, ODF-112, DD-115 | Published [CI observation guidance](https://github.com/terryyin/open-dough/blob/v0.3.38/src/skills/dough-execute-plan/references/ci-monitor.md) and the shared delivery scripts own target selection, observer coverage, retry, runtime binding, and managed attachment. Donut workflow details are occurrence evidence. |
| Delegation, proof, and failure attribution | ODF-042, ODF-051, ODF-059, ODF-082, ODF-090, ODF-091, ODF-110, ODF-111, ODF-113, DD-116, DD-120, DD-122, DD-123 | Published [delivery and proof guidance](https://github.com/terryyin/open-dough/blob/v0.3.38/src/skills/dough-execute-plan/references/wrap-up.md) owns report verification, consumer coverage, independent refactoring, readiness replay, failure baselines, and coordinator waiting. Specific CLI/backend tests and host permission or notification behavior do not make these process failures project-owned. |
| Execution workspace, claims, preparation, and maintenance receipts | ODF-081, ODF-083, ODF-084, ODF-099, DD-114, DD-117, DD-118, DD-119, DD-124 | Shared startup, readiness recording, plan-number allocation, backlog claim, and increment-delivery responsibilities. Donut’s plan-numbering reset changes only the starting number, not the allocation procedure. |

ODF-085 stays shared although Donut does not track `.claude/skills`: the
failure was guidance that pointed at that alias instead of the already-tracked
`.agents` runtime, and its follow-up is queued upstream.

### Previously resolved project findings

No new occurrence in either log contradicts an earlier resolution:
DD-073 (frontend proof now typechecks, `7b1d80b4e8`), DD-103 (E2E runner
backend race, `d86864023c`), and DD-065/DD-074 (returned to shared ownership as
ODF-085). DD-121 involves a type error at the commit gate, but in another
slice’s unstaged file; it is not a DD-073 recurrence, because the committing
slice’s own proof had typechecked.

### Grouping and backlog decision

| Priority | Group | Findings | Frequency | Impact | Backlog story |
| --- | --- | --- | --- | --- | --- |
| 1 | Commit gate in a shared execution checkout | [DD-121](#dd-121) | One execution, two failed commits | Finished slices could not be delivered until every other agent in the checkout stopped, so parallel implementation ended in serialized delivery | [Commit a finished slice while other slices are still in progress](.planning/seeds/SEED-043-commit-gate-checks-committed-changes.md#story-1) |

DD-121 is the only unresolved project finding, so only one story is queued
first; there is no second project problem to rank. No existing queued or taken
story covers it.

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.

<a id="dd-121"></a>

## DD-121 — Parallel slices in one execution checkout made each commit's hook fail on the other slices' unfinished files

Three file-disjoint slices ran at once in one worktree. The pre-commit hook runs `lint:changed`, which checks the whole frontend working tree (Biome and `vue-tsc`), not only staged files. Committing a finished slice failed twice: once on another slice's unformatted file, once on type errors in a slice still being refactored. Slices were committed only after every agent in the checkout had stopped.

### Occurrences

- Execution: SEED-039 story 4 / quick/041-faster-frontend-unit-tests / c9347a9ee3; Timestamp: 2026-09-26, ~09:00+08:00 (failed slice 3 commits before 8d4739bd0b 09:08+08:00); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.38.
  - Evidence: hook output "Found 1 error" (Biome format in `RichMarkdownEditor.propertyEntry.spec.ts`, not staged) and `TS2305 … has no exported member 'mountMarkdownTextarea'` in `NoteEditableContent.debouncedSave.spec.ts`, not staged.
  - Observed effect: delivery of finished slices waited for unrelated agents; parallelism saved implementation time but serialized delivery.
  - Inference: `execute-plan` allows concurrent slices with disjoint files, but this project's working-tree-wide hook makes a shared checkout unsafe for concurrent commits; per-slice worktrees or committing only at quiet points would avoid it. Qualified: the slices' implementation overlap still saved wall time.
