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

**Default:** Split by **user scenarios and outcomes**, not by layers (DB → API → UI) or by "build the abstraction first."

**Order scenarios** from **common / general** toward **more specific** preconditions.

**Solutions:** First phases implement a **narrow, concrete** slice. Later phases **generalize or reuse** only after you see real repetition — not a big generic framework up front.

**Regression:** If behavior **already exists** but has **no automated test**, prefer a **dedicated phase**: add a regression test and make it pass.

**Extending tests:** If similar behavior **already has** tests, extend them for the new behavior; avoid duplicate test code. Either fold "test fails → pass" into the same phase as the feature, **or** use a short phase where the **new** test fails first. **While driving that change, keep at most one intentionally failing test** (the one you are implementing toward).

**Big refactor:** If making the test pass needs a **large structural** change, plan **that structure as its own phase** before (or as the first slice of) the feature.

**E2E-shaped phases:** Prefer phases that each map to an **end-to-end** scenario; different phases may use **different preconditions** (setup, role, data). Each phase adds or extends tests in **capability-named** feature files (e.g. `note_creation.feature`, `spaced_repetition.feature`). A phase may add scenarios to an existing file or create a new file named after the capability it introduces — **never** name files or scenarios after the phase itself.

**Still too big:** If one precondition + one E2E story is still large, split by **one small part of the outcome** per phase (one aspect of the postcondition).

### E2E-led decomposition (sub-phases)

**When to use:** A planned **phase** still spans several user-visible beats (e.g. multi-step Gherkin). Prefer **sub-phases** that alternate **E2E red** and **minimal implementation green**, instead of front-loading backend or UI layers.

**Pattern (repeat until the scenario passes and `@wip` is removed):**

1. **Red sub-phase** — Write the **full** E2E scenario in the feature file and tag it `@wip`. Run E2E locally with **`cypress run --spec`** pointing at **that** feature file (not the whole suite); confirm the failure is for the **right reason** (behavior not implemented, not typos).
2. **Green sub-phase** — Implement the **smallest** production change that makes progress toward passing the scenario. **No dead code**; keep the change clean.
3. **Next** — Run the same **`--spec`** again. If the scenario still fails, go back to **2**. When all steps pass, remove the `@wip` tag.

---

## Plan document

Document **important structure and intent** in the plan. **Update** when you learn something that changes how remaining phases should run. Remove text that no longer helps the **current** snapshot.

Include:
- Phases with status (done / in-progress / planned)
- Key design decisions and their rationale
- Discoveries that affect remaining work

**Naming rule for delegated work:** When a plan references feature files, test files, classes, or directories to create or modify, names must reflect the **domain capability** (e.g. `video_playback.feature`, `SegmentExportController`), not the phase or delivery order. Phase numbers belong only in the plan document itself, never in permanent artifact names.
