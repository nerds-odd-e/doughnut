# Doughnut

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work. Approved [ADR 0002](../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) sets git-native notebooks (MySQL-backed) as the direction for portable content; one-way catalog ZIP export remains.

## Current State

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

Portable content direction is ADR 0002 (git-native notebooks). Catalog ZIP download remains (`notebook_export.feature`); path-keyed CLI sync / `.doughnut-sync` is not part of the product.

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
- ✓ Catalog notebook ZIP export (one-way) — retained

### Active

(None — define in `/gsd-new-milestone`)

### Out of Scope

- Reintroducing `.doughnut-sync` / path-keyed CLI sync — superseded by ADR 0002
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- Broad unrelated refactors not required by the current milestone

## Context

Milestone archives: `.planning/milestones/`, `MILESTONES.md`. Accepted ADRs under `docs/adrs/`.

## Constraints

- **Stack:** Prefer high-level tests; Behavior/Structure phased delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each phase the tree must remain healthier (or no worse) than before
- **ADRs:** Follow Accepted ADRs; humans own approval (`docs/adrs/`)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| ADR 0002 git-native notebooks | Git authoritative for portable content; retire `.doughnut-sync` | ✓ Approved 2026-08-04 |
| Keep catalog ZIP only | One-way portability still useful; E2E `notebook_export.feature` | ✓ Retained |

## Evolution

This file updates as milestones complete. Last updated: 2026-08-05.
