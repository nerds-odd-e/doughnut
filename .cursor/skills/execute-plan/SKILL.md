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

# Execute Plan — Autonomous Phase Runner

Local execution overlay for GSD-aligned plans. Complements `/gsd-execute-phase`:
same plans, but **this repo requires** Jidoka, refactor, plan update, and
**commit+push** after every phase. See `.cursor/rules/gsd-coexistence.mdc`.

Start with `.cursor/agent-map.md` for navigation and focused test commands.

## When to use

Developer points at a plan and asks to execute it. Common phrasings:

- "execute plan .planning/phases/03-foo/03-01-PLAN.md"
- "do @.planning/quick/001-bar/"
- "execute phases", "start plan", `/gsd-execute-phase` (then enforce local wrap-up)

**Plan locations (preferred → legacy):**

1. `.planning/phases/NN-slug/*-PLAN.md`
2. `.planning/quick/NNN-slug/PLAN.md` (or `*-PLAN.md`)
3. Legacy flat `.planning/*.md` or `ongoing/*.md`

Every executable unit (phase, plan wave, or sub-phase) must obey
**Behavior | Structure**, stop-safe, one observable behavior
(`.cursor/rules/planning.mdc`). If it does not, stop and re-plan with
**phased-planning** before implementing.

## Jidoka principle (autonomation)

Run with full autonomy **but stop the line** when something requires a
developer's brain — a learning moment, not a mechanical task.

**Stop conditions (report and wait):**

- **Value decision** — multiple valid directions with different user-facing
  trade-offs; the plan says "TBD", "decide", "option A / B", or you discover
  such a fork during implementation.
- **Design decision** — a structural choice that affects future phases or
  overall architecture.
- **Authentication / credentials** — secrets, API keys, login flows, or
  permissions the agent cannot supply.
- **Unexpected failure you cannot diagnose** — a test fails for reasons
  unrelated to the current change, CI breaks on something external, etc.
- **Ambiguity** — the phase description is unclear and guessing wrong would
  waste a commit.

When stopping: explain **what** you learned, **why** you stopped, and **what
decision** the developer needs to make. Then wait.

**Check Jidoka both before and after each phase, with separate focuses:**

- **Before** (coordinator, on the phase *description*) — is this phase safe to
  start autonomously? Look for value/design forks, ambiguity, or missing
  credentials in what the plan *asks for*. Also confirm Behavior/Structure
  grammar.
- **After** (sub-agent, on what was *learned* implementing the phase) — did
  doing the work reveal something the plan did not anticipate? A discovered
  value/design fork, an assumption proven wrong, or a learning that changes
  downstream phases. Stop even if the phase itself succeeded.

**Do NOT stop for:**

- Routine implementation choices (naming, file placement, test structure) where
  existing rules and conventions give a clear answer.
- Minor refactoring needed to make the phase fit.
- Test failures caused by your own change (fix them).

---

## The loop (coordinator)

**You are a thin coordinator. You do NOT implement phases yourself**
(except when running a single interactive phase). Each phase is delegated to a
**fresh sub-agent** so context does not accumulate.

**Parallelism:** you may run multiple independent plans/phases in parallel
(GSD waves or multiple Task agents) when their `files_modified` / touch sets
do not overlap and they do not contend on the same PLAN/STATE writes.
Otherwise run sequentially.

```
1. Read the plan (phase dir PLAN.md / GSD *-PLAN.md / legacy flat file)
2. Find the next unit whose status is NOT "done"
3. Pre-phase Jidoka + Behavior/Structure check
   → If stop condition → report & STOP
4. DELEGATE the phase to a sub-agent (see "Delegation" below)
   (or fan out a safe parallel wave)
5. When sub-agent finishes:
   a. Verify the plan shows the phase as "done" (and SUMMARY updated if GSD)
   b. Verify a new commit was pushed (`git log -1`, `git status`)
   c. If the sub-agent reported a Jidoka stop → relay to developer & STOP
   d. If the sub-agent reported REVERT & SPLIT → re-read plan, continue loop
6. Go to step 1 (next phase)
7. All phases done → clean up spent plan history (planning.mdc) →
   report "plan complete" & STOP
```

### Delegation — one sub-agent per phase

Use the **Task tool** (`subagent_type: "generalPurpose"`; or GSD
`gsd-executor` when inside `/gsd-execute-phase` — still require wrap-up below).
The sub-agent prompt **must** include:

1. **Plan file path** and **which phase/sub-phase** to implement (paste the
   phase text so the sub-agent does not need to guess).
2. **Jidoka stop conditions** (copy the list above).
3. **Implementation rules**: follow `planning.mdc` (Behavior/Structure, TDD,
   phase discipline), `gsd-coexistence.mdc`, and other rules as applicable.
   **Naming rule**: permanent artifacts are named by **capability/domain**,
   never by phase number.
4. **Wrap-up checklist** (see below).
5. **Revert & split** instructions (see below).
6. **Nix prefix**: run repo tooling through
   `CURSOR_DEV=true nix develop -c <command>` unless on Cloud VM (use
   `cloud-vm-setup` skill). **Git commands do not need the Nix prefix.**
7. **What to return**: a short summary — phase done, or Jidoka stop with
   reason, or reverted and split (with updated plan content).

**Do NOT pass the entire plan history or prior phase details** — only the
current phase and enough context for the sub-agent to work. Resume context
lives in STATE / PLAN files on disk.

### Sub-agent: wrap-up checklist (after tests pass)

1. **Delegate refactoring to a fresh sub-agent** — Before committing, spawn
   another sub-agent that runs **post-change-refactor**
   (`.cursor/skills/post-change-refactor/SKILL.md`) against the current
   uncommitted change. Pass phase text, plan path, nix prefix; **do not
   commit** from the refactor agent.
2. **Lint & format** — `CURSOR_DEV=true nix develop -c pnpm lint:all` and
   `CURSOR_DEV=true nix develop -c pnpm format:all`. Fix any issues.
3. **Regenerate API client** — if backend controller or DTO signatures changed,
   run the **generate-api-client** skill before committing.
4. **Reflect & re-plan** — update the PLAN (and STATE/SUMMARY if present):
   - Brief learnings that change remaining work (resume-useful).
   - Mark phase **done**; prune obsolete implementation detail from that phase.
   - Adjust future phases when warranted.
5. **Post-phase Jidoka check** — if learnings need developer judgment:
   commit and push work so far, then return a Jidoka stop (do not silently
   continue).
6. **Commit** — stage all changes; message may use GSD-style
   `{type}({phase}-{plan}): …` or the repo's recent convention.
7. **Push** — `git push`.

### Sub-agent: revert & split

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

---

## Reading the plan file

Recognize units by headings/status or GSD plan tasks. Typical local section:

```markdown
### Phase N: Short description
Type: Behavior | Structure
Status: planned / in-progress / done

Pre-condition / trigger / post-condition (Behavior)
— or —
Structure change + immediate next Behavior it unlocks
```

When the **entire** plan is complete: actively clean spent planning history
per `planning.mdc` (keep product/code; drop disposable diary under `.planning/`).

---

## Reporting

When the loop ends (all phases done or a stop condition):

1. **Summary** — which phases were completed this run.
2. **Current state** — PLAN/STATE pointers for resume (if stopped).
3. **Next action** — developer decision needed, or confirm cleanup done.
