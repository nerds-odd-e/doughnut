---
name: phased-planning
description: >-
  Decompose tasks into GSD-aligned phased plans with Behavior/Structure
  grammar (one observable behavior per phase, stop-safe). Use when planning
  new features, breaking down large tasks, when a task has been in progress
  for over 10 minutes, or when the planning timer hook fires.
  Triggers on: plan, decompose, phases, break down, task too large, stuck.
---

# Phased Planning — Task Decomposition

## When to use

- Developer asks to plan or decompose a task.
- A phase or sub-phase is too large to implement in one pass.
- The planning-timer hook fires (you have been working 10+ minutes).
- GSD `/gsd-plan-phase` / discuss produced a plan that violates Behavior/Structure — **rewrite or split** until it complies.

## Hard grammar (non-negotiable)

Every phase is **Behavior** or **Structure**, **stop-safe**, and carries **one** observable behavior (or one structure change for the **immediate next** behavior only). Full rules: `.cursor/rules/planning.mdc`.

## If triggered by the 10-minute timeout

1. **Stop** implementing immediately.
2. **Review your conversation history**: what files did you search, what confused you, what approaches did you try, what failed?
3. **Summarize** what you have learned (discoveries, blockers, partial progress).
4. **Stash** your changes: `git stash -m "WIP: <brief description>"` — **unless** mid **GSD** execute with task commits that must reach SUMMARY; then stop and report without stash.
5. **Decompose** the remaining work into phases (see below).
6. **Write** the plan under `.planning/quick/NNN-slug/` (next free `NNN`) as `PLAN.md` (and update `.planning/STATE.md` if it exists). Prefer promoting into `.planning/phases/NN-slug/` when this work belongs on the roadmap.
7. **Report** to the developer and wait for their decision.

## Where to put plans (GSD-aligned)

| Location | Use |
|----------|-----|
| `.planning/phases/NN-slug/` | Roadmap / milestone phases — GSD `*-CONTEXT.md`, `*-PLAN.md`, `*-SUMMARY.md`, … |
| `.planning/quick/NNN-slug/` | Timer interrupts and ad-hoc slices not yet on the roadmap |
| `ongoing/` | Legacy only — do not add new plans |

Inside a phase or quick dir, primary executable file is `*-PLAN.md` or `PLAN.md`. Sub-decomposition: additional `*-PLAN.md` files in the same directory (GSD multi-plan) or clearly marked sub-phase sections — each still Behavior/Structure.

**History:** keep resume-useful status and brief learnings while in progress; when the whole plan is done and shipped as code/permanent docs, **clean up** spent planning history (`.cursor/rules/planning.mdc`).

---

## How to decompose into phases (scenario-first)

**Default:** Split by **user scenarios and outcomes**, not by layers (DB → API → UI) or by "build the abstraction first."

**Order scenarios** from **common / general** toward **more specific** preconditions.

**Solutions:** First phases implement a **narrow, concrete** slice. Later phases **generalize or reuse** only after you see real repetition — not a big generic framework up front.

**Regression:** If behavior **already exists** but has **no automated test**, prefer a **dedicated phase**: add a regression test and make it pass.

**Extending tests:** If similar behavior **already has** tests, extend them for the new behavior; avoid duplicate test code. Either fold "test fails → pass" into the same phase as the feature, **or** use a short phase where the **new** test fails first. **While driving that change, keep at most one intentionally failing test** (the one you are implementing toward).

**Big refactor:** If making the test pass needs a **large structural** change, plan **that structure as its own phase** before (or as the first slice of) the feature.

**E2E-shaped phases:** Prefer phases that each map to an **end-to-end** scenario; different phases may use **different preconditions** (setup, role, data). Each phase adds or extends tests in **capability-named** feature files (e.g. `note_creation.feature`, `spaced_repetition.feature`). A phase may add scenarios to an existing file or create a new file named after the capability it introduces — **never** name files or scenarios after the phase itself.

**Still too big:** If one precondition + one E2E story is still large, split by **one small part of the outcome** per phase (one aspect of the postcondition).

### Testing strategy

| Layer | Role |
|--------|------|
| **E2E** | Each phase: tests that cover the **main user behavior** for that phase. |
| **Unit tests** | Formatting, errors, invalid input, edge paths. Prefer **black-box** tests (inputs/outputs), minimal tests, full coverage of those concerns. |

### Tests are owned by capability, phases only schedule work

Phases decide **when** you add or extend tests, but tests are grouped and named by **what behavior they cover** — the domain capability, not the phase that introduced them.

