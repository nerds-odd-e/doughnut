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

### 4. Preserve artifact ownership through staging — P1

- **Evidence:** Pygardon Quick 072 co-committed another process's skill changes
  using `git add -A`; explicit staging kept later commits scoped. Quick 073 had
  overlapping retrospective writers and stale planning references.
- **Proposal:** Assign one writer per shared seed/PLAN and one reconciliation
  owner; overlapping reviewers return read-only findings. Inspect status and the
  staged diff before commit. Stage only owned files or hunks when unrelated work
  exists; stage everything only after confirming the full diff is owned.
- **Owners:** Review delegation and coordinator `wrap-up.md`.
- **Success:** Concurrent work produces one attributable planning diff and
  scoped commits without lost findings or unrelated staged changes.

### 5. Enforce the no-edit refactor handoff — P1, small fix

- **Evidence:** Doughnut's final web-note receive refactor made no edits but
  reran all 2,180 backend tests because its prompt requested no-change
  confirmation, contradicting the installed skill.
- **Proposal:** Use one canonical delegation clause and reject contradictory
  prompt additions. No-edit reviews report `skipped — no refactor edits`;
  changed reviews name and rerun only proof invalidated by their edits. Do not
  count an unnecessary run as evidence that this process improvement worked.
- **Owners:** `execute-plan/references/wrap-up.md` and refactor handoff acceptance.
- **Success:** A live no-edit review runs zero tests; edits still receive the
  affected focused proof. Independent review remains mandatory.

### 6. Recover observer shutdown after compaction — conditional P1

- **Trigger:** Before another long unattended Doughnut execution.
- **Evidence:** Web-note receive closeout lost its observer handle after
  compaction. Terminating the cell left `ci-mailbox.mjs stream` running; exact
  PID shutdown was needed and terminal evidence could not be recovered.
- **Proposal:** Recover using stable observer/mailbox identity, stop only the
  exact observer, confirm exit, and retrieve or explicitly report missing
  terminal evidence. Avoid broad process-name kills.
- **Owner:** [SEED-011 Story 2](SEED-011-efficient-ci-failure-attention.md#story-2)
  and the Codex CI adapter. Revisit this narrow recovery gap without selecting
  the whole deferred repair scheduler or changing Pygardon's opt-in CI policy.
- **Success:** Simulated loss of volatile handles still permits exact shutdown
  and an honest final evidence receipt.

### 7. Prove uncertain storage assumptions before implementation — conditional P1

- **Trigger:** The next uncertain transaction or destructive migration change.
- **Evidence:** Doughnut reversed attempts involving rollback fixtures and
  `REQUIRES_NEW` locks. Pygardon Quick 069 tried two invalid DuckDB DDL sequences
  before a focused experiment established a viable migration.
- **Proposal:** Use one ephemeral, representative proof of the exact uncertain
  DDL or transaction/fixture ownership and critical postcondition. Record engine
  version, exact command, and result before declaring the affected work ready.
  Routine migrations do not require a generic research phase.
- **Owner:** Relevant planning readiness and storage/fixture guidance.
- **Success:** Implementation starts with demonstrated feasibility and avoids
  repeating an attempt based on the same untested assumption.

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
