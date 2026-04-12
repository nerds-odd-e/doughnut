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
2. **Summarize** what you have learned (discoveries, blockers, partial progress).
3. **Stash** your changes: `git stash -m "WIP: <brief description>"`.
4. **Decompose** the remaining work into phases (see below).
5. **Write** the plan to `ongoing/<short-name>.md`.
6. **Report** to the developer and wait for their decision.

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

**E2E-shaped phases:** Prefer phases that each map to an **end-to-end** scenario; different phases may use **different preconditions** (setup, role, data).

**Still too big:** If one precondition + one E2E story is still large, split by **one small part of the outcome** per phase (one aspect of the postcondition).

### E2E-led decomposition (sub-phases)

**When to use:** A planned **phase** still spans several user-visible beats (e.g. multi-step Gherkin). Prefer **sub-phases** that alternate **E2E red** and **minimal implementation green**, instead of front-loading backend or UI layers.

**Pattern (repeat until the full scenario is uncommented and green):**

1. **Red sub-phase** — Add or extend the **real** E2E scenario with the **full** story text in the feature file. **Enable only the next step** toward the final outcome: **comment out** all **later** steps (`#` in Gherkin). **Earlier** steps stay **enabled** and should already **pass**. Run E2E; confirm **exactly one** failure for the **right reason** (behavior not implemented, not typos). **Do not** leave **multiple** newly added steps failing at once.
2. **Green sub-phase** — Implement the **smallest** production change that makes the **currently enabled** prefix **pass**. **No dead code**; keep the change clean.
3. **Next** — Uncomment the **next** Gherkin line and go back to **1**.

---

## Plan document

Document **important structure and intent** in the plan. **Update** when you learn something that changes how remaining phases should run. Remove text that no longer helps the **current** snapshot.

Include:
- Phases with status (done / in-progress / planned)
- Key design decisions and their rationale
- Discoveries that affect remaining work
