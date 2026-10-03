---
id: SEED-068
status: dormant
planted: 2026-10-03
planted_during: owner review of project retrospective findings, queueing the highest-impact Donut-specific finding group first
trigger_when: refining the queued story that corrects the removal-rule finding in DonutRetrospectiveFindings.md
scope: small
---

# SEED-068: Owner rules reach every agent that plans or delivers Donut work

## Why This Matters

The owner's standing rule for removals (delete outright, then leave no
negation, absence check, or historical note anywhere in the product) is kept
only in one coordinator's memory. Planners, implementers and refactor agents
never see it, so removal plans keep prescribing traces and the owner pays for
a correction afterwards. See
[Donut retrospective findings](../../DonutRetrospectiveFindings.md#open-findings).

## Story Decomposition

<a id="removal-rule-in-guidance"></a>
### Removals leave no trace without the owner restating the rule

**Identity:** SEED-068#removal-rule-in-guidance
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/004-removal-rule-in-guidance/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"629919a4d65a011e4fd530bf5928358527b7f1f26eb79c7518fa630d8015fdac","plan":"d4777250143cd46c63daabb2f0cb30300c4fc062cfc5039dd75f93c728235950"}}
```

- **Goal:** Let the owner hand any removal to any agent (refiner, planner,
  implementer, refactor agent, coordinator, retrospective reviewer) and get a
  product with no trace of the removed thing. Today the rule lives only in one
  coordinator's memory. The owner finds traces after delivery and pays for a
  correction story each time.
- **Field evidence (what really went wrong):**
  - The trace started in story refinement, not only in the plan. SEED-066's
    refined scope stated the removal as a negative promise ("They never change
    the title … during this recording or later ones"). The plan followed it with
    a "no longer changes titles" docs note and a test
    `never changes the title across many chunks and a later recording`
    (it checks that `updateNoteTitle` is never called). Implementer, refactor
    agent and coordinator all accepted both.
  - The whole-product sweep was incomplete: five more title-service mentions
    stayed in `docs/voice-input.md` (fixed by `ac59c040e5`).
  - The retrospective caught the docs note but missed the test, which is still
    on main in `frontend/tests/notes/NoteAudioTools.processing.spec.ts`.
  - DD-142: a plan prescribed a "there is no `[aria-label=…]` element"
    assertion after deleting that hint; the refactor agent removed it during
    delivery.
- **Scope (required):**
  - State the removal rule in Donut's always-loaded guidance, `AGENTS.md` and
    `CLAUDE.md` (kept in sync), so every agent sees it without a memory. It
    covers all three parts:
    - **Delete outright, transitively:** the removed thing, its tests, and
      everything only it used (services, endpoints, DTOs, columns, helpers,
      fixtures, prompts, mocks, ratchet entries), repeated until nothing is
      orphaned. Shared code with other callers stays.
    - **Sweep the whole product:** backend, frontend, CLI, MCP, docs, code
      comments, agent guidance and generated artifacts read as if the thing
      never existed.
    - **No trace:** no absence assertion, "never happens" test, runtime guard
      against its return, or "no longer" / "used to" note. Git history is the
      record.
  - The rule tells each role what it means for them: refinement and planning
    state a removal as deletion plus sweep, never as a negative promise; an
    implementer, refactor agent or coordinator meeting a step that prescribes a
    trace skips it and reports that; a retrospective treats a trace as a
    finding.
  - Delete the leftover test
    `never changes the title across many chunks and a later recording`, so main
    follows the rule (owner decision, 2026-10-03).
- **Rejection constraints:** none beyond the rule itself.
- **Deferred promises:** no edits to shared Open Dough skills (`dough-*`),
  which `dough-update` overwrites; shared guidance on absence assertions stays
  as it is. No automated check for traces. No search for other old traces on
  main beyond the one found here.
- **Boundary assumption:** a story whose promise is a refusal the product
  actively enforces (for example, a non-owner cannot edit a notebook) is not a
  removal; its tests stay.
- **Key examples:**
  - A fresh agent refines a story "stop dictation from changing titles" →
    scope says delete the title-suggestion chain and everything only it used,
    then sweep docs; it does not say "dictation never changes the title".
  - A fresh planner plans a removal → slices delete code, tests and orphaned
    dependants, and include a whole-product sweep; no slice asks for a
    "no longer" docs note or an absence assertion (DD-202, DD-142).
  - An implementer or refactor agent gets a plan step asking for a
    "there is no `[aria-label=…]` element" check → skips it and reports the
    skip to the coordinator.
  - After a removal, the docs still mention the removed service in passing
    (as in the five leftover title-service mentions) → the sweep removes them
    before delivery.
  - A story adds "a non-owner cannot edit this notebook" → its refusal test
    stays; the rule does not apply.
  - After delivery, `NoteAudioTools.processing.spec.ts` has no test about
    titles not changing.
- **Effort hypothesis:** S — one principle in `AGENTS.md` and `CLAUDE.md` plus
  one test deletion.
- **Safe stopping point:** the rule is in both always-loaded files with its
  three parts and role guidance, and the leftover test is gone.
