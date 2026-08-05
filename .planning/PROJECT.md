# Doughnut

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work. Approved [ADR 0002](../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) sets git-native notebooks (MySQL-backed) as the direction for portable content; one-way catalog ZIP export remains.

## Current State

**2026-08-05:** After ADR 0002, removed the CLI portable-workspace sync stack (Markdown-tree `/export`, `/sync`, `/push --dry-run`, `/lint`, `.doughnut-sync`). Kept catalog ZIP download (`notebook_export.feature`).

**Shipped v1.2 (2026-08-03):** Triaged LIA training participant work for portable-workspace stories 1–6 (later largely removed — see above).

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

## Next Milestone Goals

Define via `/gsd-new-milestone`. Likely candidates:

- ADR 0002 Level 1 (git-native notebooks)
- SEED-001 spelling follow-ons (MCQ / fuzzy / `Notebook:Title`)

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading — existing
- ✓ Spaced-repetition scheduling with success/failure/partial paths — existing
- ✓ Wiki-link resolution by title or alias — existing
- ✓ Note aliases in frontmatter, indexed — existing
- ✓ Add-link UI (wiki / property / relationship) — existing
- ✓ Notebook Health lint + tab + gated empty-folder purge — v1.0
- ✓ Accidental-match + overlap spelling recall loop — v1.1
- ✓ Catalog notebook ZIP export (one-way) — retained after ADR 0002 cleanup
- ✓ Portable-workspace CLI sync/lint stack removed — 2026-08-05 (ADR 0002)

### Active

(None — define in `/gsd-new-milestone`)

### Out of Scope

- Reintroducing `.doughnut-sync` / path-keyed CLI sync — superseded by ADR 0002
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- Broad unrelated refactors not required by the current milestone

## Context

v1.2 strengthened LIA portable-workspace CLI paths; ADR 0002 then retired that sync model. Baseline story notes remain in `.planning/notes/2026-07-24-portable-notebook-workspace.md` for history. Milestone archives: `.planning/milestones/`, `MILESTONES.md`.

## Constraints

- **Stack:** Prefer high-level tests; Behavior/Structure phased delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each phase the tree must remain healthier (or no worse) than before
- **ADRs:** Follow Accepted ADRs; humans own approval (`docs/adrs/`)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| ADR 0002 git-native notebooks | Git authoritative for portable content; retire `.doughnut-sync` | ✓ Approved 2026-08-04; CLI sync stack removed 2026-08-05 |
| Keep catalog ZIP only | One-way portability still useful; E2E `notebook_export.feature` | ✓ Retained |
| Skip Terry/Yeong Sheng in v1.2 triage | Instructors / non-participant work | ✓ Applied in v1.2 |

## Evolution

This file updates as milestones complete. Last updated: 2026-08-05 (ADR 0002 cleanup).
