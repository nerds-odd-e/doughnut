# Notebook Lint & Auto-Fix

## What This Is

A notebook **Health** capability in Doughnut: mechanical lint that reports content-structure problems, plus optional safe auto-fix. Users open Notebook Settings → Health, run checks, review nested findings, and optionally apply configured fixes. v1 focuses on empty folders and dead wiki links (including frontmatter).

## Core Value

From Notebook Settings → Health, a user can run lint and see actionable health findings for their notebook — with optional bulk removal of empty folders when auto-fix is enabled.

## Requirements

### Validated

- ✓ Notebook workspace Index / Settings tabs — existing (`WorkspaceIndexSettingsTabs`, notebook/folder settings surfaces)
- ✓ Dead wiki-link detection in note body and property/frontmatter rendering — existing (`dead-link`, wiki property rendering)
- ✓ Folders as first-class workspace containers — existing

### Active

- [ ] Health tab on notebook settings: run lint, review nested findings, configure lint/auto-fix options
- [ ] Lint rule: empty folders (entire subtree has no notes — recursive)
- [ ] Lint rule: note-empty folders with non-blank **readme** (`readmeContent`) as a separate finding type
- [ ] Lint rule: dead wiki links in note content and frontmatter
- [ ] Optional auto-fix: bulk “remove empty folders” only (no per-folder multi-select; excludes readme-only folders)
- [ ] Fix action enabled only when that bulk option is selected
- [ ] User-level defaults for lint/auto-fix options (apply across notebooks)
- [ ] Research-informed rule/config model inspired by LM Wiki lint (report vs fix separation, severity, extensible rules) — configs beyond v1 explored during research

### Out of Scope

- LLM / semantic lint (contradictions, weak content, synthesis) — defer after mechanical v1
- Empty-note lint rule — not in v1 rule set
- Auto-fix for dead wiki links — report only in v1
- Per-folder selection for delete — bulk option only
- Dedicated `/health` route or findings dialog — stay on Health tab with expandable nested results
- Per-notebook defaults — v1 defaults are per-user

## Context

**Problem:** Notebooks accumulate structural decay (empty folder trees, broken `[[wiki links]]`) that is hard to notice while editing. Users need a health check surface and a carefully gated auto-fix.

**Inspiration — LM Wiki lint (Karpathy / llmwikis.org):**
- Lint is the “immune system”: find structural decay before knowledge becomes unreliable
- **Lint ≠ fix**: surface findings first; auto-fix is separate and gated
- Two tiers: structural/deterministic scripts vs semantic/LLM (v1 = structural only)
- Safe vs dangerous fixes: mechanical fixes may auto-apply when opted in; deletes/moves normally need explicit consent — here empty-folder delete is the only v1 fix and requires selecting the bulk option
- Extensible rule registry + severity levels inform future config research

**Product fit:** Doughnut already renders dead wiki links and has notebook settings tabs. Health extends that into a notebook-scoped audit with nested results and one bulk structural fix.

**UI shape (v1):** Stay on Health tab — expandable nested results (e.g. Empty folders → list; Dead links → by note) plus action bar. Lint/auto-fix options visible for this run and savable as user defaults.

## Constraints

- **Behavior scope**: Mechanical checks only in v1 — no LLM calls required for lint pass
- **Safety**: Destructive fix limited to empty-folder trees; user must opt into auto-fix path and select the bulk remove option before Fix is active
- **Defaults**: Per-user (not per-notebook) for v1
- **Stack**: Existing Doughnut Vue SPA + Spring Boot API + E2E; follow Behavior/Structure phased delivery
- **Research**: Rule config surface beyond “enable auto-fix / remove empty folders” should be explored against LM Wiki patterns before over-building settings UI

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| Entry: Notebook Settings → Health tab | Natural place beside Index/Settings; run + review in one surface | — Pending |
| v1 rules: empty folders + dead wiki links (body + frontmatter) | Highest-signal mechanical health; empty notes deferred | — Pending |
| Empty folder = recursive (subtree has no notes) | Nested empty shells are the real rot; leaf-only misses the pattern | — Pending |
| Note-empty + non-blank readme = separate finding | Avoid conflating “has readme” with fully empty; auto-fix must not delete readme-only | — Pending |
| Domain rename: index → readme (`readmeContent`) | Product renamed index content to readme; lint/docs must use current names | — Pending |
| Auto-fix optional; only bulk remove empty folders | Aligns with LM Wiki “safe fix” gate; no silent deletes | — Pending |
| No per-folder multi-select | Simpler UX; one fix option; Fix active only when selected | — Pending |
| Dead links: report only in v1 | Creating/retargeting links needs judgment; avoid wrong auto-writes | — Pending |
| UI: expandable results on tab (not route/dialog) | Keeps context; avoids premature navigation complexity | — Pending |
| Defaults: per-user | One preference set across notebooks for v1 | — Pending |
| Learn from LM Wiki for future configs | Severity, rule registry, lint≠fix separation guide research | — Pending |

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
*Last updated: 2026-07-22 after initialization*
