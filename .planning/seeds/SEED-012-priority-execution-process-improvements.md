---
id: SEED-012
status: active
planted: 2026-09-06
planted_during: cross-project prioritization of Doughnut SEED-010 and Pygardon SEED-008
trigger_when: before the next relevant execution; see conditional priorities below
scope: medium
---

# SEED-012: Priority execution-process improvements

For the developer relying on autonomous delivery, acceptance must establish the
promised behavior, preserve owned work, and avoid repeated unproductive checks.
This is the consolidated home for P1 findings moved from
[Doughnut SEED-010](SEED-010-execute-plan-wrap-up-token-efficiency.md) and
[Pygardon SEED-008](../../../pygardon/.planning/seeds/SEED-008-efficient-plan-execution-evidence-handoffs.md).
These are proposed improvements, not executable leaves or adopted rule changes.

## Priority order

P1 means address before the next relevant execution. Items 6–7 have explicit
triggers. Each item records the minimum evidence, response, and success signal.

<a id="story-1"></a>

### 1. Reject evidence that contradicts the promised behavior — P1

**Status:** Installed 2026-09-06 in `execute-plan/references/wrap-up.md`.
Instruction replay: rejected a raw-provider expected value (A) and a
hidden exception-chain observation (B); reused already-inspected HTTP
safe-text evidence (C). Live product effectiveness is not claimed.

**Goal:** For the developer relying on autonomous delivery, catch a passing
assertion that permits behavior forbidden by the selected contract before the
slice is accepted, reducing later corrective work.

**Scope:** Strengthen the existing Doughnut coordinator acceptance step with one
bounded comparison: for the promises the current slice claims to prove, inspect
the relevant assertion and enough setup to identify the exercised boundary.
Compare the expected result with the promise, rather than accepting the test's
name or passing command as sufficient. Reuse evidence already inspected unless
its covered behavior changes. If contradictory or insufficient, name the exact
promise and observation gap and return it to implementation before acceptance.
Adequate proof continues through the existing workflow without a rerun.

Keep the addition at the existing acceptance instruction owner
(`execute-plan/references/wrap-up.md`), referring to `planning.mdc` rather than
duplicating its promise-mapping or lifecycle rules. This refinement replaces the
earlier proposal for a separate pre-cleanup aggregate scan.

**Exclusions:** No new review agent, acceptance stage, report format, automated
validator, exhaustive scenario matrix, whole-plan/repository audit, or automatic
test rerun. No new product requirements, product fixes, refactor responsibilities,
or changes to sibling P1s. Pygardon provides evidence; rollout there is separate.
The check covers selected promises, not an attempt to discover every unstated
requirement or prevent every behavioral omission.

<a id="story-2"></a>

### 2. Diagnose failures before accepting a baseline exception — P1

**Status:** Installed 2026-09-06 in `execute-plan/SKILL.md` Jidoka/failure
classification. Instruction replay: rejected an unexplained retry as unrelated
(A); reused leftover-user diagnosis while retaining the isolation defect (B);
accepted a proven runner-disconnect incident without reconfirmation (C). Live
effectiveness is not claimed.

**Goal:** For the developer relying on autonomous delivery, prevent a transient
pass or cleanup from concealing an unresolved verification defect, while avoiding
repeated diagnosis of failures already explained by applicable evidence.

**Scope:** Strengthen the existing Doughnut execution decision to discount a
failure as pre-existing, unrelated, or environmental. Before accepting that
exception, require bounded evidence connecting the observed failure to its cause
and explaining which proof is affected. Record the cause, supporting observation,
and remaining defect or disposition briefly in the existing handoff/active PLAN.
A passing retry, repeated test name, or successful cleanup alone is insufficient.

Diagnosis need only support the disposition; it does not require an exhaustive
root-cause investigation. A targeted retry may test a stated explanation, but
cannot replace it. If the cause remains uncertain after focused diagnosis, use
the existing Jidoka path. Fix ordinary failures caused by the current change
through the existing implementation loop without a new approval step.

On recurrence, compare new evidence with the recorded cause and conditions.
Reuse an applicable diagnosis, but keep an unresolved isolation or other defect
explicit and apply existing stop/repair rules. Do not normalize recurrence through
retries or cleanup. A note or an unrelated-failure label does not authorize
bypassing required proof. Proven infrastructure incidents retain the existing CI
disposition; concurrent test defects still require attention.

Keep the change at the existing `execute-plan` Jidoka/failure classification.
Reuse `ci-monitor.md`'s current policy; do not create competing local and CI
checklists or a new acceptance stage.

