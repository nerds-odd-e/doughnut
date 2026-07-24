# Roadmap: Spelling Answer Match & Link

## Project

**Core value:** During spelling recall, an answer that names a *different* note becomes a learning opportunity — penalized lightly, both notes revealed, and a link offered — turning recall confusion into connection-building; and overlapping-but-distinct notes are kept distinct by asking the user for a more specific answer.

**Granularity:** fine (6–10 phases; natural boundaries stand)
**Phase ID convention:** sequential (`Phase N`)
**Project mode:** mvp

## Phases

- [x] **Phase 1: Extend Answer outcome API** - Add a third outcome (accidental-match + overlap) to the Answer/AnsweredQuestion contract and regen the OpenAPI client (completed 2026-07-23)
- [x] **Phase 2: Accidental-match grading & penalty** - Detect a spelling answer that names a different note and apply a lighter-than-wrong SRS penalty (completed 2026-07-24)
- [x] **Phase 3: Reveal both notes after accidental match** - Show the reviewed note and the matched note(s) together after an accidental match (completed 2026-07-24)
- [x] **Phase 4: Offer link between notes** - Let the user build a link (property link or relationship note) between the reviewed and matched note via the existing add-link UI, with the matched note pre-selected (completed 2026-07-24)
- [ ] **Phase 5: Alias-as-wiki-link overlap declaration** - Extend `aliases` frontmatter to accept wiki-link values pointing to another note, preserving wiki-resolve/search/cloze-masking behavior
- [ ] **Phase 6: Overlap "try again, no credit"** - When the answer is correct but the reviewed note declares overlap, respond "correct, but we're looking for another answer — try again" with no credit

## Phase Details

### Phase 1: Extend Answer outcome API

**Goal:** The backend→frontend answer contract can represent a third outcome (accidental-match with matched-note id, and an overlap flag) instead of only a boolean `correct`.
**Mode:** mvp
**Depends on**: Nothing (first phase; the contract change is the foundation for every later behavior)
**Requirements**: API-01, API-02
**Success Criteria** (what must be TRUE):

  1. The `Answer` outcome type carries an accidental-match state with a matched-note id (not just a boolean `correct`).
  2. The `AnsweredQuestion` response carries matched-note topology and an overlap flag.
  3. The regenerated OpenAPI client compiles and the frontend type-checks against the new contract (no backend behavior wired yet — the new states are representable but not yet returned).

**Plans**: 1/1 plans executed
**UI hint**: no

Plans:

- [x] 01-01-PLAN.md — Extend the Answer/AnsweredQuestion contract (Option A: @Transient matchedNoteId + AnswerOutcome enum on Answer; optional overlap + matchedNotes: List<NoteTopology> on AnsweredQuestion), regenerate the OpenAPI client, verify the @Transient fields surface in types.gen.ts (A1), confirm the frontend type-checks, and pin representable-but-not-returned with no-behavior backend tests on both contract surfaces (AnsweredQuestion + RecallPromptHistoryItem)

### Phase 2: Accidental-match grading & penalty

**Goal:** When a user types a spelling answer that is wrong for the reviewed note but matches another note's title or alias (searched across all notebooks the user can read), the system grades it as an accidental match with a lighter-than-wrong spaced-repetition penalty.
**Mode:** mvp
**Depends on**: Phase 1
**Requirements**: AM-01, AM-02
**Success Criteria** (what must be TRUE):

  1. A spelling answer that matches another note's title or alias (across all readable notebooks) is graded as an accidental match, distinct from a plain wrong answer.
  2. An accidental match applies a lighter SRS penalty than a plain wrong answer (a third outcome via `updateForgettingCurve`, no 12h override).
  3. When the answer already matches the reviewed note, the accidental-match search is skipped (overlap is declared, not auto-detected).

**Plans**: 2/2 plans executed

Plans:
**Wave 1**

- [x] 02-01-PLAN.md — Wire the end-to-end title-leg accidental-match path (WikiLinkResolver.findAccidentalMatch + NoteRepository.findByNoteTitleOrderByIdAsc + ForgettingCurve.partialFail + MemoryTracker.markAsAccidentalMatch + MemoryTrackerService wiring) as the first production writer of Answer.matchedNoteId/outcome=ACCIDENTAL_MATCH with the lighter -10 clamped penalty (no 12h), plus IDOR-unreadable and skip-when-correct-shared-title controller tests

**Wave 2** *(blocked on Wave 1 completion)*

- [x] 02-02-PLAN.md — Add the alias leg of findAccidentalMatch (NoteAliasIndexRepository.findByAliasLookupKeyOrderByNoteIdAsc + title-then-alias fallback) and the floor-clamp + threshold-counts edge-case controller tests

### Phase 3: Reveal both notes after accidental match

**Goal:** As a learner doing spelling recall, I want to see the reviewed note and all matched notes revealed together after an accidental match, so that my confusion becomes visible.
**Mode:** mvp
**Depends on**: Phase 2
**Requirements**: AM-03
**Success Criteria** (what must be TRUE):

  1. After an accidental match, the reviewed note and the matched note are both shown to the user in the spelling answer result.
  2. When multiple notes match, all matched notes are surfaced (not just one).

**Plans**: 3/3 plans executed
**UI hint**: yes

Plans:
**Wave 1**

- [x] 03-01-PLAN.md — findAllAccidentalMatches + populate AnsweredQuestion.matchedNotes on answer-spelling (tracer) + IDOR list coverage

**Wave 2** *(blocked on Wave 1)*

- [x] 03-02-PLAN.md — AnsweredSpellingQuestion ACCIDENTAL_MATCH alert + vertical Matched note(s) NoteShow stack + Vitest

**Wave 3** *(blocked on Waves 1–2)*

