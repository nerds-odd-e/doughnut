# Doughnut — Portable Workspace Training Cleanup

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing. This milestone cleans up code left by LeSS in Action training participants who worked on the portable Markdown workspace (pull/export, preview, incremental pull, OKF lint, push preview, and safe push).

## Core Value

Keep a healthy mainline for future classes: retain only participant work that matches the portable-workspace stories, has no WIP, and delivers external user value — strengthen near-misses; remove the rest. Never touch Terry or Yeong Sheng changes.

## Current Milestone: v1.2 Clean up LIA training participant code

**Goal:** Audit and triage LIA participant changes against portable-workspace stories 1–6; keep/strengthen valuable complete work; remove WIP, incorrect, or low-value debris so the tree is class-ready.

**Target features:**
- Audit participant commits (non–Terry / non–Yeong Sheng) against stories 1–6
- Keep correct, complete, externally valuable work
- Strengthen keepable work that has only minor gaps
- Remove WIP, incorrect, or non-valuable participant code (and related dead tests/docs/plans)
- Leave the tree class-ready: green targeted tests, no leftover training debris

## Current State

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

**In flight (pre-audit):** Training participants (Eric Yeh, Ben Huang, etta.huang, Joy-kgo, and peers) landed CLI/E2E work toward portable Markdown workspace stories 1–6 during the LIA class. Quality is mixed — this milestone triages it.

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading — existing
- ✓ Spaced-repetition scheduling with success/failure/partial paths — existing
- ✓ Wiki-link resolution by title or alias — existing
- ✓ Note aliases in frontmatter, indexed — existing
- ✓ Add-link UI (wiki / property / relationship) — existing
- ✓ Notebook Health lint + tab + gated empty-folder purge — v1.0
- ✓ Accidental-match + overlap spelling recall loop — v1.1
- ✓ Baseline portable-workspace direction documented in `.planning/notes/2026-07-24-portable-notebook-workspace.md`

### Active

- [ ] Audit LIA participant changes (exclude Terry Yin, Tan Yeong Sheng) against stories 1–6
- [ ] Keep work that is requirement-correct, has no WIP, and has external user value
- [ ] Strengthen keepable work that has only minor gaps
- [ ] Remove WIP, incorrect, or non-valuable participant code and related debris
- [ ] Confirm class-ready mainline (targeted tests green; no training WIP left)

### Out of Scope

- Stories 7–10 (create from local file, rename, move, reconcile deletions) — not attempted in this class; leave for later
- Terry Yin and Tan Yeong Sheng changes — skip entirely
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- New portable-workspace features beyond keep/strengthen of existing participant work
- Broad unrelated refactors not required by triage decisions

## Context

Last week Doughnut was the exercise project for LeSS in Action. Two student teams worked the first items of the portable Markdown workspace backlog (stories 1–6 in `.planning/notes/2026-07-24-portable-notebook-workspace.md`). Keep only work that is correct per those acceptance examples, unfinished-free, and valuable to an external user. Near-misses with real user value should be finished enough to keep. Everything else goes so future classes start from a healthy tree.

## Constraints

- **Authors:** Do not revert, rewrite, or "clean" commits/changes attributable to Terry Yin or Tan Yeong Sheng
- **Acceptance bar:** Story acceptance examples in the portable-workspace note are the keep/remove oracle
- **WIP:** Incomplete or `@wip` / half-wired work is remove-by-default unless strengthening is cheaper and clearly valuable
- **Stack:** Prefer CLI + E2E capability tests; Behavior/Structure phased delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each phase the tree must remain healthier (or no worse) than before

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Include story 5 (push dry-run / conflict preview) in audit | Students landed it; part of the push safety loop with story 6 | — Pending |
| Skip Terry and Yeong Sheng | Instructors / non-participant work; out of triage | — Pending |
| Keep only correct + no WIP + external value | Class-ready mainline over preserving every experiment | — Pending |
| Strengthen minor gaps on valuable work | Prefer keeping usable capability over delete-and-rebuild | — Pending |
| v1.1 spelling follow-ons → SEED-001 | Detour for training cleanup; do not lose the idea | ✓ Parked |

<details>
<summary>Pre-v1.2 spelling-era project framing (archived)</summary>

Earlier PROJECT.md framed the active product around spelling accidental-match & overlap (v1.1). See `.planning/milestones/v1.1-ROADMAP.md` and `.planning/MILESTONES.md` for the shipped record. Spelling follow-ons live in SEED-001.

</details>

## Evolution

This document evolves at phase transitions and milestone boundaries.

**After each phase transition** (via `/gsd-transition`):
1. Requirements invalidated? → Move to Out of Scope with reason
2. Requirements validated? → Move to Validated with phase reference
3. New requirements emerged? → Add to Active
4. Decisions to log? → Add to Key Decisions
5. "What This Is" still accurate? → Update if drifted

**After each milestone** (via `/gsd-complete-milestone`):
1. Full review of all sections
2. Core Value check — still the right priority?
3. Audit Out of Scope — reasons still valid?
4. Update Context with current state

---
*Last updated: 2026-08-03 after starting milestone v1.2*