**Exclusions:** No baseline suite before every slice, automatic rerun policy,
new agent, failure registry, metrics system, CI observer/repair-scheduling change,
or blanket halt on the first test failure. No database cleanup commands, test
isolation repairs, or other product fixes in this story. Pygardon rollout and
other P1s remain separate.

<a id="story-3"></a>

### 3. Escalate repeated overruns to story review — P1

**Status:** Installed 2026-09-06 in `problem-decomposition.mdc` Learning
escalation, with entry points in `execute-plan` and `slice-plan-refinement`.
Instruction replay: counted a 12- then 13-minute replacement as two attempts and
sent unresolved scope to story review with parked proof kept (A); presented a
demonstrated daily-only outcome and remaining archives at story review without
dropping remainder or auto-running archive leaves (B); treated a recorded
focused-test exception and a same-boundary E2E checkpoint as non-triggers (C).
Live effectiveness is not claimed.

**Goal:** For the developer funding autonomous delivery, recognize when a story
should be narrowed or split before spending another attempt on smaller execution
leaves, with compatible work and proof available to resume.

**Scope:** At the existing coordinator refinement decision, reassess the story
boundary before another leaf-only refinement when either condition holds:

- A second non-exempt greater-than-ten-minute implementation/proof attempt
  occurs in the same selected story. Retries and replacement leaves count;
  renaming or splitting a leaf does not reset the evidence. An established
  exception for a focused test or external wait does not count as a sizing
  failure. Record the reason when handling the trigger, before more work, not
  retrospectively after finishing the attempt.
- Evidence already encountered in execution establishes a useful narrower
  Valuable, Visible, Vertical outcome that changes the assumed story boundary.
  Merely completing a planned E2E checkpoint does not trigger another review.

Use the existing PLAN's elapsed-time/proof notes to state what was delivered,
what remains, and whether the problem is leaf sizing or story scope. When a
boundary change is indicated or unresolved, use the existing `awaiting story
review` route for developer judgment before continuing. Recommend the smallest
useful cut; do not autonomously cancel remaining scope or rewrite sibling stories.
If the evidence supports retaining the story, record the concrete sizing reason
and continue the existing refinement path; repeating "make leaves smaller" alone
does not resolve the trigger. Reassess on a further qualifying overrun.

Preserve compatible attempt-owned work safely with its completed proof while
reconsidering scope. Reuse it when the revised leaf's promise and covered boundary
still match; it must still pass ordinary review and delivery gates. Discard work
only for a stated incompatibility or safety reason, not merely to restart a
timer. Preserve unrelated work under the current ownership rules.

**Exclusions:** No timer service, new reporting cadence, metrics registry,
additional reviewer, mandatory pre-execution refinement, automatic story split,
or whole-plan audit. No new sizing bands or Behavior/Structure policy, generic
work-recovery tooling, retrospective replanning of Pygardon, or sibling P1 work.
Do not add a stop to ordinary within-budget execution or to justified external
waiting. Initial instruction rollout is Doughnut only.

<a id="story-4"></a>

### 4. Preserve artifact ownership through staging — P1

**Status:** Installed 2026-09-06 in `execute-plan/references/wrap-up.md` and
`execution-retrospective/SKILL.md`. Instruction replay: owned product edit A
with unstaged skill edit B → stage A only, leave B, no extra approval; separable
owned/unrelated hunks → stage the owned hunk; unrelated already staged or
ambiguous hunks → resolve the boundary first, do not co-commit or silently
alter the other task's index/worktree; all content owned → whole-change staging
allowed. Two authorized reviews of one PLAN → one writer/reconciler, the other
read-only; disjoint PLANs or one reviewer → existing path. `wc -w`: wrap-up.md
558 → 557; execution-retrospective/SKILL.md 1794 → 1792. Observe the next
natural mixed-worktree or shared-review case; live effectiveness is not claimed.

**Goal:** For the developer reviewing concurrent work, keep each planning edit
and delivery commit attributable to its intended task, without overwriting or
co-committing another task's changes.

**Scope:** Two small instruction guards at the existing ownership boundaries:

- **Commit preparation:** Review current status and the complete staged diff
  against known task-owned work. When unrelated changes exist, stage only owned
  files or attributable hunks. Staging everything is appropriate only when the
  full diff is confirmed task-owned. Inspect the final staged content before
  committing; a shared filename alone does not establish ownership of its edits.
  If unrelated work is already staged or hunk ownership is ambiguous, resolve
  the commit boundary with its owner before committing; do not silently include,
  unstage, reset, or revert that work. Ordinary unrelated unstaged files do not
  require a stop or permission to commit clearly owned changes.
