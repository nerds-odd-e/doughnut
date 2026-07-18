---
name: execute-plan
description: >-
  Autonomously execute a phased plan under .planning/phases/ or
  .planning/quick/ (GSD PLAN.md files, or legacy flat/ongoing plans).
  Applies local wrap-up on every phase: Jidoka, post-change-refactor,
  plan update, commit, and push. Parallel waves OK when safe.
  Triggers on: execute plan, run plan, execute phases, start plan,
  do .planning, execute .planning, run .planning.
---

<objective>
Autonomously execute a GSD-aligned phased plan with **local wrap-up on every
phase**: Jidoka gates, post-change-refactor, plan update, commit, and push.

Purpose: Local execution overlay for GSD plans — complements
`/gsd-execute-phase` but **requires** this repo's wrap-up per
`.cursor/rules/gsd-coexistence.mdc`.

Output: Phases completed with commits pushed, or a Jidoka stop report ending
with `## PLAN EXECUTION COMPLETE` (all phases done) or a stop summary when
waiting on the developer.
</objective>

<context>
**Mandatory first read:** `.cursor/agent-map.md` (navigation + focused test commands).

**Plan locations (preferred → legacy):**

1. `.planning/phases/NN-slug/*-PLAN.md`
2. `.planning/quick/NNN-slug/PLAN.md` (or `*-PLAN.md`)
3. Legacy flat `.planning/*.md` or `ongoing/*.md`

Every executable unit (phase, plan wave, or sub-phase) must obey
**Behavior | Structure**, stop-safe, one observable behavior
(`.cursor/rules/planning.mdc`). If it does not, stop and re-plan with
**phased-planning** before implementing.

**Git does not use the Nix prefix.** All other repo tooling does:
`CURSOR_DEV=true nix develop -c …` unless on Cloud VM (use **cloud-vm-setup**
skill — no nix prefix there).

**Coordinator role:** You are a thin coordinator. You do **not** implement phases
yourself (except a single interactive phase). Delegate each phase to a **fresh
sub-agent** so context does not accumulate.

**Parallelism:** Run multiple independent plans/phases in parallel (GSD waves or
Task agents) when `files_modified` / touch sets do not overlap and they do not
contend on the same PLAN/STATE writes. Otherwise run sequentially.
</context>

<process>

<preflight_gate name="jidoka_stop_conditions">
Run with full autonomy **but stop the line** when something requires a
developer's brain.

**Stop and wait when:**

- **Value decision** — multiple valid directions with different user-facing
  trade-offs; the plan says "TBD", "decide", "option A / B", or you discover
  such a fork during implementation.
- **Design decision** — a structural choice that affects future phases or
  overall architecture.
- **Authentication / credentials** — secrets, API keys, login flows, or
  permissions the agent cannot supply.
- **Unexpected failure you cannot diagnose** — test fails for reasons unrelated
  to the current change, CI breaks on something external, etc.
- **Ambiguity** — the phase description is unclear and guessing wrong would
  waste a commit.

When stopping: explain **what** you learned, **why** you stopped, and **what
decision** the developer needs. Then wait.

**Do NOT stop for:**

- Routine implementation choices (naming, file placement, test structure) where
  existing rules and conventions give a clear answer.
- Minor refactoring needed to make the phase fit.
- Test failures caused by your own change (fix them).

**Check Jidoka both before and after each phase:**

- **Before** (coordinator, on the phase *description*) — safe to start
  autonomously? Value/design forks, ambiguity, missing credentials, Behavior/Structure
  grammar.
- **After** (sub-agent, on what was *learned*) — did work reveal something the
  plan did not anticipate? Stop even if the phase succeeded.
</preflight_gate>

<step name="coordinator_loop">
```
1. Read the plan (phase dir PLAN.md / GSD *-PLAN.md / legacy flat file)
2. Find the next unit whose status is NOT "done"
3. Pre-phase Jidoka + Behavior/Structure check
   → If stop condition → report & STOP
4. DELEGATE the phase to a sub-agent (or fan out a safe parallel wave)
5. When sub-agent finishes:
   a. Verify the plan shows the phase as "done" (and SUMMARY updated if GSD)
   b. Verify a new commit was pushed (`git log -1`, `git status`)
   c. If sub-agent reported Jidoka stop → relay to developer & STOP
   d. If sub-agent reported REVERT & SPLIT → re-read plan, continue loop
6. Go to step 1 (next phase)
7. All phases done → clean up spent plan history (planning.mdc) → report & STOP
```

