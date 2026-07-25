# Spelling Answer Match & Link

## What This Is

A recall capability in Doughnut: when a user answers a **spelling** recall question and the typed answer matches the title or alias of **another** note (not the one being reviewed), the system detects an **accidental match** — applies a slight spaced-repetition penalty, reveals both notes, and lets the user build a link between them (property link or relationship note) via the existing add-link UI. It also handles the **overlap** case: when the answer is correct for the reviewed note but the reviewed note declares (via an alias-as-wiki-link) that it overlaps with another note, the system says "correct, but we're looking for another answer — try again," with no credit.

## Core Value

During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.

## Current State

**Shipped v1.1 (2026-07-25):** Accidental-match + overlap spelling recall loop is live end-to-end (API outcomes, grading, UI reveal/link offer, alias-as-wiki-link declaration, try-again with durable outcome persistence).

**Shipped v1.0 (2026-07-23):** Notebook Health lint (empty / readme-only / dead wiki links) + Health tab + gated empty-folder purge.

## Next Milestone Goals

Define via `/gsd-new-milestone`. Candidate deferred scope from v1.1:

- MCQ accidental-match
- Fuzzy / partial / substring answer matching
- Cross-notebook qualified `Notebook:Title` typing

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading — existing
- ✓ Spaced-repetition scheduling with success/failure/partial paths — existing
- ✓ Wiki-link resolution by title or alias — existing
- ✓ Note aliases in frontmatter, indexed — existing
- ✓ Add-link UI (wiki / property / relationship) — existing
- ✓ Notebook Health lint + tab + gated empty-folder purge — v1.0
- ✓ `AnswerOutcome` + matched-note topology on Answer/AnsweredQuestion; OpenAPI client — v1.1 (API-01, API-02)
- ✓ Accidental-match detection + lighter SRS penalty — v1.1 (AM-01, AM-02)
- ✓ Reveal matched notes after accidental match — v1.1 (AM-03)
- ✓ Offer link with matched note pre-selected — v1.1 (AM-04)
- ✓ Alias-as-wiki-link overlap declaration without resolve/search/cloze regressions — v1.1 (OVL-02, OVL-03)
- ✓ Overlap try-again with no SRS credit — v1.1 (OVL-01)

### Active

_(none — next milestone requirements TBD via `/gsd-new-milestone`)_

### Out of Scope

- MCQ questions — spelling only in v1.1
- Auto-creating links without user choice
- Fuzzy / partial / substring match
- Re-assimilation threshold changes for plain wrong answers
- Cross-notebook qualified `Notebook:Title` typing
- LLM / semantic match

## Context

Shipped the full three-problem spelling loop: unreliable memory (accidental match), potential link (offer link), and declared overlap (try again). Built on existing `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, and `updateForgettingCurve`.

## Constraints

- **Behavior scope:** Spelling recall only; no LLM
- **Match scope:** All notebooks the user can read
- **Safety:** Link-building is user-initiated; overlap try-again does not mutate note data
- **Stack:** Spring Boot + Vue + E2E; Behavior/Structure phased delivery

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| v1 covers accidental match + link + overlap | Combined recall-improvement loop; half-feature not acceptable | ✓ Good — shipped v1.1 |
| Match scope: all readable notebooks | Confusion/links cross notebook boundaries | ✓ Good |
| Accidental-match penalty lighter than wrong | User knew a real note, just not this one | ✓ Good — `partialFail` −10 |
| Overlap via wiki-link values in aliases | Reuses known alias concept; user opts in per pair | ✓ Good — plain-only `from*` |
| Overlap declared, not auto-detected | Shared alias auto-detect too aggressive | ✓ Good |
| Reuse WikiLinkResolver + LinkInsertionChoice | Minimize new concepts | ✓ Good |
| Third SRS outcome via updateForgettingCurve | No new scheduling machinery | ✓ Good |
| Persist `quiz_answer.outcome`; exclude OVERLAP from wrong-count | Repeated try-agains must not trip re-assimilation | ✓ Good |

<details>
<summary>Pre-v1.1 project draft (archived)</summary>

Earlier active requirements and phase-by-phase “Current State” notes lived here while phases 1–6 were in flight. See `.planning/milestones/v1.1-ROADMAP.md` and `.planning/MILESTONES.md` for the shipped record.

</details>

## Evolution

This document evolves at milestone boundaries via `/gsd-complete-milestone` and `/gsd-new-milestone`.

---
*Last updated: 2026-07-25 after v1.1 milestone*
