# Doughnut

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work. Approved [ADR 0002](../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) sets git-native notebooks (MySQL-backed) as the direction for portable content; one-way catalog ZIP export remains.

## Current State

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

Portable content direction is ADR 0002 (git-native notebooks). Catalog ZIP download remains (`notebook_export.feature`); path-keyed CLI sync / `.doughnut-sync` is not part of the product.

## Current Milestone: v1.2 Accidental Match Resolve UX

**Goal:** Replace stacked matched-note NoteShows with a compact, optional resolve dialog so the reviewed note keeps the full-height focus.

**Target features:**
- Remove stacked matched notes from the accidental-match result
- CTA under the alert: "Resolve accidental match" → opens a dialog
- Dialog lists each match with clickable title + notebook path/breadcrumb (no note body)
- Per match: "Build a link" (property/relationship) or "Add as overlapped note" (declare overlap)
- Resolving is optional; after navigating away via a title, user can return and reopen the dialog
- Choosing "Add as overlapped note" does not prompt try-again or reclaim SRS credit

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

- Accidental-match resolve dialog UX (v1.2) — see REQUIREMENTS.md

### Out of Scope

- Reintroducing `.doughnut-sync` / path-keyed CLI sync — superseded by ADR 0002
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- ADR 0002 Level 1 (git-native notebooks) — deferred to a later milestone
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
| Accidental-match resolve via dialog (not stacked notes) | Full-height reviewed note stays primary; resolution is optional and compact | Active — v1.2 |
| Overlap from dialog skips try-again / credit reclaim | Declaring overlap is the action; no secondary retry/credit flow | Active — v1.2 |

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

Last updated: 2026-08-05.
