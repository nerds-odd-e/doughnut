# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved findings are removed; Git history keeps their evidence.

A finding is Donut’s when its correction lands in Donut’s product, scripts, or
Donut-authored skills and `.agents/agent-map.md`; Donut details that only
illustrate a shared lesson stay in DearDough as occurrence evidence.

Groups are ordered by priority: impact first, then recurrence, with unknown
causes and one-off environment repair below repeatedly blocked feedback.
Ownership and current resolution evidence reviewed 2026-10-09 at `d4481ac31e`.

## Ownership and priority assessment

Compared the installed skills with published Open Dough `v0.3.57`
(`e3c5fa0a61e4743ed6a16331755ddd2d1e2a399b`) in the source checkout. The
[execution wrap-up](https://github.com/terryyin/open-dough/blob/v0.3.57/src/skills/dough-execute-plan/references/wrap-up.md)
and [refactor checks](https://github.com/terryyin/open-dough/blob/v0.3.57/src/skills/dough-post-change-refactor/references/refactor-checks.md)
match the installed files byte for byte.

| Group | Distinct recorded executions | Impact and disposition |
| --- | --- | --- |
| E2E proof blocked by formatting | 3, October 5–7 | Startup prevents scenario feedback; queue first. |
| Donut removal-proof churn | 2 after the correction, plus the original DD-202 | Repeated proof written and deleted despite the local rule; queue second as a recurrence. |
| MinerU venv recovery | 1 | Mixed environment/stack cost of about 28 minutes; venv remainder unqueued. |
| Stale Development classes | 1; later probes healthy | A failed start, cause unknown; retain unqueued, not confirmed resolved. |

The public finding catalog already names ODF-215. A published name does not
settle correction ownership: Donut's ordinary SUT starts Vite through
`scripts/sut-services.mjs` and `package.json`, and
`frontend/vite.config.ts` enables Biome outside Vitest. The selected story
repairs that local startup coupling while retaining the public formatting
workflow. Its last occurrence was in a coordinator-run slice, so delegated
formatting permission alone would not resolve it.

DD-205 concerns Donut's specific deletion-and-sweep convention in `AGENTS.md`
and `CLAUDE.md`. Public refactor guidance retains negative assertions for actual
promises. The second story applies the local convention through project-owned
handoffs; it does not change that public guidance.

The expensive mock-reproduction framing (ODF-213), file-size rule, proof-selection,
CI observer/delivery scripts, and coordinator authorization/acceptance lessons
remain shared findings in DearDough. In particular, fixing the dictation race
from DD-214 does not prove its coordinator-acceptance lesson resolved.

No complete finding retained in the project file has a confirmed durable
resolution in this pass. Earlier closed held-stack and Development-lifecycle
groups stay removed; current `e2e:hold` and `dev:stop` commands confirm those
corrections. DD-159's unsuccessful reproduction is not resolution evidence.

<a id="e2e-proof-formatting"></a>

## Queued: E2E feedback reaches scenarios before final formatting

Story: [Run Donut E2E proof before final formatting](.planning/seeds/SEED-071-reliable-project-feedback.md#e2e-proof-before-final-formatting)
— SEED-071#e2e-proof-before-final-formatting.

Priority: first. Three executions met the gate, including a recurrence after a
read-only formatting check was added only to one execution's delegation. No
current project configuration or maintained command guidance corrects the
startup coupling. Existing finding names and occurrence evidence are retained.

### ODF-215 — E2E startup fails on unformatted files, but implementers are told not to format

Former local code: DD-211.

The execution wrap-up formats once, by the coordinator, after the refactor.
The frontend dev server's checker (`frontend/vite.config.ts`, `checker({ biome: true })`)
runs Biome when the E2E stack starts, so an implementer whose slice proof is
an E2E run cannot run it on unformatted files.

#### Occurrences

- Execution: SEED-066#recover-failed-transcription / `212e428968:.planning/slice-plans/005-recover-failed-transcription/PLAN.md` / ab773157bb
  - Timestamp: unknown (slice 3, before commit 2026-10-05T14:58:50+09:00)
  - Tool: Claude Code
  - Open Dough release: 0.3.56 (execution-checkout VERSION, unchanged during execution)
  - Evidence: slice 3 implementer learning, recorded in the plan's Learnings at ab773157bb: "E2E stack startup runs a Biome format check, so an unformatted frontend file stops `cy:run` before any scenario runs"; CLAUDE.md "implementers/refactorers run neither it nor standalone `lint:changed`".
  - Observed effect: the slice 3 E2E proof was blocked until files were formatted; extra time not recorded.
  - Inference: the rule and the project tooling conflict for any slice whose proof is an E2E run; the agent map or the slice delegation could say that formatting the touched files before `cy:run` is allowed. Qualified: one occurrence; how the implementer resolved it was not supplied.
- Execution: SEED-066#understandable-first-dictation / `0662bac730:.planning/slice-plans/008-complete-a-first-dictation-with-understandable-controls/PLAN.md` / 31b9b7b7bb
  - Timestamp: unknown (slice 1, before commit 2026-10-06T09:18:48+09:00)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.56 (execution-checkout VERSION)
  - Evidence: slice 1 implementer return: the first mocked-journey run failed on a Biome line-length error in `NoteToolbar.panels.spec.ts`; it was rewrapped by hand. The coordinator then told later implementers they may run `biome check --linter-enabled=false` on their files, and the slice 3 and slice 4 E2E runs passed first time.
  - Observed effect: one failed E2E run in slice 1; extra time not recorded.
  - Inference: a read-only format check of touched files before `cy:run` avoided a repeat without breaking the coordinator-only formatting rule. Qualified: the delegation prompt supplied it, not the guidance.
- Execution: SEED-066#no-record-while-stopping / `b63b742f8a:.planning/slice-plans/010-keep-the-dictation-result-true-while-stop-finishes/PLAN.md` / 2c30afdd26
  - Timestamp: 2026-10-07T13:47:02+09:00 (SUT readiness failure in `sut.log`)
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.57 (execution-checkout `.agents/skills/dough-update/VERSION`)
  - Evidence: slice 1, implemented by the coordinator; first `pnpm cy:run --spec e2e_test/features/note_creation_and_update/record_live_audio.feature` stopped with "SUT readiness failed" because `frontend:sut` reported "Formatter would have printed the following content" for `NoteAudioTools.retry.spec.ts`; after `biome format --write` on the three touched frontend files the rerun passed (2 scenarios). The plan's proof commands did not mention it; the plan's Learnings now do.
  - Observed effect: one failed E2E stack start and a log read before the rerun; about one minute.
  - Inference: the third occurrence in three SEED-066 executions; the guidance or the agent map stating "format touched frontend files before `cy:run`" would remove it. Qualified: the coordinator, not a delegated implementer, hit it here.

<a id="local-removal-proof"></a>

## Queued: Apply Donut’s removal rule from planning through delivery

Story: [Deliver Donut removals under the project's deletion rule](.planning/seeds/SEED-071-reliable-project-feedback.md#removals-follow-project-rule)
— SEED-071#removals-follow-project-rule.

Recurrence of resolved Donut finding DD-202 (not the DD-202 that became ODF-208),
whose correction put the removal rule in always-loaded agent guidance
(`CLAUDE.md` principle 7, `18b99b6424` and `52460a4077`, 2026-10-03). DD-205 happened the next day, so that correction did not reach
slice planning. Shared Open Dough guidance keeps absence assertions when absence
is the promise; the rule is Donut’s, so the correction lands in Donut.

Priority: second. Two distinct executions after the October 3 correction each
wrote and removed a test or assertion, in addition to the earlier DD-202
occurrence. The existing rule already addresses every role (`AGENTS.md:30–35`,
unchanged since `18b99b6424`); its publication alone did not resolve the problem.
The completed SEED-068 story is absent from both active backlog lists, so the
recurrence has its own story. Its correction belongs in Donut-authored context
and handoffs, rather than the installed public `dough-*` skills.

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

One retained execution; no repeat or automatic venv-recovery correction is
confirmed. Kept unqueued below the recurring groups. The pinned version and
held-stack parts were delivered (`f70984790f`, `ee414fbdf1` through `1ae39ec64f`)
and are outside this remaining finding.

### DD-161 — A `.venv-mineru` whose Python was garbage-collected had to be rebuilt by hand

A `.venv-mineru` whose Python lived in a garbage-collected Nix store path must
be rebuilt by hand. The plan's manual slice needed real MinerU to `/attach` real
PDFs through the CLI.

#### Occurrences

- Execution: SEED-059#story-3 / slice-plans/051-pdf-layout-from-bookmarks / 946e2a70e3; Timestamp: 2026-09-29 (slice 6, after 6f36cb2952); Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: slice 6 agent report (1,692 s, about 197k tokens, 96 tool uses); plan premise "`.venv-mineru/bin` has no `python`"; venv rebuilt with Homebrew Python 3.12, `mineru[pipeline]==3.4.5` and `six`.
  - Observed effect: about 28 minutes for one manual slice, most of it environment repair and stack scaffolding rather than observation.
  - Inference: a supported way to rebuild the pinned MinerU environment after its Python disappears could reduce this remaining setup cost. The recorded 28 minutes also includes stack scaffolding, so it is not a measurement of venv recovery alone.

## Open, not queued: Development startup on stale compiled classes

### DD-159 — The Development stack failed to start on stale compiled backend classes in the default checkout

`pnpm dev` from the default checkout failed with `No qualifying bean of type NotebookGitCutoverService`: `backend/build/classes` still held classes from before that service was removed. Deleting `backend/build/classes` let the next start succeed. Its cause is unknown.

#### Occurrences

- Execution: SEED-054#story-1 / `4f2f230505:.planning/slice-plans/011-book-reading-uat/PLAN.md` / 681768a71b; Timestamp: 2026-09-29T07:25:42+08:00; Tool: Claude Code; Model: claude-opus-5-5; Open Dough release: 0.3.46.
  - Evidence: `dev.log` "APPLICATION FAILED TO START" with the missing-bean message; `git grep NotebookGitCutoverService -- backend/src` found nothing; the next `pnpm dev` was healthy.
  - Observed effect: one failed start and a short diagnosis before the UAT setup could continue.
  - Inference: the Development start's incremental build did not drop classes whose sources were deleted. Qualified: cause not investigated further.
- Investigation (2026-10-03, SEED-067#stacks-survive-other-builds slice 2): not reproduced. In the default checkout, a compiled `@Service` and its injecting consumer were deleted with the stack stopped, then `pnpm dev` started healthy and Gradle removed the stale class; the same held with a concurrent `classes --rerun-tasks` build and with a build-cache restore. Linked-worktree probes also removed the classes. No writer found; the cause remains unknown. Owner decides whether to keep watching or drop.

## Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
