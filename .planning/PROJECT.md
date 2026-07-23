# Spelling Answer Match & Link

## What This Is

A recall capability in Doughnut: when a user answers a **spelling** recall question and the typed answer matches the title or alias of **another** note (not the one being reviewed), the system detects an **accidental match** — applies a slight spaced-repetition penalty, reveals both notes, and lets the user build a link between them (property link or relationship note) via the existing add-link UI. It also handles the **overlap** case: when the answer is correct for the reviewed note but the reviewed note declares (via an alias-as-wiki-link) that it overlaps with another note, the system says "correct, but we're looking for another answer — try again," with no credit.

## Core Value

During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.

## Requirements

### Validated

- ✓ Spelling recall question type and answer grading (`RecallPrompt`/`SpellingQuestion`/`AnswerSpellingDTO`/`MemoryTrackerService.answerSpelling`/`Note.matchAnswer`) — existing
- ✓ Spaced-repetition scheduling with success/failure paths and a partial-adjustment API (`MemoryTracker`/`ForgettingCurve`/`updateForgettingCurve`) — existing
- ✓ Wiki-link resolution by title or alias, notebook-scoped (`WikiLinkResolver.resolveWikiLinkToken`) — existing
- ✓ Note aliases in frontmatter, indexed (`FrontmatterAliases`/`NoteAliasIndex`/`NoteAliasIndexService`) — existing
- ✓ Add-link UI: wiki link, property link, relationship note (`SearchForm`/`LinkInsertionChoice`/`AddRelationshipFinalize`) — existing
- ✓ Notebook settings Health tab + recall-stats tab — existing (v1.0 lint milestone shipped)

### Active

- [ ] Accidental-match detection: when a spelling answer is wrong for the reviewed note but matches another note's title or alias, detect the match (search all notebooks the user can read)
- [ ] Slight (lighter-than-wrong) penalty for accidental match — a third SRS outcome (not full `recallFailed` −20/12h)
- [ ] Reveal the reviewed note and the matched note(s) together after an accidental match
- [ ] Offer to build a link between the notes via the existing add-link UI (property link or relationship note), with the matched note pre-selected
- [ ] Overlap handling: when the answer is correct for the reviewed note but the reviewed note declares overlap with another note, respond "correct, but we're looking for another answer — try again," no credit
- [ ] Overlap declaration model: extend the `aliases` frontmatter to accept wiki-link values pointing to another note
- [ ] API/DTO extension: `Answer.correct` (boolean) → third outcome with accidental-match metadata (matched note id, overlap flag); `AnsweredQuestion` extension; OpenAPI regen

### Out of Scope

- MCQ questions — spelling only in v1 (MCQ grading is separate)
- Auto-creating links without user choice — link-building is user-initiated via the add-link UI
- Fuzzy / partial / substring match — exact title or alias only in v1
- Re-assimilation threshold changes — existing wrong-answer threshold behavior unchanged
- Cross-notebook qualified `Notebook:Title` typing — v1 searches all readable notebooks transparently

## Context

**Problem:** During spelling recall, a user often types the title of a *different* note — either because they confuse two notes (unreliable memory), because the two notes are related (a potential link), or because the two notes overlap in name but must stay distinct. Today grading is binary (`Note.matchAnswer` → correct/incorrect); the connection to the other note is invisible and the overlap case silently accepts a non-distinguishing answer.

**Three problems this addresses:**
1. **Unreliable memory** — the answer matching another note signals the user is confusing the two notes (recall-quality signal).
2. **Potential link** — the match reveals a relationship worth linking.
3. **Overlap that must stay distinct** — the answer is technically correct but also names another note; the two notes must remain distinct, so the system asks for a more specific answer.

**Product fit:** Doughnut already grades spelling answers, resolves wiki links by title/alias, indexes aliases, and has an add-link UI with property-link and relationship-note choices. This feature wires those together at the grading moment.

**Integration point (from codebase map):** `MemoryTrackerService.answerSpelling` (255–280), after `Note.matchAnswer`:
- `correct=false` + `resolveWikiLinkToken(answer,…)` finds another note → accidental match (problems 1 & 2)
- `correct=true` + reviewed note declares overlap with another note (alias-as-wiki-link) → overlap (problem 3)

## Constraints

- **Behavior scope:** Spelling recall only in v1; no LLM calls
- **Match scope:** All notebooks the user can read (broader than the notebook-scoped `WikiLinkResolver` — needs a wider lookup than the existing unqualified resolver)
- **Match semantics:** Answer matches another note's title or alias (exact, case-insensitive); if it already matches the reviewed note, the accidental-match search is skipped (overlap is declared, not auto-detected)
- **Penalty:** Lighter than a plain wrong answer (e.g. `updateForgettingCurve(−10)`-style, no 12h override) — a third SRS outcome
- **Safety:** Link-building is user-initiated; the system never auto-writes links. Overlap "try again" withholds credit but does not mutate note data
- **Stack:** Existing Spring Boot + Vue + E2E; reuse `WikiLinkResolver`, `Note.matchAnswer`, `LinkInsertionChoice`, `updateForgettingCurve`; follow Behavior/Structure phased delivery
- **Alias blast radius:** Extending aliases to accept wiki links affects wiki resolve, search, and cloze masking — the overlap phase must handle this carefully (expect a design spike)

## Key Decisions

| Decision | Rationale | Outcome |
|----------|-----------|---------|
| v1 covers all three problems (accidental match + link + overlap) | User value is the combined recall-improvement loop; splitting would ship a half-feature | — Pending |
| Match scope: all notebooks the user can read | Confusion/links cross notebook boundaries; notebook-scoped would miss real matches | — Pending |
| Accidental-match penalty lighter than a plain wrong answer | The user did know a real note, just not this one; full wrong-answer penalty is too harsh | — Pending |
| Overlap modeled by extending aliases to accept wiki links | Reuses the alias concept the user already knows; declares "A overlaps B" as a link | — Pending (broad blast radius — spike) |
| Overlap is declared, not auto-detected | Auto-detecting any shared alias as overlap would be too aggressive; user opts in per pair | — Pending |
| Reuse WikiLinkResolver + LinkInsertionChoice | Existing matching + add-link UI; minimize new concepts | — Pending |
| Third SRS outcome via updateForgettingCurve | Existing partial-adjustment API; no new scheduling machinery | — Pending |

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
*Last updated: 2026-07-23 after initialization*
