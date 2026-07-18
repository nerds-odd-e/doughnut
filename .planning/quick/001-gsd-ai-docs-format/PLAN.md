---
name: gsd-ai-docs-format
status: planned
created: 2026-07-18
---

# Apply GSD skill language to AI-oriented docs

## Goal

Bring remaining AI-facing docs in this repo to the same **pattern language / conventions / format** as `.cursor/skills/post-change-refactor/SKILL.md`: YAML frontmatter + `<objective>` / `<context>` / `<process>` (+ named `<step>`s) / `<success_criteria>` / `<output>` (completion markers) / `<out_of_scope>` where useful — **inline** (no thin skill + workflow split), **concise**, **accurate** (no content invention).

## Already done

- [x] `.cursor/skills/post-change-refactor/SKILL.md`

## Learnings (from 10-min interrupt)

- Inventory: 11 skills; only `post-change-refactor` is GSD-tagged. Largest: `test-optimization` (267), `execute-plan` (199), `phased-planning` (134).
- Working tree was clean at interrupt — nothing to stash.
- **Do not** force GSD XML onto `.cursor/rules/*.mdc` (glob/always-applied constraint docs; different contract).
- **Do not** split skills into thin dispatcher + workflow (same decision as for post-change-refactor).
- Entry docs (`AGENTS.md`, `CLAUDE.md`, `agent-map.md`) are indexes/maps — light alignment, not full `<process>` workflows.
- Keep Cursor `description:` + triggers (discovery); add ALL-CAPS completion markers only where a caller handoff exists.

## Reference pattern (from post-change-refactor)

| Tag | Use |
|-----|-----|
| `<objective>` | Purpose + Output |
| `<context>` | Scope, preconditions, mandatory reads |
| `<process>` / `<step name>` / gates | Ordered work |
| `<success_criteria>` | Done checks |
| `<output>` | Summary shape + `## … COMPLETE` marker |
| `<out_of_scope>` | Abort boundaries |

Keep under ~500 lines; prefer tightening prose over progressive-disclosure files.

---

## Phase 1: Convert short procedure skills
**Type:** Structure  
**Status:** done  
**Unlocks:** Phase 2 (same pattern on larger skills without inventing a second convention)

**Learning:** Short procedure skills fit the GSD tag shell cleanly; overrides/env tables belong in `<context>`, not after `<out_of_scope>`.

**Files:**
- `.cursor/skills/bug-fixing/SKILL.md`
- `.cursor/skills/generate-api-client/SKILL.md`
- `.cursor/skills/database-erd/SKILL.md`
- `.cursor/skills/manual-testing/SKILL.md`
- `.cursor/skills/mutation-testing/SKILL.md`

**Actions:**
- Rewrite each to GSD tags; preserve all commands, ports, accounts, nix prefixes, and policy (e.g. mutation: don't auto-add shallow tests).
- Add completion markers where handoff is useful (`## BUG FIX COMPLETE`, `## API CLIENT GENERATED`, `## ERD EXPORTED`, etc.).
- Trim redundant “when to use” prose already covered by frontmatter `description`.

**Done when:** All five use the reference tag set; no factual drift; each stays concise.

---

## Phase 2: Convert orchestration / timer skills
**Type:** Structure  
**Status:** done  
**Unlocks:** Phase 3 (entry docs can point at consistent skill contracts)

**Learning:** Orchestration skills map cleanly to `<process>` steps (coordinator loop, timer hooks, profile→plan→execute); Jidoka and wrap-up checklists belong in `<preflight_gate>` / named steps, not loose headings.

**Files:**
- `.cursor/skills/execute-plan/SKILL.md`
- `.cursor/skills/phased-planning/SKILL.md`
- `.cursor/skills/test-optimization/SKILL.md` (+ `plan-template.md` only if needed for tag consistency)
- `.cursor/skills/codebase-retrospective/SKILL.md`
- `.cursor/skills/cloud-vm-setup/SKILL.md`

**Actions:**
- Same GSD shell; keep Jidoka, wrap-up, Behavior/Structure grammar, nix/Cloud VM rules intact.
- Prefer shortening; do **not** extract workflows/.
- Completion markers for orchestrator handoffs (e.g. `## PLAN COMPLETE`, `## RETROSPECTIVE COMPLETE`).

**Done when:** All five (plus template if touched) match the pattern; execute-plan still requires post-change-refactor before commit.

---

## Phase 3: Align entry docs (light touch)
**Type:** Structure  
**Status:** planned  

**Files:**
- `AGENTS.md` / `CLAUDE.md` (keep identical content)
- `.cursor/agent-map.md`

**Actions:**
- Keep as short indexes; optional `<objective>`-style opening only if it reduces duplication.
- Do **not** wrap `agent-map` in a fake `<process>` — tables/lists stay.
- Point wrap-up / completion markers at skills consistently; no new policy.

**Done when:** Entry docs remain ≤ current length (±small), accurate, and use the same vocabulary as skills.

---

## Out of scope

- `.cursor/rules/**` (including `planning.mdc`, `gsd-coexistence.mdc`)
- Thin skill + workflow splits
- Renaming skills to `gsd-*`
- Changing product code or planning grammar itself

## Stop-safe

Stopping after Phase 1 still leaves the most-invoked small skills consistent with `post-change-refactor`. Phase 2/3 are independent follow-ons.
