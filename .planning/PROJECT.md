# Donut

## What This Is

Donut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work.

## Current State

**Shipped v1.3 (2026-08-08):** Commissioned learning session — assimilate as commissioned, potential sessions on recall progress bar, Request markdown ([commissioned learning session protocol](../docs/commissioned-learning-session-protocol.md)), record Report with ADR 0003 scheduling (`e2e_test/features/learning_session/commissioned_learning_session.feature`). Request is ephemeral (`GET /api/learning-sessions/request` from due trackers). Recall list shows potential sessions. Tutor Feedback is a Grade and descriptive text; the next Request carries the last two dated Feedbacks per Session Item.

**Shipped v1.2 (2026-08-06):** Accidental-match resolve dialog UX; distinct `overlaps` frontmatter; reviewed note stays primary.

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

Portable content format follows Accepted ADR 0004. Git-native two-way
synchronization is proposed in
[ADR 0002](../docs/adrs/0002-git-native-portable-notebook-synchronization.md);
catalog ZIP download remains (`notebook_export.feature`).

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
- ✓ Record Report → Grade schedule + feedback log (REC-01–05) — v1.3
- ✓ Descriptive Feedback on tutor RecallLog, recall history, and last two in the Request

### Active

(none — define with `/gsd-new-milestone`)

### Out of Scope

- Portable notebook two-way synchronization — deferred pending advice and a
  human decision on Proposed ADR 0002
- Spelling follow-ons (MCQ / fuzzy / `Notebook:Title`) — parked as SEED-001
- Broad unrelated refactors not required by the current milestone
- Stacked matched `NoteShow` bodies on accidental-match result — replaced by dialog
- Content peek in resolve dialog — identity only
- Forced resolve / try-again or SRS reclaim after dialog overlap declare — locked anti-features
- Smart request generator, in-app Tutor, machine transport — later
- Feedback recommendations of what to study next — later
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
| Proposed ADR 0002 Git-native Portable notebook synchronization | Standard Git remote and linear `main`, no Donut-specific local state, conservative identity projection, and future repository-subtree binding | Proposed 2026-09-04; not binding |
| Keep catalog ZIP only | One-way portability still useful; E2E `notebook_export.feature` | ✓ Retained |
| Accidental-match resolve via dialog (not stacked notes) | Full-height reviewed note stays primary; resolution is optional and compact | ✓ Shipped v1.2 |
| Overlap from dialog skips try-again / credit reclaim | Declaring overlap is the action; no secondary retry/credit flow | ✓ Shipped v1.2 |
| Distinct `overlaps` vs plain `aliases` | Wiki-link overlap declarations are not aliases; grading reads `overlaps` | ✓ Shipped v1.2 |
| Commissioned learning glossary (ADR 0001) | Tutor / Learning Session / Feedback names | ✓ |
| Markdown copy-paste protocol | [Commissioned learning session protocol](../docs/commissioned-learning-session-protocol.md) | ✓ |
| Feedback Grade → schedule (ADR 0003) | Grades 1–4 (= FSRS G) | ✓ |
| Memory tracker `type` enum | `COMMISSIONED` alongside `UNDERSTANDING` / `SPELLING` | ✓ |

## Evolution

This document evolves at phase transitions and milestone boundaries.

---
*Last updated: 2026-09-04*
