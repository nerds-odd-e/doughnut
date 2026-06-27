---
name: execute-plan
description: >-
  Autonomously execute a phased plan from ongoing/*.md. Delegates each phase to
  a fresh sub-agent so context does not accumulate. Each sub-agent commits and
  pushes before the coordinator starts the next. Stops only when developer
  judgment is needed (Jidoka).
  Triggers on: execute plan, run plan, execute phases, start plan, do ongoing,
  execute ongoing, run ongoing.
---

# Execute Plan — Autonomous Phase Runner

Start with `.cursor/agent-map.md` for navigation and focused test commands.

## When to use

Developer points at a plan file and asks to execute it. Common phrasings:

- "execute plan ongoing/something.md"
- "do @ongoing/something.md"
- "run @ongoing/something-sub-phases.md"
- "execute phases", "start plan"

Plans and sub-phase plans live in `ongoing/*.md`. Sub-phase documents
(`ongoing/<plan-name>-<phase-number>-sub-phases.md`) follow the **same** loop
as top-level plans.

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
  credentials in what the plan *asks for*.
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

**You are a thin coordinator. You do NOT implement phases yourself.**
Each phase is delegated to a **fresh sub-agent** so that context does not
accumulate across phases.

```
1. Read the plan file (ongoing/*.md)
2. Find the next phase/sub-phase whose status is NOT "done"
3. Check stop conditions on the phase description
   → If stop condition → report & STOP
4. DELEGATE the phase to a sub-agent (see "Delegation" below)
5. When sub-agent finishes:
   a. Verify the plan file shows the phase as "done"
   b. Verify a new commit was pushed (`git log -1`, `git status`)
   c. If the sub-agent reported a Jidoka stop → relay to developer & STOP
   d. If the sub-agent reported REVERT & SPLIT → re-read plan, continue loop
6. Go to step 1 (next phase)
7. All phases done → report "plan complete" & STOP
```

### Delegation — one sub-agent per phase

Use the **Task tool** (`subagent_type: "generalPurpose"`) for each phase.
The sub-agent prompt **must** include:

1. **Plan file path** and **which phase/sub-phase** to implement (paste the
   phase text so the sub-agent does not need to guess).
2. **Jidoka stop conditions** (copy the list above).
3. **Implementation rules**: follow `planning.mdc` and the **phased-planning**
   skill (TDD, test-first, phase discipline). Use `.cursor/rules/` for the
   areas touched — e.g. `backend-code.mdc`, `backend-testing.mdc`,
   `e2e-authoring.mdc`, `frontend-component.mdc`, `frontend-testing.mdc`,
   `frontend-api.mdc`, `db-migration.mdc`, `mcp-server.mdc`, `cli.mdc`.
   **Naming rule**: all permanent artifacts (file names, test names, feature
   files, class names) are named by **capability/domain**, never by phase
   number. Phases drive *when* work happens; code/tests present a static
   consumer's view of the system.
4. **Wrap-up checklist** (see below).
5. **Revert & split** instructions (see below).
6. **Nix prefix**: run repo tooling through
   `CURSOR_DEV=true nix develop -c <command>` unless on Cloud VM (use
   `cloud-vm-setup` skill). **Git commands do not need the Nix prefix.**
7. **What to return**: a short summary — phase done, or Jidoka stop with
   reason, or reverted and split (with updated plan content).

**Do NOT pass the entire plan history or prior phase details** — only the
current phase and enough context for the sub-agent to work.

### Sub-agent: wrap-up checklist (after tests pass)

1. **Delegate refactoring to a fresh sub-agent** — Before committing, the
   phase sub-agent spawns another sub-agent (Task tool,
   `subagent_type: "generalPurpose"`) that runs the
   **post-change-refactor** skill
   (`.cursor/skills/post-change-refactor/SKILL.md`) against the current
   uncommitted change. Pass it:
   - The phase being closed (paste the phase text from the plan).
   - The path to the plan file, so it can read the immediate next phase
     when deciding what is justified.
   - The nix prefix rule (`CURSOR_DEV=true nix develop -c <command>`; git
     without prefix).
   - Instruction to follow the post-change-refactor skill end-to-end and
     **not** to commit.
   - Instruction to return a short summary of what it cleaned up and which
     related tests it confirmed passing.

   When the refactor sub-agent returns, the phase sub-agent continues with
   the remaining wrap-up steps below.
2. **Lint & format** — run `CURSOR_DEV=true nix develop -c pnpm lint:all` and
   `CURSOR_DEV=true nix develop -c pnpm format:all`. Fix any issues.
3. **Regenerate API client** — if backend controller or DTO signatures changed,
   run the **generate-api-client** skill before committing.
4. **Reflect & re-plan the plan file** — based on what this phase taught you:
   - **Summarize learnings** — capture anything that changes future work
     (surprises, constraints, assumptions proven wrong). Keep it brief; drop it
     if there is nothing worth carrying forward.
   - **Prune done phases** — mark the phase "done" and remove now-obsolete
     implementation detail from completed phases. The plan reflects the current
     state and what remains, not a development diary.
   - **Adjust future phases** — revise, reorder, add, or remove downstream
     phases when the learnings warrant it.
5. **Post-phase Jidoka check** — if the learnings surface a value/design fork,
   a wrong assumption, or anything needing developer judgment (see the
   **after** focus in the Jidoka section), **do not commit blindly**: commit and
   push the work so far, then return a Jidoka stop to the coordinator instead of
   silently continuing.
6. **Commit** — stage all changes and commit with a message that references
   the phase. Use the conventional format from the repo's recent history.
7. **Push** — `git push`.

### Sub-agent: revert & split

A phase is **too big** when:

- Changes span many unrelated files with no clear single behavior emerging.
- Tests are not converging after reasonable effort.

When this happens:

1. `git checkout .` — revert all uncommitted changes.
2. `git clean -fd` — remove untracked files from the attempt.
3. Invoke the **phased-planning** skill to split the phase into sub-phases.
4. Update the plan file: replace the original phase with the sub-phases.
5. Commit and push the updated plan file.
6. Return a "reverted and split" result to the coordinator.

Sub-phases follow the same rules — if a sub-phase is still too big, split
again.

---

## Reading the plan file

Plans live in `ongoing/*.md`. Each phase typically looks like:

```markdown
### Phase N: Short description
Status: planned / in-progress / done

Details about what to implement...
```

Sub-phase documents (`ongoing/<plan-name>-<phase-number>-sub-phases.md`) use
the same format with sub-phase headings. The executor treats both identically.

The executor recognizes phases by headings and status markers. When closing a
phase, update its status and prune/adjust as described in the wrap-up checklist;
otherwise leave unrelated parts of the plan intact.

---

## Reporting

When the loop ends (either all phases done or a stop condition), provide:

1. **Summary** — which phases were completed this run.
2. **Current state** — what the plan looks like now.
3. **Next action** — what the developer needs to do or decide (if stopped).