### Observable behavior first (avoid structure-mapped tests)

**Preference:** Tests should stand on **what a user or integrator can observe** (HTTP response, DOM, terminal output, exit code, public error text), not on how the code is factored inside.

- **Prefer tests that drive a high-level entry point** — e.g. Spring **controllers**, **mounted** pages/components, CLI **`run` / `runInteractive`** (or subprocess). The test may **never import or call the code under change directly** and still exercise it **through the real call chain**. Use **direct** tests on internal helpers only when that API **is** the deliberate isolated contract (pure formatting, algorithm I/O).
- **Avoid tests that mirror code structure** — e.g. calling a low-level function with **internal-only parameters** (feature flags passed only from tests, bespoke `OutputAdapter` mocks) to assert behavior that really belongs to a full path. Those tests **break on harmless refactors** and **miss wiring bugs** (wrong argument at the real call site).
- **Minimum tests for the same coverage; no 1:1 test ↔ implementation map** — When behavior spans layers, prefer **fewer integration-style tests** (one per **observable surface** if surfaces differ — e.g. CLI TTY vs piped stdin) over **many** tests scattered across files that each pin a private function. **Do not** add a test file (or case) just because a new module exists if an existing entry-point test already proves the behavior.
- **Cohesion for one behavior** — Assertions for a single user-visible behavior should live **together** (one file or one focused `describe`) when practical, instead of duplicating partial checks in three places.
- **Where small unit tests still shine** — Pure functions, validation, error messages, edge inputs: **inputs → outputs** with no need to know caller shape.

- **Test-driven:** Prefer tests **first** or **alongside** implementation.
- **Phase-complete:** Everything in a phase is **justified and tested inside that phase**. No separate "final integration phase" or deferred test pass for that slice.
- **No dead code:** Production code must be **used** by current E2E tests **or** by unit tests that cover **non–happy-path** behavior. For **normal user paths**, unit tests alone are **not** enough to justify keeping the code — pair with E2E for that phase.

**Scope of E2E:** "E2E for this phase" means Cypress (or equivalent) coverage for the **behavior and feature files touched in that phase** — run the relevant `--spec`(s), not the **entire** suite. See `e2e-authoring.mdc` for running a single feature.

### E2E-led decomposition (sub-phases)

**When to use:** A planned **phase** still spans several user-visible beats (e.g. multi-step Gherkin). Prefer **sub-phases** that alternate **E2E red** and **minimal implementation green**, instead of front-loading backend or UI layers.

**Pattern (repeat until the scenario passes and `@wip` is removed):**

1. **Red sub-phase** — Write the **full** E2E scenario in the feature file and tag it `@wip`. Run E2E locally with **`cypress run --spec`** pointing at **that** feature file (not the whole suite); confirm the failure is for the **right reason** (behavior not implemented, not typos).
2. **Green sub-phase** — Implement the **smallest** production change that makes progress toward passing the scenario. **No dead code**; keep the change clean.
3. **Next** — Run the same **`--spec`** again. If the scenario still fails, go back to **2**. When all steps pass, remove the `@wip` tag.

### Test-driven workflow

When adding or changing behavior:

1. Add or change the **E2E or unit** test; **run it** and confirm it **fails**.
2. Confirm it fails for the **right reason** (not a typo or env issue).
3. If the failure is unclear, **improve the assertion or message** so the next reader learns what was wrong.
4. Implement the **smallest** change that makes the test pass.
5. **Refactor** with tests green, then continue.
6. Remove the `@wip` tag from any E2E scenario that now passes.

### Phase discipline checklist

Before closing a phase and starting the next: follow `.cursor/rules/planning.mdc` (clean up, tests, `@wip`, Jidoka, plan update, deploy gate, parallelism).

### Interim behavior

- **Allowed** when it gets the feature to users faster **or** gives the team **earlier end-to-end feedback**.
- **Remove** interim behavior when a **later phase** replaces it with the intended design.

---

## Plan document

Document **important structure and intent** in the plan. **Update** when you learn something that changes how remaining phases should run. Remove text that no longer helps the **current** snapshot.

Include:
- Phases with status (done / in-progress / planned) and type (Behavior | Structure)
- Key design decisions and their rationale
- Discoveries that affect remaining work

**Naming rule for delegated work:** When a plan references feature files, test files, classes, or directories to create or modify, names must reflect the **domain capability** (e.g. `video_playback.feature`, `SegmentExportController`), not the phase or delivery order. Phase numbers belong only under `.planning/`, never in permanent artifact names.
