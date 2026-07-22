---
name: phased-planning
description: >-
  Decompose tasks into GSD-aligned phased plans with Behavior/Structure
  grammar (one observable behavior per phase, stop-safe). Use when planning
  new features, breaking down large tasks, or when a task is too large to finish
  safely in one pass.
  Triggers on: plan, decompose, phases, break down, task too large, stuck.
---

<objective>
Decompose a task into GSD-aligned phased plans obeying **Behavior/Structure**
grammar: stop-safe, one observable behavior per phase.

Purpose: Planning entry point for new features, oversized phases, and ad-hoc
work not yet on the roadmap.

Output: Written plan under `.planning/` + summary ending with
`## PHASED PLAN WRITTEN`.
</objective>

<context>
**Hard grammar (non-negotiable):** Every phase is **Behavior** or **Structure**,
**stop-safe**, and carries **one** observable behavior (or one structure change
for the **immediate next** behavior only). Full rules: `.cursor/rules/planning.mdc`.

**Where to put plans (GSD-aligned):**

| Location | Use |
|----------|-----|
| `.planning/phases/NN-slug/` | Roadmap / milestone — GSD `*-CONTEXT.md`, `*-PLAN.md`, `*-SUMMARY.md`, … |
| `.planning/quick/NNN-slug/` | Ad-hoc slices not yet on the roadmap |
| `ongoing/` | Legacy only — do not add new plans |

Primary executable file: `*-PLAN.md` or `PLAN.md`. Sub-decomposition: additional
`*-PLAN.md` in the same directory or clearly marked sub-phase sections — each
still Behavior/Structure.

**History:** keep resume-useful status and brief learnings while in progress; when
the whole plan is done and shipped as code/permanent docs, **clean up** spent
planning history (`.cursor/rules/planning.mdc`).

**Git does not use the Nix prefix.**
</context>

<process>

<step name="decompose">
**Default:** Split by **user scenarios and outcomes**, not by layers (DB → API → UI)
or "build the abstraction first."

**Order scenarios** from **common / general** toward **more specific** preconditions.

**Solutions:** First phases implement a **narrow, concrete** slice. Later phases
**generalize or reuse** only after real repetition — not a big generic framework up front.

**Regression:** If behavior **already exists** but has **no automated test**, prefer
a **dedicated phase**: add a regression test and make it pass.

**Extending tests:** If similar behavior **already has** tests, extend them; avoid
duplicate test code. Fold "test fails → pass" into the feature phase, **or** use a
short phase where the **new** test fails first. **Keep at most one intentionally
failing test** while driving a change.

**Big refactor:** If making the test pass needs a **large structural** change, plan
**that structure as its own phase** before (or as the first slice of) the feature.

**E2E-shaped phases:** Each phase maps to an **end-to-end** scenario; different
phases may use **different preconditions**. Add or extend tests in **capability-named**
feature files (e.g. `note_creation.feature`) — **never** name files or scenarios after
the phase.

**Still too big:** Split by **one small part of the outcome** per phase.

### Testing strategy

| Layer | Role |
|--------|------|
| **E2E** | Each phase: tests covering the **main user behavior** for that phase. |
| **Unit tests** | Formatting, errors, invalid input, edge paths. Black-box, minimal, full coverage of those concerns. |

**Tests are owned by capability; phases only schedule work.**

**Observable behavior first:**

- Prefer tests driving **high-level entry points** (controllers, mounted components,
  CLI `run` / `runInteractive`) — not internal helpers unless that API **is** the
  deliberate isolated contract.
- Avoid tests mirroring code structure (internal-only parameters, bespoke mocks).
- **Minimum tests for same coverage** — fewer integration-style tests over many
  scattered unit tests pinning private functions.
- **Cohesion** — assertions for one user-visible behavior live together when practical.
- **Small unit tests** — pure functions, validation, error messages: inputs → outputs.

- **Test-driven:** tests first or alongside implementation.
- **Phase-complete:** everything in a phase justified and tested inside that phase.
- **No dead code:** production code used by current E2E **or** unit tests for
  non–happy-path behavior. Normal user paths need E2E, not unit tests alone.

**E2E scope:** "E2E for this phase" = relevant `--spec`(s), not the entire suite.
See `e2e-authoring.mdc`.

### E2E-led decomposition (sub-phases)

When a phase spans several user-visible beats:

1. **Red sub-phase** — Write full E2E scenario; tag `@wip`. Run `cypress run --spec`
   for that feature; confirm failure is for the **right reason**.
2. **Green sub-phase** — Smallest production change toward passing. No dead code.
3. Repeat until scenario passes; remove `@wip`.

### Test-driven workflow

1. Add or change E2E or unit test; run and confirm it **fails**.
2. Confirm failure for the **right reason** (not typo or env issue).
3. Improve assertion/message if unclear.
4. Smallest change that makes the test pass.
5. Refactor with tests green.
6. Remove `@wip` from passing E2E scenarios.

### Phase discipline

Before closing a phase: `.cursor/rules/planning.mdc` (clean up, tests, `@wip`,
Jidoka, plan update, deploy gate, parallelism).

### Interim behavior

- **Allowed** when it gets the feature to users faster or gives earlier E2E feedback.
- **Remove** when a later phase replaces it with the intended design.
</step>

<step name="write_plan_document">
Document **important structure and intent**. **Update** when learnings change
remaining work. Remove text that no longer helps the current snapshot.

Include:

- Phases with status (done / in-progress / planned) and type (Behavior | Structure)
- Key design decisions and rationale
- Discoveries that affect remaining work

**Naming rule:** Feature files, test files, classes, directories reflect **domain
capability** (e.g. `video_playback.feature`, `SegmentExportController`), not phase
number. Phase numbers belong only under `.planning/`.

If GSD `/gsd-plan-phase` / discuss produced a plan violating Behavior/Structure —
**rewrite or split** until it complies.
</step>

</process>

<success_criteria>
- Every phase is Behavior or Structure, stop-safe, one observable behavior
- Plan written to `.planning/quick/` or `.planning/phases/` (not `ongoing/`)
- Scenario-first ordering; capability-named permanent artifacts
- STATE.md updated when it exists
- Final output includes `## PHASED PLAN WRITTEN`
</success_criteria>

<output>
Report to the developer:

1. Plan location and phase summary.
2. Key design decisions.
3. Discoveries affecting remaining work.

```
## PHASED PLAN WRITTEN
```

Then wait for their decision.
</output>

<out_of_scope>
- Do not implement feature code during planning (except tiny fixes from retrospective).
- Do not add new plans under `ongoing/`.
- Do not encode phase numbers in product file/test names.
</out_of_scope>