- **Shared retrospective artifacts:** Before parallel review activities write
  a shared seed/PLAN, designate one writer who also reconciles their findings.
  Other reviewers of that artifact return read-only evidence to the writer.
  Reconcile against the current artifact once before writing. Disjoint artifacts
  may have independent writers; a single review needs no additional ceremony.

Use existing task/delegation context for ownership; no registry or new tracking
artifact. Initial rollout is Doughnut only, in coordinator `wrap-up.md` and the
existing `execution-retrospective` context. This authorizes neither new parallel
reviews nor writing artifacts beyond the review's existing authorized scope.

**Exclusions:** No Git hooks, index-management tool, locking system, worktree
automation, cross-task discovery service, general concurrency protocol, history
rewrite, or cleanup of existing mixed commits. No default prohibition of
`git add -A` when everything is owned. No new review stage, Pygardon rollout, or
sibling P1 changes. Preserve the current independent refactor and check-only hook.

<a id="story-5"></a>

### 5. Enforce the no-edit refactor handoff — P1, small fix

**Status:** Installed 2026-09-06. Four supplied instruction replays passed;
`wrap-up.md` shortened from 557 to 547 words. Independent refactor made no edits
and skipped tests. Live effectiveness is not claimed.
Execution: [Enforce decide-first refactor handoffs](../quick/015-enforce-decide-first-refactor-handoffs/PLAN.md).

**Goal:** Avoid unnecessary test runs caused by coordinator-generated refactor
instructions while retaining independent review and proof for refactor edits.

**Evidence:** A no-edit web-note receive refactor reran all 2,180 backend tests
because its delegation requested no-change confirmation. The existing refactor
skill already forbids that; the gap is applying it at delegation and return.

**Scope:** In `execute-plan/references/wrap-up.md`, replace the current refactor
delegation/acceptance wording with one concise source clause: decide first;
no edits means no tests and `skipped — no refactor edits`; edits mean only
invalidated focused proof, with a reason for any replacement command. The
coordinator uses that clause without contradictory additions and checks the
existing handoff for consistency, not just its completion marker.

Correct a contradictory prompt before dispatch. If an unnecessary run already
occurred, report the deviation honestly and reuse otherwise valid evidence;
do not rerun review/tests just to obtain a compliant-looking handoff. Actual
failures still follow existing diagnosis rules. Explicit developer-requested
verification remains authoritative; this concerns routine refactor delegation.

Replace repetition so this document becomes shorter under the seed's concision
constraint. Preserve prior acceptance/staging safeguards and the refactor skill's
existing policy; no change to `post-change-refactor/SKILL.md` is needed.

**Exclusions:** No prompt generator/parser, new agent, handoff schema, test runner,
review stage, full-suite policy change, Pygardon rollout, or sibling P1 work.
Independent refactor, formatting, and hook ownership stay in place.

**Key examples:**

| Situation | Required outcome |
|---|---|
| Routine draft prompt adds “run the full suite to confirm a no-change review.” | Remove the contradiction before dispatch; retain the canonical decide-first clause. |
| Review reports no edits and `skipped — no refactor edits`. | Accept the consistent handoff without running tests. |
| Refactor edits invalidate proof A but leave B valid. | Rerun A, reuse B; explain a focused replacement if A's boundary moved. |
| A no-edit review already ran tests unnecessarily. | Record the deviation, preserve valid proof, and correct future delegation; do not claim compliance or repeat the run. |

**Evaluation / ROI:** Replay these four handoffs/prompts without real test runs.
Report before/after word counts and preserve existing obligations in review.
Observe the next ordinary refactor for live effectiveness; no dedicated product
exercise or monitor. No unresolved scope decision blocks slice planning.

<a id="story-6"></a>

### 6. Recover observer shutdown after compaction — conditional P1

**Status:** Installed 2026-09-06. Five focused lifecycle/stream tests passed,
including durable-note recovery, exact cooperative stop, retained failure evidence,
and an unaffected second observer. Missing identity/evidence instruction replays
remain unresolved. Adapter word count: 717 → 633. Real compaction is not claimed.
**Trigger:** Deliver before another long unattended Doughnut execution.

**Goal:** Let the coordinator close its exact CI observer and retain an honest
coverage receipt when Codex's volatile cell/session handles are unavailable.

**Evidence:** Web-note receive closeout lost its handles; terminating the cell
left the stream subprocess running and its terminal receipt unavailable. The
mailbox already persists events/results and supports `stop DIRECTORY`.