Recognize units by headings/status or GSD plan tasks. Typical local section:

```markdown
### Phase N: Short description
Type: Behavior | Structure
Status: planned / in-progress / done

Pre-condition / trigger / post-condition (Behavior)
— or —
Structure change + immediate next Behavior it unlocks
```

When the **entire** plan is complete: actively clean spent planning history per
`planning.mdc` (keep product/code; drop disposable diary under `.planning/`).
</step>

<step name="delegation">
Use the **Task tool** (`subagent_type: "generalPurpose"`; or GSD `gsd-executor`
when inside `/gsd-execute-phase` — still require wrap-up below).

The sub-agent prompt **must** include:

1. **Plan file path** and **which phase/sub-phase** to implement (paste the
   phase text).
2. **Jidoka stop conditions** (copy the list above).
3. **Implementation rules**: `planning.mdc` (Behavior/Structure, TDD, phase
   discipline), `gsd-coexistence.mdc`, and other applicable rules. **Naming:**
   permanent artifacts by **capability/domain**, never phase number.
4. **Wrap-up checklist** (see `wrap_up` step).
5. **Revert & split** instructions (see `revert_and_split` step).
6. **Nix prefix**: `CURSOR_DEV=true nix develop -c <command>` unless Cloud VM
   (`cloud-vm-setup`). **Git commands do not need the Nix prefix.**
7. **Return**: short summary — phase done, Jidoka stop, or reverted and split.

**Do NOT pass entire plan history** — only the current phase. Resume context
lives in STATE / PLAN files on disk.
</step>

<step name="wrap_up">
Sub-agent wrap-up checklist (after tests pass):

1. **Delegate refactoring** — Spawn a fresh sub-agent running **post-change-refactor**
   (`.cursor/skills/post-change-refactor/SKILL.md`) on the uncommitted change.
   Pass phase text, plan path, nix prefix; **do not commit** from the refactor agent.
2. **Lint & format** — `CURSOR_DEV=true nix develop -c pnpm lint:all` and
   `CURSOR_DEV=true nix develop -c pnpm format:all`. Fix any issues.
3. **Regenerate API client** — if backend controller or DTO signatures changed,
   run **generate-api-client** before committing.
4. **Reflect & re-plan** — update PLAN (and STATE/SUMMARY if present):
   - Brief learnings that change remaining work.
   - Mark phase **done**; prune obsolete detail from that phase.
   - Adjust future phases when warranted.
5. **Post-phase Jidoka** — if learnings need developer judgment: commit and push
   work so far, then return a Jidoka stop (do not silently continue).
6. **Commit** — stage all changes; message may use GSD-style
   `{type}({phase}-{plan}): …` or the repo's recent convention.
7. **Push** — `git push`.
</step>

<step name="revert_and_split">
A phase is **too big** when:

- Changes span many unrelated files with no clear single behavior emerging.
- Tests are not converging after reasonable effort.

When this happens:

1. `git checkout .` — revert all uncommitted changes.
2. `git clean -fd` — remove untracked files from the attempt.
3. Invoke **phased-planning** to split into Behavior/Structure sub-phases.
4. Update the PLAN in the phase/quick dir.
5. Commit and push the updated plan.
6. Return "reverted and split" to the coordinator.
</step>

</process>

<success_criteria>
- Each phase delegated to a fresh sub-agent (coordinator does not accumulate context)
- Pre- and post-phase Jidoka checks applied
- Every completed phase: post-change-refactor → lint/format → plan update → commit → push
- Parallel waves only when touch sets and PLAN/STATE writes do not conflict
- Spent planning history cleaned when entire plan is done
- Final output includes `## PLAN EXECUTION COMPLETE` when all phases finish
</success_criteria>

<output>
When the loop ends (all phases done or a stop condition):

1. **Summary** — which phases were completed this run.
2. **Current state** — PLAN/STATE pointers for resume (if stopped).
3. **Next action** — developer decision needed, or confirm cleanup done.

```
## PLAN EXECUTION COMPLETE
```

(Use when all phases are done. For Jidoka stops, report the stop reason and wait
— do not emit the completion marker until the developer resolves and work resumes.)
</output>

<out_of_scope>
- Do not implement phases in the coordinator agent (except single interactive phase).
- Do not skip post-change-refactor, commit, or push per phase.
- Do not pass full plan history to sub-agents.
- Do not continue past a Jidoka stop without developer input.
</out_of_scope>
