---
name: phased-planning
description: >-
  Decompose tasks into phased plans with one behavior per phase. Use when
  planning new features, breaking down large tasks, when a task has been
  in progress for over 10 minutes, or when the planning timer hook fires.
  Triggers on: plan, decompose, phases, break down, task too large, stuck.
---

# Phased Planning — Task Decomposition

## When to use

- Developer asks to plan or decompose a task.
- A phase or sub-phase is too large to implement in one pass.
- The planning-timer hook fires (you have been working 10+ minutes).

## If triggered by the 10-minute timeout

1. **Stop** implementing immediately.
2. **Review your conversation history**: what files did you search, what confused you, what approaches did you try, what failed? Identify the concrete reasons progress was slow.
3. **Summarize** what you have learned (discoveries, blockers, partial progress) — include the friction points from step 2.
4. **Stash** your changes: `git stash -m "WIP: <brief description>"`.
5. **Decompose** the remaining work into phases (see below).
6. **Write** the plan to `ongoing/<short-name>.md`.
7. **Report** to the developer and wait for their decision.

## Where to put plans

`ongoing/<short-name>.md` — informal, temporary. Update as work proceeds; remove when done.

For sub-decomposition of a single phase: `ongoing/<plan-name>-<phase-number>-sub-phases.md`.

---

## How to decompose into phases (scenario-first)

Every phase is one of two types:

| Type | What it does | Constraint |
|------|-------------|------------|
| **Behavior** | Delivers user value directly observable from the external perspective. | Must be externally observable and testable. |
| **Structure** | Restructures internals to prepare for the **immediate next** behavior phase, without changing any external behavior. | All changes must be **verifiable from the external perspective** (existing tests still pass, no observable difference). No speculative prep for phases beyond the next behavior phase. |

### Sequencing rules

1. **Stop-safe ordering** — The user may decide to stop after **any** phase. The value delivered must be proportional to (or greater than) the number of completed phases, with close to **zero waste** if remaining phases are never implemented.
2. **Order by value** — Earlier phases = higher user value and usability first.
3. **Many phases are good** — Split as finely as you can while each phase still meets the type constraints above.
4. **Structure phases only justify themselves through the next behavior phase** — A structure phase that is not immediately followed by the behavior phase it enables is speculative waste.

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

Phases decide **when** you add or extend tests, but tests are grouped and named by **what behavior they cover** — the domain capability, not the phase that introduced them. A phase may add scenarios to an existing capability-named feature file or create a new file named after the capability it introduces (e.g. `video_playback.feature`, `segment_export.feature` — never `phase_1.feature`).

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

Before closing a phase and starting the next:

1. **Clean up** — Remove dead or unreachable code.
2. **No failing tests** — All unit tests and existing E2E tests must pass at the **merge / CI** gate. No test may be left in a failing state at the end of any phase or sub-phase. **Locally and for AI agents:** satisfy this by running **targeted** E2E (`cypress run --spec` for the feature(s) you touched and any clearly related features); do **not** run the **full** E2E suite unless CI, the workflow, or the user **explicitly** requires it.
3. **`@wip` for not-yet-passing E2E scenarios** — When writing E2E scenarios test-first for behavior that is not yet implemented, tag them `@wip`. This tag runs normally in local development but is skipped in GitHub Actions CI. Remove the `@wip` tag once the scenario passes. CI enforces a maximum of 5 `@wip` scenarios across the entire E2E project to prevent accumulation.
4. **Deploy gate** — Commit, push, and let **CD deploy** before the next phase (unless the team explicitly agrees otherwise). Usually **manual** on the developer side.
5. **Update the plan** — Reflect what is done and what remains; drop obsolete notes.

### Interim behavior

- **Allowed** when it gets the feature to users faster **or** gives the team **earlier end-to-end feedback**.
- **Remove** interim behavior when a **later phase** replaces it with the intended design.

---

## Plan document

Document **important structure and intent** in the plan. **Update** when you learn something that changes how remaining phases should run. Remove text that no longer helps the **current** snapshot.

Include:
- Phases with status (done / in-progress / planned)
- Key design decisions and their rationale
- Discoveries that affect remaining work

**Naming rule for delegated work:** When a plan references feature files, test files, classes, or directories to create or modify, names must reflect the **domain capability** (e.g. `video_playback.feature`, `SegmentExportController`), not the phase or delivery order. Phase numbers belong only in the plan document itself, never in permanent artifact names.
