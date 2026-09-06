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

**Status:** Installed 2026-09-06 in `4253b3b954`. Four supplied instruction
replays passed; `wrap-up.md` word count 557 → 547. Independent refactor made no
edits and skipped tests. The later local-note execution supplied natural evidence:
its final no-edit review also skipped tests. Agent-capacity exhaustion and a lost
isolated-review output caused a repeated review, not a recurrence of this
decide-first/no-test defect; that efficiency finding remains in SEED-010.

**Goal:** Avoid unnecessary test runs caused by coordinator-generated refactor
instructions while retaining independent review and proof for refactor edits.

**Scope:** The existing `execute-plan/references/wrap-up.md` delegation and return
steps enforce decide-first proof, correct contradictory additions, and inspect
handoff consistency. No edits means no tests; edits rerun only invalidated focused
proof, explaining replacements. Prior unnecessary runs are reported as deviations
without repeated verification or concealed failures. Explicit developer verification,
acceptance, staging, independent refactor, formatting, and hook ownership remain.
No new agent, schema, runner, review stage, full-suite policy, or Pygardon rollout.

<a id="story-6"></a>

### 6. Recover observer shutdown after compaction — conditional P1

**Status:** Installed 2026-09-06 in `99870b8e88`. Five focused lifecycle/stream
tests passed, including saved-note recovery, exact cooperative stop, preserved
failure evidence, and a live second observer. Missing identity/evidence replays
require an unresolved report. Adapter word count 717 → 633. The later local-note
execution supplied natural compaction evidence: after volatile handles were no
longer usable, the retained PLAN identity selected the exact mailbox, cooperative
stop returned `pendingCi: unobserved`, and the recorded PID was confirmed exited.
Broader recovery effectiveness is not claimed; its startup bridge recovery is a
separate SEED-010/SEED-011 candidate.

**Goal:** Let the coordinator close its exact CI observer and retain an honest
coverage receipt when Codex's volatile cell/session handles are unavailable.

**Scope:** Stream receipts include PID; the existing PLAN retains directory/PID,
coordinator, and checkout. Lost-handle shutdown validates that note and request,
uses the existing cooperative mailbox stop, confirms recorded PID exit within a
finite wait, and preserves terminal/failure evidence and unobserved CI coverage.
Missing identity or evidence remains unresolved. Normal saved-handle and other-host
policies remain. No PID signals on recovery, guessed discovery, replacement launch,
forced termination, new registry, acknowledgment changes, or repair scheduler.
Older unidentified observers and SEED-011's remaining repair work stay deferred.

<a id="story-7"></a>

### 7. Prove uncertain storage assumptions before implementation — conditional P1

**Status:** Installed 2026-09-06 in `f4e9e290e5`. Three supplied readiness replays
passed: uncertain DDL and transaction visibility require relevant isolated proof;
known behavior or matching evidence needs no experiment. Skill word count 630 → 536.
No database experiment or live effectiveness claim.

**Goal:** Avoid implementation based on unproved storage behavior without charging
routine migrations for generic research.

**Scope:** `slice-planning` checks concrete unresolved storage assumptions, reuses
matching evidence, or requires isolated representative proof before affected work
is ready. DDL covers the exact sequence and a later parent update; fixture proof
covers actual transaction ownership/visibility. The existing PLAN records assumption,
engine/version, literal command, observed critical postcondition, and result.
Failed proof changes the plan; routine known behavior needs no new run. No invented
uncertainty, current database experiment, shared/production mutation, mandatory spike,
fixture overhaul, new framework, Pygardon rollout, or sibling changes.

## Evaluation and provenance

Replay each selected failure, then evaluate the next applicable real execution.
Keep installed changes distinct from demonstrated effectiveness. Retain compact
proof and commit references; do not add agents, duplicate checklists, or a new
tracking framework. Preserve the direct formatter, reusable focused proof, and
fresh independent refactor review.

Source execution boundaries for recovering detail from Git:
Doughnut clone/publication through `dd1ca6415a`, web-note receive through
`a5278d667f`, and local-note creation through `d2d8fb9ff5`; Pygardon Quick 069
through `4951c03e`, Quick 072 through `5d02400cf`, Quick 073 at `7865a330e`.
Earlier proof-contract adoption: Doughnut
`e6ea35d4f97250809ebb8676edfcca9be3e3f2d1`.
