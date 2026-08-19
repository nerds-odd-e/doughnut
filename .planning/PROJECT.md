# Doughnut

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work. Approved [ADR 0002](../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) sets git-native notebooks (MySQL-backed) as the direction for portable content; one-way catalog ZIP export remains.

## Current State

**Shipped v1.3 (2026-08-08):** Commissioned learning session MVP — assimilate as commissioned, potential sessions on recall progress bar, ADR 0005 Request markdown, record Report with ADR 0003 scheduling (`e2e_test/features/learning_session/commissioned_learning_session.feature`).

**Post-v1.3 polish (2026-08-08):** Richer Learning Session Request (tutor role, notebook QGI, XML sections, title list, report example); learning session hub on recall bar; `<session_item_scores>` report parsing (ADR 0005).

**Post-v1.3 CLS refactor (2026-08-10):** Request is ephemeral (`GET /api/learning-sessions/request` from due trackers, no session persisted). Session + feedback created only on record. Recall list shows potential sessions only (no awaiting/recorded strips). Commission API, amend flow, session status, and pre-session snapshots removed.

**Shipped v1.2 (2026-08-06):** Accidental-match resolve dialog UX; distinct `overlaps` frontmatter; reviewed note stays primary.

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

Portable content direction is ADR 0002 (git-native notebooks). Catalog ZIP download remains (`notebook_export.feature`); path-keyed CLI sync / `.doughnut-sync` is not part of the product.

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading — existing
- ✓ Spaced-repetition scheduling with success/failure/partial paths — existing
- ✓ Wiki-link resolution by title or alias — existing
- ✓ Note aliases in frontmatter, indexed — existing
- ✓ Add-link UI (wiki / property / relationship) — existing
- ✓ Notebook Health lint + tab + gated empty-folder purge — v1.0
- ✓ Accidental-match + overlap spelling recall loop — v1.1
- ✓ Accidental-match resolve dialog (compact CTA, identity-only rows, Build a link, Add as overlapped) — v1.2
- ✓ Distinct `overlaps` frontmatter + plain-only `aliases` (wiki-in-aliases rejected on save) — v1.2
- ✓ Catalog notebook ZIP export (one-way) — retained
- ✓ Commissioned memory tracker assimilation and coexistence (TRK-01–03) — v1.3
- ✓ Potential learning sessions by notebook on recall (POT-01, POT-02) — v1.3
- ✓ Commission Learning Session + Request markdown (COM-01–03) — v1.3
- ✓ Record Report → score schedule + feedback log; amend recorded session (REC-01–05, AMD-01) — v1.3

### Active

(none — define with `/gsd-new-milestone`)

### Out of Scope

- Reintroducing `.doughnut-sync` / path-keyed CLI sync — superseded by ADR 0002
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- ADR 0002 Level 1 (git-native notebooks) — deferred to a later milestone
- Broad unrelated refactors not required by the current milestone
- Stacked matched `NoteShow` bodies on accidental-match result — replaced by dialog
- Content peek in resolve dialog — identity only
- Forced resolve / try-again or SRS reclaim after dialog overlap declare — locked anti-features
- Descriptive Feedback, smart request generator, in-app Tutor, machine transport — v2 / later
- Session identity codes in protocol documents — learner loads report into the open session
- Commissioned trackers for properties in UI — domain allows; UI deferred (TRK-04)
- Commissioned assimilation (first intake via Tutor only) — TRK-05 deferred

## Context

Accepted ADRs under `docs/adrs/`. Planning history for completed milestones is not retained under `.planning/` (product + ADRs are the record).

## Constraints

- **Stack:** Prefer high-level tests; Behavior/Structure slice delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each slice the tree must remain healthier (or no worse) than before
- **ADRs:** Follow Accepted ADRs; humans own approval (`docs/adrs/`)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| ADR 0002 git-native notebooks | Git authoritative for portable content; retire `.doughnut-sync` | ✓ Approved 2026-08-04 |
| Keep catalog ZIP only | One-way portability still useful; E2E `notebook_export.feature` | ✓ Retained |
| Accidental-match resolve via dialog (not stacked notes) | Full-height reviewed note stays primary; resolution is optional and compact | ✓ Shipped v1.2 |
| Overlap from dialog skips try-again / credit reclaim | Declaring overlap is the action; no secondary retry/credit flow | ✓ Shipped v1.2 |
| Distinct `overlaps` vs plain `aliases` | Wiki-link overlap declarations are not aliases; grading reads `overlaps` | ✓ Shipped v1.2 |
| Commissioned learning glossary (ADR 0001 §3) | Lock Tutor / Learning Session / Feedback names before build | ✓ Shipped v1.3 (ADR still Proposed) |
| Markdown copy-paste protocol (ADR 0005) | No existing ed-tech standard fits offline Tutor exchange | ✓ Shipped v1.3 (ADR still Proposed) |
| Tutor score → schedule (ADR 0003) | 1–4 identity (`score = G`) | ✓ Shipped v1.3 (ADR still Proposed) |
| Amend re-grade from pre-session snapshot | Avoid compound re-scheduling on amend | Superseded 2026-08-10 (amend removed) |
| Memory tracker `type` enum | `COMMISSIONED` alongside `UNDERSTANDING` / `SPELLING` | ✓ Shipped v1.3 (quick 006) |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-08-10 after post-v1.3 CLS ephemeral-request refactor*
