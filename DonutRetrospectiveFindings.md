# Donut Retrospective Findings

Findings specific to Donut’s product, repository tooling, or local conventions.
Findings about shared Open Dough skills, supporting guidelines, and skill scripts
remain in [DearDough.md](DearDough.md), including those observed in this project.
Existing finding identifiers and occurrence evidence are preserved when moved.
Resolved problems are removed; Git history keeps their evidence.

A finding is Donut’s when the remaining problem concerns Donut’s product,
scripts, or Donut-authored guidance. A repaired product incident alone does not
establish a defect in published Open Dough guidance. Shared planning, proof
acceptance, and delivery failures remain in DearDough when the observation
concerns those responsibilities, even after their product symptom is repaired.

## Ownership and priority assessment

Reviewed 2026-10-12 against Donut `8ac439b16b` and published Open Dough
`v0.3.60` (`e72ed2aaea475c7fbe8427a5c66bc877dda51275`). The installed
[execution wrap-up](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-execute-plan/references/wrap-up.md)
and [refactor checks](https://github.com/terryyin/open-dough/blob/v0.3.60/src/skills/dough-post-change-refactor/references/refactor-checks.md)
match that release. Public refactor guidance retains negative assertions when
absence is the actual promise and defers to project test style. Donut’s
principle 7 and `unit-testing` skill supply the stricter removal convention.
The queued work therefore belongs in Donut-authored guidance and evidence
handling; it does not change public `dough-*` skills.

| Group | Distinct recorded executions | Impact and disposition |
| --- | --- | --- |
| Donut removal proof and evidence handling | Four after the October 3 correction, including two after the October 10 correction; plus original DD-202 | Repeated invalid proof handoffs; one altered dated observation and broken link. One recurrence story queued first. |
| MinerU venv recovery | One | About 28 minutes of mixed environment and stack work; recovery remains unqueued. |
| Stale Development classes | One; later probes healthy | Failed start, unknown cause; unqueued, not confirmed resolved. |

Only the first group has repeated post-correction evidence and an unresolved
disposition for dated observation records encountered by a removal sweep. The one-off environment findings do not
justify a second highest-priority story. The backlog’s voice-input direction
stays unchanged; the recurring group directly arose during voice-input work.

Shared findings retained in DearDough include changed-boundary proof acceptance
(DD-218; public wrap-up step 2), decisive planning premises (DD-224; public
`dough-slice-planning`), proof-table coverage, sizing, file-size rules, and CI
observation/delivery. Their remaining claims concern the shared workflow, not
an unrepaired Donut helper or voice-control defect. Successful local practice
is separated below from open problems and has no backlog entry.

<a id="local-removal-proof"></a>

## Queued first: Apply Donut’s removal convention to proof and dated evidence

Follow-up: [Make Donut removal handoffs preserve valid proof and truthful evidence](.planning/seeds/SEED-071-reliable-project-feedback.md#removal-proof-and-evidence-recurrence)
— SEED-071#removal-proof-and-evidence-recurrence.

The earlier SEED-071#removals-follow-project-rule completed on 2026-10-10 and
is not active in either backlog list. Its correction `b96f234fb8` (14:25 +09:00)
stated removal proof in principle 7 and the `unit-testing` skill; a one-time
demonstration passed for two earlier shapes
(`231b5e320d:.planning/slice-plans/013-removal-proof-in-guidance/PLAN.md`).
That demonstration was not evidence of later real-plan compliance.

DD-225 occurred that evening (22:17 +09:00). DD-227’s plan was created at
`7d062d701f` on October 11 at 06:56 +09:00 and caught during execution at
08:00. Both follow the correction. DD-227 reopens the proof-handoff problem;
DD-225 adds the local sweep’s handling of dated evidence to the same group.
The new recurrence story preserves the predecessor’s done record.

DD-205 already followed an earlier correction of Donut DD-202 (not the DD-202
that became ODF-208): `18b99b6424` and `52460a4077` put the local convention in
always-loaded guidance on October 3. Its two later executions each wrote and
removed proof. Guidance publication and isolated demonstrations have therefore
not yet established effectiveness on real removals.

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

### DD-225 — A removal sweep's search terms matched a dated observation record, so the implementer reworded history to make the sweep return nothing

Plan 064's sweep reading required searches for the removed control names to return nothing over the product. One name appeared in `docs/voice-input-observations.md`, a dated record of journeys run with that control.

#### Occurrences
- Execution: SEED-066#single-button-voice-input (first implementation commit 5c3ca7033b)
  - Timestamp: 2026-10-10T22:17:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: slice 2 return, decision 3 (five mentions reworded to "requested (mid-speech) conversion", heading renamed, a seed link's anchor broken); the coordinator's anchor repair in `.planning/seeds/SEED-066-voice-input.md`; slice 2 refactor return ("a truthful rewrite means deleting or re-running the observation, which is an owner decision")
  - Observed effect: a dated observation now describes an author-requested conversion the product does not offer; one planning link broke and was repaired during delivery; the question was left for the owner
  - Inference: the removal rule and the plan's sweep did not say whether a dated observation record counts as documentation to sweep, to delete, or to leave

Current qualification (2026-10-12): the observation document explicitly dates
its evidence to October 3 and says it is not a guarantee about later revisions.
“Requested conversion” can describe the original action. The recorded concern
is unresolved evidence disposition and anchor churn, not confirmed falsification
of the observations or a claim that the current product offers that action.

### DD-227 — A slice plan asked for a test that the removed status line is absent, against the project's removal rule

Plan 065 slice 5 listed "no `[role="status"]` anywhere" among its proof for removing the status line. The project's principle 7 forbids a check that a removed thing is absent.

#### Occurrences
- Execution: SEED-066#unobtrusive-selection-aware-spoken-title (first implementation commit 57a659f018)
  - Timestamp: 2026-10-11T08:00:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: `7d062d701f:.planning/slice-plans/065-unobtrusive-spoken-title/PLAN.md` slice 5 Proof; `CLAUDE.md` principle 7 ("no check that it is absent"); the coordinator's slice 5 brief ("principle 7 overrides that: do not add absence assertions"); slice 5 return, decision 2
  - Observed effect: the coordinator caught it while writing the brief and the test was not written; the plan had passed slice planning and a readiness assessment with the instruction in it
  - Inference: planning checked the removal as deletion plus sweep in its scope text but not in each slice's proof list

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

## Useful project practice (not an open problem)

The following is a successful Donut-specific observation technique. It is not
an unresolved defect and contributes neither to issue frequency nor priority.

### DD-230 — An environment-only Flyway callback with a MySQL named lock held a real startup migration open for full-stack proof

To observe the runner during a migration, the execution supplied `beforeMigrate.sql` through `SPRING_FLYWAY_LOCATIONS` from a directory outside the checkout; the file waited on a named lock that a separate session held and released. No product or test code and no delay option was added.

#### Occurrences
- Execution: SEED-067#start-e2e-after-migrations (first implementation commit f297a916c5)
  - Timestamp: 2026-10-11T09:18:00+09:00
  - Tool: Claude Code
  - Model: claude-opus-5-5
  - Open Dough release: 0.3.58
  - Evidence: plan 066 Learnings "Slice 3" and "Slice 4" at `af62c7cd0a`; `sut.log` lines 148 to 234 in the execution worktree (`Executing SQL callback: beforeMigrate`, `Successfully applied 41 migrations`)
  - Observed effect: both `pnpm e2e:hold` and `pnpm cy:run` were observed returning 503 during the held migration and resetting test data only after the first 200; each slice took about five minutes including startup
  - Inference: useful practice: a timing fault was proved at the real boundary with a release barrier instead of a sleep or a stand-in response; it depends on the runner passing its environment to the backend, which was confirmed with `ps eww`

## Reopening

Reopen a project finding when a new occurrence contradicts its actual correction,
link that evidence to the existing story or correction if still active, and
otherwise queue a bounded recurrence story. A previously recorded resolution
without supporting correction evidence is insufficient to close a finding.
