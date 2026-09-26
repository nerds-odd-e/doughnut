---
id: SEED-044
status: dormant
planted: 2026-09-26
planted_during: owner-reported discrepancy after updating Open Dough guidance to 0.3.41
trigger_when: Donut's planning layout differs from the Open Dough guidance it installs
scope: small
---

# SEED-044: Donut's planning layout follows the installed Open Dough guidance

## Why This Matters

Agents following the installed Open Dough guidance write executable plans under
`.planning/slice-plans/`, while Donut's own instructions, planning records, and
ignore rules point elsewhere. Agents then read conflicting guidance about where
plans live.

## Story Decomposition

<a id="story-1"></a>

### Executable plans live under `.planning/slice-plans/`

**Identity:** SEED-044#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planless"}
```

**Goal:** Agents and the owner find every executable plan, and every Donut
instruction or planning record that points to one, under
`.planning/slice-plans/NNN-slug/`, matching the installed Open Dough
guidance.

**Scope:** move the plans folder on `main` and update Donut-owned references
(`AGENTS.md`, `CLAUDE.md`, `.agents/agent-map.md`, `.gitignore`, backlog,
seeds, findings logs, and docs). Excludes Open Dough guidance files and
other branches that still carry older paths.

**Expectations:** no Donut-owned file on `main` refers to the previous plans
folder; plan links in the backlog and seeds resolve.
