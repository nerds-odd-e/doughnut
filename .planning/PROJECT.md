# Doughnut

## What This Is

Doughnut is a Personal Knowledge Management tool combining zettelkasten-style note capture, spaced repetition, and knowledge sharing.

## Core Value

Healthy mainline for learning and knowledge work. Approved [ADR 0002](../docs/adrs/0002-git-native-notebooks-backed-by-mysql.md) sets git-native notebooks (MySQL-backed) as the direction for portable content; one-way catalog ZIP export remains.

## Current State

**Shipped v1.2 (2026-08-06):** Accidental-match resolve dialog UX; distinct `overlaps` frontmatter; reviewed note stays primary.

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop end-to-end.

**Shipped v1.0 (2026-07-23):** Notebook Health lint + Health tab + gated empty-folder purge.

Portable content direction is ADR 0002 (git-native notebooks). Catalog ZIP download remains (`notebook_export.feature`); path-keyed CLI sync / `.doughnut-sync` is not part of the product.

## Current Milestone: v1.3 Commissioned Learning Session MVP

**Goal:** The Learning Orchestrator commissions a Tutor to conduct an appropriate Learning Session, then records the resulting Learning Session (full offline copy-paste loop; score-only Feedback).

**Target features:**
- Assimilate a note as a commissioned memory tracker (caret next to Assimilate; coexists with ordinary trackers)
- Potential learning sessions by notebook on the recall page progress bar
- Commission → Learning Session Request (markdown, ADR 0005)
- Record Learning Session Report → schedule from score (ADR 0003) + feedback log; amend later reports

**Key context:** Domain terms in ADR 0001 §3; protocol ADR 0005 (Proposed); score mapping in ADR 0003 (Proposed). Behavioral scope: `.planning/phases/01-commissioned-tracker-model/commissioned_learning_session.feature`. Amend recomputation deferred to plan-phase 7.

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

### Active

- [ ] Commissioned memory tracker assimilation and coexistence (TRK-*)
- [ ] Potential learning sessions by notebook on recall (POT-*)
- [ ] Commission Learning Session + Request markdown (COM-*)
- [ ] Record Report → score schedule + feedback log; amend (REC-*, AMD-01)

See `.planning/REQUIREMENTS.md` for REQ-IDs.

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

## Context

Accepted ADRs under `docs/adrs/`. Planning history for completed milestones is not retained under `.planning/` (product + ADRs are the record).

## Constraints

- **Stack:** Prefer high-level tests; Behavior/Structure phased delivery; Nix tooling via `CURSOR_DEV=true nix develop -c …`
- **Stop-safe:** After each phase the tree must remain healthier (or no worse) than before
- **ADRs:** Follow Accepted ADRs; humans own approval (`docs/adrs/`)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| ADR 0002 git-native notebooks | Git authoritative for portable content; retire `.doughnut-sync` | ✓ Approved 2026-08-04 |
| Keep catalog ZIP only | One-way portability still useful; E2E `notebook_export.feature` | ✓ Retained |
| Accidental-match resolve via dialog (not stacked notes) | Full-height reviewed note stays primary; resolution is optional and compact | ✓ Shipped v1.2 |
| Overlap from dialog skips try-again / credit reclaim | Declaring overlap is the action; no secondary retry/credit flow | ✓ Shipped v1.2 |
| Distinct `overlaps` vs plain `aliases` | Wiki-link overlap declarations are not aliases; grading reads `overlaps` | ✓ Shipped v1.2 |
| Commissioned learning glossary (ADR 0001 §3) | Lock Tutor / Learning Session / Feedback names before build | Proposed — binding for new work |
| Markdown copy-paste protocol (ADR 0005) | No existing ed-tech standard fits offline Tutor exchange | Proposed |
| Tutor score → schedule (ADR 0003) | Shifted-band 0–5 mapping; mastery always progresses | Proposed |

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
*Last updated: 2026-08-07 — start milestone v1.3 Commissioned Learning Session MVP*
