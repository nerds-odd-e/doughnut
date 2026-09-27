# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved findings are removed; Git history keeps their evidence.

## Review — 2026-09-27

Reviewed both finding logs at `299cd69bac`. Ownership follows the failing
responsibility, not the repository in which it was observed: a finding is
Donut’s when its correction lands in Donut’s product, scripts, or Donut-authored
skills and `.agents/agent-map.md`.

### Shared findings retained in DearDough

| Responsibility | Findings | Ownership evidence |
| --- | --- | --- |
| Measurement scope | ODF-030 | Disposable profiling versus durable delivery scope is shared execution feedback. |
| CI observation and host/runtime integration | ODF-034, ODF-069, ODF-085, ODF-092, ODF-104, ODF-112, ODF-121 | Published CI observation guidance and the shared delivery scripts own target selection, observer coverage, retry, runtime binding, and managed attachment. Donut workflow details are occurrence evidence. |
| Delegation, proof, and failure attribution | ODF-042, ODF-051, ODF-059, ODF-082, ODF-090, ODF-091, ODF-110, ODF-111, ODF-113, ODF-124, ODF-125, DD-126, DD-127, DD-131, DD-132, DD-135, DD-136, DD-140 | Published delivery and proof guidance owns report verification, red runs, consumer coverage, exit-code capture in delegated commands, and coordinator ordering and waiting. |
| Refinement and planning premises | DD-128, DD-129, DD-130, DD-134, DD-137, DD-139 | Shared refinement, slice-planning and retrospective guidance own checking key examples, commands and mechanisms against today’s behavior before promising them. |
| Execution workspace, claims, preparation, and shared scripts | ODF-081, ODF-083, ODF-084, ODF-106, ODF-116, ODF-120, ODF-122, ODF-123, DD-133, DD-138 | Shared startup, readiness recording, plan-number allocation, backlog claim, the `agent-commit.mjs` entry guard, and the refactor file-size check. |

Donut tooling in the new entries behaved as documented, so it does not make
them project findings:

- DD-134: `scripts/e2e-invocation-selection.mjs` refuses a directory loudly, and
  `.agents/agent-map.md` and the `e2e-authoring` skill already give feature-file
  `--spec` paths; the plan’s command was never checked.
- DD-135: the `frontend` skill’s proof command is plain `vue-tsc --noEmit`; the
  agents added the `| tail` themselves.
- DD-140: `scripts/backend-test-worktree.sh` refuses anything but one
  `--tests` pattern; the failure was a case-sensitive filter claimed as coverage
  without the test’s name in the result.
- DD-126: `scripts/test/quality_changed.test` and the `script` skill existed;
  the failure was a plan’s unchecked negative claim.

### Resolved project findings

No occurrence in either log contradicts these resolutions:

- DD-073 — frontend proof typechecks (`7b1d80b4e8`). DD-135 is not a
  recurrence: the typecheck ran; only its exit code was misreported.
- DD-103 — E2E runner backend race (`d86864023c`). DD-134 is a documented
  refusal, not a startup race.
- DD-121 — parallel slices’ commit hook failed on other slices’ unstaged
  files. Resolved by `574d61b52c` (the gate lints a temporary copy of the
  index), `c76f69b62e` (`format` runs Biome only) and `120753a097`
  (`scripts/test/quality_changed.test`); removed from this log.
- DD-065/DD-074 — returned to shared ownership as ODF-085.

No project finding is open, so no product backlog story is queued from this log.

### Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