**Scope:** Reuse that cooperative stop command. Retain the initial mailbox
directory and stream PID in the existing execution PLAN, tied to its coordinator
and checkout. Add PID to the stream's existing startup receipt; create no new
registry. After handle loss, recover the exact saved directory, validate its
checkout/request, stop that mailbox, verify the recorded process exits within a
bounded local wait, and report terminal evidence with `pendingCi: unobserved`.
Preserve recorded failures; do not start a replacement merely to shut it down.

If identity is missing/mismatched, or completion/exit cannot be established,
report unresolved shutdown or missing evidence. Do not guess by newest mailbox,
scan for a process to kill, or infer green CI. The recovered path uses PID only
for exit confirmation, not termination; keep ordinary saved-handle shutdown.

**Exclusions:** Automatic discovery of older unidentified observers, forced
termination of stuck streams, repair scheduling/resumption, delivery-acknowledgment
redesign, other hosts, and new monitoring infrastructure. This selects only the
shutdown subset of [SEED-011 Story 2](SEED-011-efficient-ci-failure-attention.md#story-2).
Other repair work stays deferred. Shorten the changed Codex adapter instructions.

**Key examples:** Saved identity plus lost volatile handles → exact mailbox
stops, process exits, and its persisted failure/coverage receipt remains usable;
another observer stays running. Missing or mismatched identity → no guessed
stop. Missing terminal evidence or unconfirmed exit → explicit unresolved result,
without a replacement observer or broad kill.

**Evaluation:** Extend the existing Codex process-lifecycle proof with real
local subprocesses and fake GitHub input. Discard the coordinator handle map;
recover only from the durable note. No live GitHub run or actual context
compaction is required. This demonstrates recovery mechanics, not a general
cross-context repair system. No unresolved scope decision blocks planning.

<a id="story-7"></a>

### 7. Prove uncertain storage assumptions before implementation — conditional P1

**Status:** Installed 2026-09-06. Three supplied readiness replays passed:
uncertain DDL and transaction visibility require relevant isolated proof; known
behavior or matching evidence proceeds without a new experiment. Skill word count:
630 → 536. No database experiment or live effectiveness claim.
**Trigger:** Apply the readiness check when a proposed transaction/destructive
migration has a concrete unresolved engine or fixture-ownership assumption.

**Goal:** Avoid an implementation attempt based on an unproved storage behavior
without charging routine migrations for a generic research phase.

**Evidence:** Doughnut reversed work involving rollback fixtures and
`REQUIRES_NEW`; Pygardon tried invalid DuckDB DDL sequences before a small
experiment identified a viable one.

**Scope:** Tighten `slice-planning`'s existing execution-context check. Name the
specific uncertain DDL/transaction behavior and critical postcondition. Reuse
applicable existing evidence first; otherwise obtain one isolated representative
proof using the relevant engine/version and transaction semantics before calling
the affected work ready. Keep assumption, literal command, version, observation,
and result compactly in its existing PLAN. Failed proof informs the plan rather
than a speculative implementation attempt. Shorten the changed skill document.

**Exclusions:** No current database experiments or migration changes in this
process improvement; no mandatory spike, new test framework, fixture overhaul,
exhaustive migration matrix, or shared/production-database manipulation. Routine
known behavior needs no extra check. Pygardon rollout and sibling changes stay out.

**Key examples:** Uncertain rename/FK semantics without evidence → require a
throwaway proof of that sequence and a representative later parent update;
uncertain rollback-fixture visibility to `REQUIRES_NEW` → prove the actual
transaction ownership with isolated data before broad migration; matching
existing evidence or routine known behavior → reuse/continue without a new run.

**Evaluation:** Replay those three readiness decisions using supplied evidence.
Judge live value at the next naturally occurring matching storage change;
do not manufacture one for this instruction update. No unresolved scope decision
blocks planning.

## Evaluation and provenance

Replay each selected failure, then evaluate the next applicable real execution.
Keep installed changes distinct from demonstrated effectiveness. Retain compact
proof and commit references; do not add agents, duplicate checklists, or a new
tracking framework. Preserve the direct formatter, reusable focused proof, and
fresh independent refactor review.

Source execution boundaries for recovering detail from Git:
Doughnut clone/publication through `dd1ca6415a`, web-note receive through
`a5278d667f`; Pygardon Quick 069 through `4951c03e`, Quick 072 through
`5d02400cf`, Quick 073 at `7865a330e`. Earlier proof-contract adoption:
Doughnut `e6ea35d4f97250809ebb8676edfcca9be3e3f2d1`.
