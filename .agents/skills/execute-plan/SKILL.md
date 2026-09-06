---
name: execute-plan
description: >-
  Autonomously execute a plan under .planning/phases/ or
  .planning/quick/ .
  Applies local wrap-up on every slice: Jidoka, post-change-refactor,
  selective formatting, plan update, commit, and push. Observes CI
  asynchronously and coordinates failure repair. Parallel waves OK when safe.
  Triggers on: execute plan, run plan, execute slices, start plan,
  do .planning, execute .planning, run .planning.
---

<objective>
Execute a GSD-aligned PLAN autonomously with the per-slice delivery contract in
`.cursor/rules/gsd-coexistence.mdc`, including when using `/gsd-execute-phase`.
</objective>

<context>
**Mandatory first read:** `.cursor/agent-map.md` (navigation + focused test commands).
Before executing, also read [delegation.md](references/delegation.md),
[disposable-research.md](references/disposable-research.md), and
[destructive-later-outcome-check.md](references/destructive-later-outcome-check.md),
and [wrap-up.md](references/wrap-up.md) in full. Read
[ci-monitor.md](references/ci-monitor.md) for the background observer and
pause/stash/repair/resume protocol, then only its adapter for your host
(Codex, Cursor, or Claude Code).

**Input:** A PLAN under `.planning/phases/NN-slug/` or `.planning/quick/NNN-slug/`
(`PLAN.md` or `*-PLAN.md`) for one selected story or bounded GSD phase; reject
seeds. Every slice/wave must pass `problem-decomposition.mdc`'s stop-safe
Behavior/Structure gate. Refine coarse or low-confidence leaves on the same
PLAN before implementation; straightforward leaves may execute directly.

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …` unless on Cloud VM (use **cloud-vm-setup**
skill — no nix prefix there).

**Ownership:** Delegate each slice to a fresh agent under `delegation.md`;
implement locally only for a single interactive slice. The coordinator owns
the mandatory `wrap-up.md` sequence, including independent refactor, formatting,
plan update, commit, and push. Implementers stop before wrap-up.

**Resume:** Use the PLAN's status, learnings, and adjusted leaves as execution
state; reconcile ownership and evidence under planning.mdc's Proof decisions.
Do not write `.planning/STATE.md` or use it as execution/resume state.

**Parallelism:** Only when touch sets and PLAN writes do not overlap. Each unit
completes its own coordinator-owned wrap-up before dependent work starts.
</context>

<process>

<preflight_gate name="jidoka_stop_conditions">
Check before delegation and after both implementer and refactor returns,
even when tests pass. Stop for:

- unresolved user-facing value trade-offs or structural choices affecting
  future slices/architecture;
- missing credentials/permissions or ambiguity that could waste a commit;
- failures unresolved by focused diagnosis (for CI, first use `ci-monitor.md`);
- evidence changing the selected story's beneficiary, outcome, evaluation,
  boundary, or sibling delivery/order.

Explain the finding and required decision, then wait for the developer. At a
post-slice stop, first deliver safe work as specified in `wrap-up.md`.
Resolve routine naming/placement/test choices, minor refactoring, and failures
caused by your own change without stopping.

Discount a failure as pre-existing, unrelated, or environmental only with
bounded evidence connecting it to a cause and the proof affected. Put a short
cause, supporting observation, affected proof, and remaining defect or
disposition in the existing handoff or active PLAN; require no template or
record for every successful command. A passing retry, repeated test name, or
successful cleanup does not establish cause or repair. A targeted retry may
test a stated explanation but cannot replace it. On recurrence, compare current
observations and conditions with the recorded cause; reuse it when applicable
without erasing a remaining defect. Stop focused diagnosis once evidence justifies
a disposition; do not require complete root-cause analysis. Uncertainty after
that uses this Jidoka stop. A recorded explanation never waives required proof or
existing stop/repair rules. Proven infrastructure incidents retain `ci-monitor.md`
policy; that disposition covers only the supported attempt/cause and cannot
excuse a separate assertion failure.
</preflight_gate>

<step name="coordinator_loop">
```
1. Read the PLAN (slice headings/status or GSD tasks per planning.mdc). On the
   first entry, start one current-host CI observer for the execution before the
   first push; on resume, reuse its exact handle. Handle delivered CI events
   using ci-monitor.md; polling runs without AI.
2. Find the next slice whose status is NOT "done"
3. Check Jidoka, Behavior/Structure, refinement triggers, and planning.mdc's Proof
   decisions; for destructive work, run the [named later-outcome check](references/destructive-later-outcome-check.md)
   → If Jidoka stop condition → report & STOP
   → If the selected outcome is valid but a refinement trigger applies, invoke
     slice-plan-refinement on this PLAN, then reread it before continuing
4. Delegate implementation under references/delegation.md.
5. When implementer finishes:
   a. If Jidoka stop / REVERT & REFINE → handle as below; do not wrap up
   b. Apply the proof acceptance/reuse gate in references/wrap-up.md.
      Verify git status shows uncommitted work (or an explained empty slice).
   c. If the implementer already committed → process failure: stop and report
      (do not continue as if wrap-up succeeded). Prefer fixing by soft-resetting
      an unpushed commit only when safe and the developer has not forbidden it;
      otherwise wait for developer judgment.
6. Run references/wrap-up.md end-to-end; recheck Jidoka after refactor.
7. Go to step 1 (next slice)
8. All slices done → handle delivered CI events, stop observers without waiting
   for CI → clean up spent plan history (planning.mdc) → report & STOP
```

</step>

<step name="revert_and_refine">
A slice is too big when changes lack one coherent behavior, tests fail to
converge, or it exceeds `problem-decomposition.mdc`'s budget: scrutinize after
~5 minutes; after >10 minutes, refinement/retry is required unless a reason is
stated to the coordinator/developer. Include implementation and test runtime.

When this happens:

1. Inventory attempt-owned tracked/untracked paths and safely park or revert
   only that WIP. Preserve pre-existing changes; never use broad `git checkout .`
   or `git clean -fd`. Unclear ownership requires developer judgment.
2. Invoke **slice-plan-refinement** on the same PLAN for smaller leaves.
3. Have the coordinator commit and push the updated PLAN.
4. Return "reverted and refined" with elapsed time and whether the hard trigger
   applied.
</step>

</process>

<output>
Report completed slices and cleanup, or the active PLAN, next undone slice,
and required developer decision. Emit `## PLAN EXECUTION COMPLETE` only when all
slices are done; a Jidoka stop waits without that marker.
</output>
