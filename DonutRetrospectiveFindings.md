# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved findings are removed; Git history keeps their evidence.

## Open findings

### Group: local run environment for manual and development runs

Both findings are Donut's own run tooling costing time before any product
observation starts. Home seed:
[SEED-060](.planning/seeds/SEED-060-dependable-local-run-environment.md).

#### DD-161 — Real-book manual acceptance had no supported way to hold a disposable stack or run MinerU

Story: [SEED-060#story-1](.planning/seeds/SEED-060-dependable-local-run-environment.md#story-1).

The plan's manual slice needed real MinerU and a running app to `/attach` real PDFs through the CLI. `.venv-mineru`'s Python pointed into a garbage-collected Nix store path, the unpinned `pip install 'mineru[pipeline]'` in the repo's docstrings installs MinerU 4.x (no `pipeline` extra, no `mineru.cli.common`), and no repo command keeps a disposable E2E stack up without Cypress, so the agent wrote a temporary `hold-stack.mjs` around `runE2eInteractive`.

##### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 6, after 6f36cb2952); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 6 agent report (1,692 s, about 197k tokens, 96 tool uses); plan premise "`.venv-mineru/bin` has no `python`"; venv rebuilt with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six`; `cli/python/mineru_book_outline.py` and `regenerate_mineru_output_for_refactoring.sh` still say unpinned.
  - Observed effect: about 28 minutes for one manual slice, most of it environment repair and stack scaffolding rather than observation.
  - Inference: much of the cost was necessary once; a pinned MinerU install and a documented "hold a disposable stack" command would make the next real-book acceptance cheaper. Donut tooling, so correction belongs to Donut, not shared guidance.

#### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

Story: [SEED-060#story-2](.planning/seeds/SEED-060-dependable-local-run-environment.md#story-2).

`pnpm dev` from the default checkout failed with `No qualifying bean of type NotebookGitCutoverService`: `backend/build/classes` still held classes from before that service was removed. Deleting `backend/build/classes` let the next start succeed.

##### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:25:42+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `dev.log` "APPLICATION FAILED TO START" with the missing-bean message; `git grep NotebookGitCutoverService -- backend/src` found nothing; the next `pnpm dev` was healthy.
  - Observed effect: one failed start and a short diagnosis before the UAT setup could continue.
  - Inference: the Development start's incremental build did not drop classes whose sources were deleted. Qualified: cause not investigated further.

## Review — 2026-09-29

Reviewed `DearDough.md` at `b9d503cf02` with the ownership rule below
(a finding is Donut's when its correction lands in Donut's product, scripts,
or Donut-authored skills and `.agents/agent-map.md`).

### Moved from DearDough

- DD-161 and DD-159 — open; see above.
- DD-165 — reviving `epub_book.feature` broke `scripts/` tests that used it as
  their "not admitted" example (SEED-059#story-1, CI run 36522703164). The
  cause was Donut's script tests coupling to a live spec name. Resolved by
  `7580fceccc`, which introduced the made-up
  `UNADMITTED_ISOLATED_CYPRESS_SPEC`; removed here, Git history keeps its
  evidence. The general lesson (prove the changed code's consumers) stays with
  ODF-150 and ODF-111.

### Newer entries kept in DearDough

Donut tooling behaved as documented, or the correction belongs to shared
guidance or the host:

- DD-143: the `frontend` skill names `pnpm -C frontend exec vue-tsc --noEmit`;
  the coordinator invented `pnpm test:typecheck`.
- DD-147: `docs/worktree-backend-tests.md` documents one `--tests '<pattern>'`
  and the wrapper refuses more loudly, as for DD-140; the plan copied Gradle's
  form unchecked.
- DD-145: shared planning of production observations; Donut's missing agent
  DB route and log routing are the facts the plan failed to check.
- DD-144, DD-160: the file-size rule in the shared
  `dough-post-change-refactor` references (with ODF-152).
- DD-142, DD-162, DD-163, DD-164, DD-166, DD-167: shared planning, proof and
  delegation guidance.
- DD-146, DD-148: host behavior. DD-156: shared manual-testing guidance.
  DD-157: shared refactor cadence. DD-158: shared `execution-start.mjs`.

### Resolved findings rechecked

No new occurrence reopens DD-073, DD-103 or DD-121. The SUT start fix
`d33dedc7c8` (SEED-039#story-4) repaired a script test racing its own deadline,
not the DD-103 runner backend race.

### Priority

Only two project findings are open, each with one occurrence; both are queued
as the first two backlog stories. DD-161 ranks first: about 28 minutes lost,
and the queued SEED-059 book-reading stories will need real-book acceptance
again. DD-159 ranks second: low cost per occurrence, but it recurs whenever a
backend class is removed and the stack starts from an older build.

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
- DD-133: Donut’s trigger is its layout. `.gitignore` excludes `.claude/*`, so
  a fresh worktree has no installed `.claude/skills/dough-*` copies, and
  `scripts/worktree_setup.sh` links every skill there to `.agents/skills`.
  The shared runtime guidance prefers that alias, and the sibling entry
  scripts compare real paths; only `agent-commit.mjs`’s literal-path guard
  fails, so the correction belongs upstream. Excluding `dough-*` from Donut’s
  links would add a special case to work around it.

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

No project finding was open at that review, so no product backlog story was queued.

### Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
