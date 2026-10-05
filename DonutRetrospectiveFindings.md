# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved findings are removed; Git history keeps their evidence.

A finding is Donut’s when its correction lands in Donut’s product, scripts, or
Donut-authored skills and `.agents/agent-map.md`; Donut details that only
illustrate a shared lesson stay in DearDough as occurrence evidence.

Groups are ordered by priority: impact first, then frequency.

## Open, not queued: Development startup on stale compiled classes

### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

`pnpm dev` from the default checkout failed with `No qualifying bean of type NotebookGitCutoverService`: `backend/build/classes` still held classes from before that service was removed. Deleting `backend/build/classes` let the next start succeed. Its cause is unknown.

#### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:25:42+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `dev.log` "APPLICATION FAILED TO START" with the missing-bean message; `git grep NotebookGitCutoverService -- backend/src` found nothing; the next `pnpm dev` was healthy.
  - Observed effect: one failed start and a short diagnosis before the UAT setup could continue.
  - Inference: the Development start's incremental build did not drop classes whose sources were deleted. Qualified: cause not investigated further.
- Investigation (2026-10-03, SEED-067#stacks-survive-other-builds slice 2): not reproduced. In the default checkout, a compiled `@Service` and its injecting consumer were deleted with the stack stopped, then `pnpm dev` started healthy and Gradle removed the stale class; the same held with a concurrent `classes --rerun-tasks` build and with a build-cache restore. Linked-worktree probes also removed the classes. No writer found; the cause remains unknown. Owner decides whether to keep watching or drop.

## Open, not queued: Plans prescribe absence checks that Donut’s removal rule forbids

Recurrence of resolved Donut finding DD-202 (not the DD-202 that became ODF-208),
whose correction put the removal rule in always-loaded agent guidance
(`CLAUDE.md` principle 7, `18b99b6424` and `52460a4077`, 2026-10-03). DD-205 happened the next day, so that correction did not reach
slice planning. Shared Open Dough guidance keeps absence assertions when absence
is the promise; the rule is Donut’s, so the correction lands in Donut.

Not queued: three occurrences, each costing one test or assertion written and removed.
It is the next candidate if it recurs.

### DD-205 — A plan's proof named an absence check that the project's removal rule forbids

A removal slice's proof listed "no Responses API call is made" for the deleted
model rewrite. CLAUDE.md principle 7 says a removal leaves no check that the
removed thing is absent, so the check was written by one agent and deleted by
the next.

#### Occurrences

- Execution: SEED-066#keep-every-transcribed-sentence / `9ad1cedcc0:.planning/slice-plans/008-keep-every-transcribed-sentence/PLAN.md` / d68937a0d2
  - Timestamp: 2026-10-04 (slice 2 implementation and refactor, before b233c0e7f3)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: plan 008 slice 2 Proof "no Responses API call is made"; the slice 2 implementer added `verify(officialClient, never()).responses()` to `AiAudioControllerTests`; the refactor report removed it citing principle 7; the coordinator recorded the drop in the plan (b233c0e7f3).
  - Observed effect: one assertion written and removed, and a plan proof item the delivered tests do not carry. Small cost.
  - Inference: when planning a removal, check each proof item against the removal rule; prove the replacement behavior instead of the absence. Qualified: one execution.
- Execution: SEED-069#reliable-development-stack-lifecycle / `404892b54af9d40804a0309c8bd92204d41c3e7d:.planning/slice-plans/006-stop-and-restart-development-stack/PLAN.md` / 62035934b9
  - Timestamp: 2026-10-05, slice 3 before the 14:54+09:00 commit
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: unknown
  - Evidence: plan proof row 10 prescribed writing a leftover `dev.pid` holding the test process's PID. The test was run red, then green, and deleted once no code read that file; the coordinator's Learnings explicitly cite principle 7. The existing free-primary start test carries the replacement outcome.
  - Observed effect: another test written and deleted during one slice; the plan's prescribed permanent proof did not survive.
  - Inference: third recorded occurrence of the same Donut planning gap; use the replacement start behavior as retained proof.

## Open, not queued: Rebuilding a stale MinerU virtual environment

### DD-161 — A `.venv-mineru` whose Python was garbage-collected had to be rebuilt by hand

A `.venv-mineru` whose Python lived in a garbage-collected Nix store path must
be rebuilt by hand. The plan's manual slice needed real MinerU to `/attach` real
PDFs through the CLI.

#### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 6, after 6f36cb2952); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 6 agent report (1,692 s, about 197k tokens, 96 tool uses); plan premise "`.venv-mineru/bin` has no `python`"; venv rebuilt with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six`.
  - Observed effect: about 28 minutes for one manual slice, most of it environment repair and stack scaffolding rather than observation.
  - Inference: a documented "hold a disposable stack" command would make the next real-service acceptance cheaper. Donut tooling, so correction belongs to Donut, not shared guidance.

## Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
