---
name: slice-planning
description: >-
  Plan one clear, bounded story as executable GSD-aligned, stop-safe
  Behavior/Structure leaves. Straightforward plans execute directly;
  use slice-plan-refinement for complex plans or overruns.
---

<objective>
Write a sufficient one-pass PLAN using `.cursor/rules/problem-decomposition.mdc`
for slice decisions and `.cursor/rules/planning.mdc` for artifacts and lifecycle.
Resolve obvious multiple outcomes and unsafe stopping points now.
</objective>

<input_gate>
Require one user or stakeholder outcome, its value, evaluable key examples, and
boundaries from later stories. Use **story-refinement** for unresolved selected-story
detail; **story-decomposition** for unclear parent problems or candidate selection.
Never execute a decomposition seed directly.
</input_gate>

<locations>
- Existing GSD phase: `.planning/phases/NN-slug/*-PLAN.md`
- Ad-hoc selected story: `.planning/quick/NNN-slug/PLAN.md`
- Never create a new plan under `ongoing/`.
</locations>

<process>

<step name="record_the_story_understanding">
Read the home seed's refined story when present; record source, goal, scope, and
key examples without enlargement. Apply `planning.mdc` scope discipline: report
excluded uncertain additions; clarify necessary decisions through **story-refinement**.
</step>

<step name="inspect_execution_context">
Read only needed code, tests, stack rules, and relevant Accepted ADRs to identify
the stable outside-in test or demonstration entry point, behavior and tests to
extend, genuine dependencies, and whether the first Behavior needs preceding
Structure. Do not slice by file, component, or layer.

For a concrete uncertain storage assumption, name the behavior and critical
postcondition. Reuse matching evidence first; otherwise require one isolated
representative proof using the relevant engine and version and transaction
ownership and visibility semantics before declaring affected work ready. Uncertain
DDL requires the exact sequence and a representative later parent update;
uncertain rollback-fixture visibility to `REQUIRES_NEW` requires actual transaction
ownership with isolated data. Record assumption, engine and version, literal
command, observed critical postcondition, and result in the existing PLAN. Failed
proof changes the plan before broad implementation. Routine known behavior and
matching evidence require no new experiment. Do not invent uncertainty; keep
experiments off shared and production databases.
</step>

<step name="cut_and_order_leaves">
Apply `problem-decomposition.mdc`'s execution-leaf gate and initial sizing pass.

For every leaf:

1. Record required Behavior/Structure fields; tie Behavior to included scope or key
   examples and Structure to its immediate next Behavior.
2. Map contract promises to owning leaves, focused verification, and observations
   under `planning.mdc`'s Proof decisions.
3. Split independent postconditions, obvious hard-limit paths, and multi-beat slices.
4. Place Structure immediately before its Behavior; order Behaviors by user value,
   learning value, then genuine prerequisites.

Do not end a slice on CI-breaking red; keep a multi-beat E2E `@wip` until green.
Keep product and test artifacts capability-named.
</step>

<step name="decide_whether_refinement_is_needed">
Compare every leaf with only the `<refinement_triggers>` gate in
`.agents/skills/slice-plan-refinement/SKILL.md`. Run refinement only when requested
or required by a later execution workflow.

- If every leaf has one proof loop, a cohesive execution path, meets the target,
  and has no unexplained hard-limit path, report **ready for direct execution**.
- If any trigger remains, report **refinement recommended** and identify those
  slices. Do not claim an execution-time guarantee.
</step>

<step name="write_the_plan">
Use `planning.mdc`'s required contents and slice format. Rewrite GSD tasks bundling
Behaviors or speculative Structure before execution. Do not implement features.
</step>

</process>

<output>
Report the plan path, ordered leaves, considered-but-excluded additions, and one of:
`ready for direct execution` or `refinement recommended: <slices>`.
End with `## SLICE PLAN WRITTEN`.
</output>

<out_of_scope>
- Broad requirement exploration.
- Feature implementation.
- Direct execution of a story-decomposition seed.
</out_of_scope>