- [x] 03-03-PLAN.md — capability-named E2E accidental_match_reveal + human spot-check (approved)

### Phase 4: Offer link between notes

**Goal:** As a learner doing spelling recall, I want to build a link between the reviewed note and a matched note after an accidental match with the matched note already selected, so that I can turn recall confusion into a lasting connection without the system auto-writing a link.
**Mode:** mvp
**Depends on**: Phase 3
**Requirements**: AM-04
**Success Criteria** (what must be TRUE):

  1. After an accidental match, the user is offered the existing add-link UI (property link or relationship note) to connect the reviewed note to a matched note.
  2. The matched note is pre-selected in the add-link UI, so the user can confirm a link with minimal effort.
  3. The system never auto-writes a link — link creation is user-initiated.

**Plans**: 3/3 plans executed
**UI hint**: yes

Plans:
**Wave 1**

- [x] 04-01-PLAN.md — Tracer: per-matched CTA + MatchedNoteLinkOffer + bareWikiLinkAvailable + property wiki-link via updateTextField + Vitest (D-01–D-06 property path)

**Wave 2** *(blocked on Wave 1)*

- [x] 04-02-PLAN.md — Relationship finalize with skipNavigation / navigateOnSuccess=false so D-07 stays on recall result + Vitest

**Wave 3** *(blocked on Waves 1–2)*

- [x] 04-03-PLAN.md — Extend accidental_match_reveal E2E for offer-link (property + relationship) + human spot-check (approved)

### Phase 5: Alias-as-wiki-link overlap declaration

**Goal:** As a note author, I want to declare overlap by putting well-formed wiki-link tokens in the aliases frontmatter list, so that overlapping notes are declared for later grading without breaking plain-alias wiki-resolve, search, or cloze masking.
**Mode:** mvp
**Depends on**: Phase 1 (overlap flag in the contract); independent of Phases 2–4
**Requirements**: OVL-02, OVL-03
**Success Criteria** (what must be TRUE):

  1. A note's `aliases` frontmatter can contain a wiki-link value (e.g. `[[Other Note]]`) that declares overlap with another note.
  2. Existing wiki-link resolution by title/alias still resolves correctly for plain (non-wiki-link) aliases.
  3. Existing search behavior is unchanged for plain aliases (a wiki-link alias is not surfaced as a searchable title where it shouldn't be).
  4. Existing cloze-masking behavior is unchanged (a wiki-link alias does not leak or break cloze deletion).

**Plans**: 1/3 plans executed

> **⚠️ Known risk — alias blast radius:** Extending `aliases` to accept wiki-link values touches the derived-index coherence path (wiki title / property / alias caches refreshed via `WikiTitleCacheService.refreshForNote` and backfills — see `.planning/codebase/CONCERNS.md` "Derived index coherence"). Missed refresh sites recreate assimilation/search/cloze bugs. Treat this phase as a design spike: enumerate every consumer of `aliases` (wiki resolve, search index, cloze masking, `NoteAliasIndex`/`NoteAliasIndexService`, `FrontmatterAliases`) before changing the parser, and gate on regression tests for each consumer. Expect this phase to take longer than its neighbors; do not rush it.

Plans:
**Wave 1**

- [x] 05-01-PLAN.md — Tracer: FrontmatterAliases plain/wiki segregation + overlapWikiLinkTokensFrom* + HTTP accept + frontend authoredAliasesValidation parity (D-01–D-04 / OVL-02)

**Wave 2** *(blocked on Wave 1; 05-02 ∥ 05-03)*

- [ ] 05-02-PLAN.md — OVL-03 index + alias search: plain-only note_alias_index rows; wiki-link items not searchable
- [ ] 05-03-PLAN.md — OVL-03 wiki-resolve + cloze + matchAnswer + accidental-match alias leg ignore wiki-link items

### Phase 6: Overlap "try again, no credit"

**Goal:** When a spelling answer is correct for the reviewed note but the reviewed note declares overlap with another note, the system responds "correct, but we're looking for another answer — try again," with no credit and no note-data mutation.
**Mode:** mvp
**Depends on**: Phase 5
**Requirements**: OVL-01
**Success Criteria** (what must be TRUE):

  1. When the answer is correct for the reviewed note AND the reviewed note declares overlap (via an alias-as-wiki-link), the user is told "correct, but we're looking for another answer — try again."
  2. An overlap response gives no SRS credit (the review is not marked correct).
  3. An overlap response does not mutate note data — the user simply retries the same review.

**Plans**: TBD
**UI hint**: yes

## Progress

| Phase | Plans Complete | Status | Completed |
|-------|----------------|--------|-----------|
| 1. Extend Answer outcome API | 1/1 | Complete    | 2026-07-23 |
| 2. Accidental-match grading & penalty | 2/2 | Complete    | 2026-07-24 |
| 3. Reveal both notes after accidental match | 3/3 | Complete    | 2026-07-24 |
| 4. Offer link between notes | 3/3 | Complete    | 2026-07-24 |
| 5. Alias-as-wiki-link overlap declaration | 1/3 | In Progress|  |
| 6. Overlap "try again, no credit" | 0/0 | Not started | - |

## Coverage

All 9 v1 requirements mapped:

| Requirement | Phase |
|-------------|-------|
| AM-01 | Phase 2 |
| AM-02 | Phase 2 |
| AM-03 | Phase 3 |
| AM-04 | Phase 4 |
| OVL-01 | Phase 6 |
| OVL-02 | Phase 5 |
| OVL-03 | Phase 5 |
| API-01 | Phase 1 |
| API-02 | Phase 1 |

✓ 9/9 v1 requirements mapped — no orphans, no duplicates.

---
*Last updated: 2026-07-23 during roadmap creation*
