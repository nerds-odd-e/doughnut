# Project Research Summary

**Project:** Doughnut (v1.2 Accidental Match Resolve UX)
**Domain:** PKM + spaced-repetition recall UX — compact accidental-match resolve dialog
**Researched:** 2026-08-05
**Confidence:** HIGH

## Executive Summary

Doughnut v1.2 is a **frontend presentation milestone**, not a new product surface. Experts in PKM (Obsidian/Logseq) and SRS (RemNote/Anki) surface title collisions as optional identity + actions—not full-content stacks mid-review. Doughnut already detects `ACCIDENTAL_MATCH` at grade time (v1.1); this milestone re-homes that reveal into a compact optional resolve dialog so the reviewed note keeps full-height focus.

**Recommended approach:** Add **zero new runtime libraries**. Compose existing Vue 3 + DaisyUI `PopButton`/`Modal`, breadcrumb, `MatchedNoteLinkOffer`, and content-update seams. Prefer client-side `NoteRealm` hydrate for notebook path over OpenAPI enrichment. Order delivery so stacked match bodies disappear as soon as the CTA + dialog shell ships (stop-safe), then path display, then “Build a link” (single Modal step swap—never nested modals), then “Add as overlapped note” as a wiki-link frontmatter write that **must not** re-grade or show OVERLAP try-again.

**Key risks:** (1) removing stacked NoteShows without a compact reveal (AM-03 regression); (2) nesting PopButtons for link offer (focus/close bugs); (3) conflating dialog overlap declare with `AnswerOutcome.OVERLAP` try-again / SRS reclaim (ADR 0003 violation)—the highest-risk behavior pitfall. Mitigate with outcome-discriminated UI, wiki-link token asserts, and E2E that keeps OVERLAP try-again coverage green while rewriting accidental-match scenarios.

## Key Findings

### Recommended Stack

Full detail: [STACK.md](./STACK.md). Verdict is reuse-only: Vue 3.5.40, vue-router 5.2.0, DaisyUI 5.7.15 / Tailwind 4.3.3, existing `@generated/doughnut-backend-api`, Vitest + Cypress. No Headless UI / Radix / third-party modal packages; Doughnut’s native `<dialog showModal>` Modal already matches DaisyUI 5 guidance.

**Core technologies:**
- **Vue 3 + in-repo `PopButton`/`Modal`:** Resolve dialog shell — Teleport, ESC, route-close already correct
- **`BreadcrumbWithCircle` + `getNoteRealmRefAndLoadWhenNeeded`:** Notebook path under each match — `NoteTopology` alone lacks trail
- **`MatchedNoteLinkOffer` + `appendAlias`/`buildWikiLinkText` + `updateTextField`:** Build link / declare overlap — existing mutation paths; no new RPC
- **Vitest browser + Cypress:** Dialog flows and “no try-again after dialog overlap” — no new test framework

### Expected Features

Full detail: [FEATURES.md](./FEATURES.md). Table stakes are identity + optional actions (Obsidian users ask for jump/merge from duplicate notices; RemNote/Anki treat interference fixes as optional). Differentiators are recall-time detection → graph fix without leaving the learning loop, dual verbs (link vs overlap alias), and compact dialog vs stacked notes.

**Must have (table stakes):**
- Keep accidental-match alert; remove stacked matched `NoteShow`s — restore full-height reviewed note
- CTA **Resolve accidental match** → optional dialog (dismiss anytime)
- Match rows: clickable title + notebook breadcrumb (no body peek)
- Per match: **Build a link** | **Add as overlapped note**
- Title navigate + reopen resolve on return; stay-on-page after link; readonly gates

**Should have (competitive):**
- Dual resolve verbs unique to Doughnut overlap wiki-link aliases (future OVERLAP try-again on later reviews)
- Multi-match list in one dialog; reopen after title navigation
- Non-grading resolve (lighter penalty already applied in v1.1)

**Defer (v2+):**
- Fuzzy/MCQ/`Notebook:Title` match (SEED-001), merge notes, distinguish-card auto-create, Health backlog for unresolved matches, batch “mark all overlapped”

### Architecture Approach

Full detail: [ARCHITECTURE.md](./ARCHITECTURE.md). Frontend-only change on the accidental-match result. Backend grading and SRS unchanged. Single Modal with stepped content (`list` | `linkOffer`); realm hydrate for path; overlap-from-dialog = content write only.

**Major components:**
1. **`AnsweredSpellingQuestion`** — Alert + Resolve CTA; drop stacked matches; host Modal
2. **`AccidentalMatchResolveDialog` (+ optional row)** — Compact match list + action orchestration
3. **`MatchedNoteLinkOffer` (reuse)** — Build-a-link as in-dialog step, not nested PopButton
4. **Overlap append util** — `buildWikiLinkText` + wiki-link alias merge → `updateTextField`
5. **Backend (`MemoryTrackerService` / `FrontmatterAliases`)** — Unchanged; future OVERLAP consumes declared wiki-links

### Critical Pitfalls

Full detail: [PITFALLS.md](./PITFALLS.md).

1. **Reveal lost when removing stacked NoteShows** — Replace with CTA → dialog list in the same phase; rewrite E2E selectors, don’t delete coverage
2. **Nested PopButton for Build a link** — Single Modal; swap step to `MatchedNoteLinkOffer`; keep stay-on-result
3. **Overlap declare → try-again / SRS reclaim** — Content mutation only; leave `ACCIDENTAL_MATCH` outcome; assert no `overlap-try-again`
4. **Plain alias instead of wiki-link overlap token** — Use `buildWikiLinkText` / overlap-token shape; assert `overlapWikiLinkTokensFrom*`
5. **Dialog state dies on title navigation** — Allow navigate; guarantee Resolve CTA reopen on return (manual reopen is the minimum bar)

## Implications for Roadmap

Based on research, suggested phase structure (aligns with ARCHITECTURE build order + PITFALLS mapping; Behavior/Structure grammar):

### Phase 1: Compact result + Resolve dialog shell
**Rationale:** Milestone value is full-height reviewed note; stop-safe only if reveal (match identity) survives when stacks are removed. Architecture says drop stacks when introducing CTA/dialog—not as a later cleanup.
**Type:** Behavior (structure of removing stacks is inseparable from the new reveal behavior)
**Delivers:** Stacked matched `NoteShow`s gone; alert + **Resolve accidental match** CTA; Modal lists match titles from `matchedNotes`; optional open/dismiss; OVERLAP chrome stays outcome-gated
**Addresses:** Remove stacked notes; Resolve CTA + dialog shell; optional dismiss (FEATURES P1)
**Avoids:** Pitfalls 1, 2 (mandatory/buried CTA), 8 (OVERLAP coupling)

### Phase 2: Path / breadcrumb on match rows
**Rationale:** Identity without path is hostile PKM UX (Obsidian disambiguates by path); stack prefers client `NoteRealm` hydrate over API enrichment.
**Type:** Behavior
**Delivers:** Per-row notebook breadcrumb via `getNoteRealmRefAndLoadWhenNeeded`; clickable titles (nav may land in Phase 4 E2E, but links can ship here)
**Addresses:** Match rows title + path (FEATURES P1)
**Uses:** Breadcrumb components, StoredApi realm load (STACK)
**Avoids:** Mounting `NoteShow` inside dialog; widening `NoteTopology`

### Phase 3: Build a link from resolve dialog
**Rationale:** Highest-frequency graph fix; reuse existing offer; architecture requires single-Modal step swap before overlap write.
**Type:** Behavior
**Delivers:** Per-row **Build a link** → step into `MatchedNoteLinkOffer`; stay on result; readonly/unload gates ported
**Addresses:** Build a link reuse (FEATURES P1); stay-on-page AM-04
**Implements:** Pattern “single Modal, stepped content”
**Avoids:** Pitfall 3 (nested modals); Pitfall 7 (readonly gates)

### Phase 4: Add as overlapped note (no try-again / no reclaim)
**Rationale:** Highest-risk behavior; depends on dialog shell + preferably a small Structure util for wiki-link append. Must not touch grading UI.
**Type:** Behavior (preceded by optional Structure: overlap-alias append helper)
**Delivers:** **Add as overlapped note** writes wiki-link alias; result stays ACCIDENTAL_MATCH; no try-again / credit reclaim; schedule unchanged
**Addresses:** Dual resolve verb; overlap declare rules (FEATURES P1)
**Avoids:** Pitfalls 4 and 5 (regrade / plain alias)—**must research-assert in plan**

### Phase 5: Title navigate + reopen + E2E polish
**Rationale:** Soft dependency on result remount/state; minimum bar is CTA reopen, not auto-reopen dialog. Close the “looks done but isn’t” checklist and page objects.
**Type:** Behavior + verification
**Delivers:** Title → note → return → Resolve CTA → same matches; multi-match / readonly E2E; `overlap_try_again.feature` still green; page object dialog selectors
**Addresses:** Navigate + reopen (FEATURES P1); E2E rewrite
**Avoids:** Pitfall 6; soft-deleted E2E asserts

### Optional Structure before Phase 4
**Structure — overlap alias append util:** Pure helper wrapping `buildWikiLinkText` + wiki-link list merge; unit-testable; enables Phase 4 without bloating the dialog SFC. Architecture lists this as step 1 of the dependency-aware build order—roadmapper may place it immediately before Phase 4 or as Phase 4a.

### Phase Ordering Rationale

- **Value first:** Compact reviewed-note focus (Phase 1) is the milestone goal; every later phase builds on that shell
- **Architecture dependency:** Shell → path hydrate → link step → overlap write → nav/reopen E2E; backend enrichment is **off** the critical path
- **Pitfall sequencing:** Nested-modal risk before link (Phase 3); ADR 0003 / wiki-link shape risk before overlap (Phase 4); navigation state last so CTA persistence is already proven
- **Stop-safe:** After Phase 1 alone, users get healthier recall chrome + optional title list; actions can land incrementally without stacked-body waste
- **Anti-feature lock:** No content peek, no forced resolve, no merge, no SRS math change, no SEED-001 fuzzy in this milestone

### Research Flags

Phases likely needing deeper research during planning:
- **Phase 4 (Add as overlapped):** Wiki-link frontmatter shape vs plain `appendAliasToNoteContent`; exact util extension; assert against `FrontmatterAliases.overlapWikiLinkTokensFrom*` — sparse “how to declare” docs outside code
- **Phase 5 (Navigate + reopen):** How answered-question remount/session works when leaving recall via title — may need `/gsd-plan-phase --research` on result persistence

Phases with standard patterns (skip research-phase):
- **Phase 1 (shell + drop stacks):** In-repo Modal/PopButton + existing accidental-match unit/E2E patterns
- **Phase 2 (breadcrumb hydrate):** Same seam as `canOfferLinkToMatched` / link offer
- **Phase 3 (Build a link):** Rehost existing `MatchedNoteLinkOffer` as step; well-documented in-repo

## Confidence Assessment

| Area | Confidence | Notes |
|------|------------|-------|
| Stack | HIGH | Repo pins + in-repo seams; Context7 DaisyUI/Vue only corroborative |
| Features | MEDIUM | Locked PRODUCT/milestone decisions HIGH; competitor norms MEDIUM (forums/docs) |
| Architecture | HIGH | Verified against shipped components, DTOs, ADR 0003 |
| Pitfalls | HIGH | Integration pitfalls vs v1.1 code + ADR; Vue Router modal-state MEDIUM |

**Overall confidence:** HIGH for implementation approach and phase order; MEDIUM on competitive feature framing (does not block roadmap).

### Gaps to Address

- **Answer history / reopen without live cache:** Today `Answer` may store only first matched id—validate whether Phase 5 needs anything beyond “result still mounted + CTA”; do not enrich API unless reopen-from-history without cache is required
- **Partial-resolve row state (“already linked/overlapped”):** FEATURES P2—out of v1.2 MVP unless validation demands it; plan only if product insists
- **Auto-reopen vs manual reopen:** Product lock is reopen *capability*; prefer manual CTA reopen unless Phase 5 explicitly specs auto-reopen + E2E
- **SEED-001 fuzzy/MCQ:** Explicitly out of scope; keep off roadmap for this milestone

## Sources

### Primary (HIGH confidence)
- Repo: `frontend/package.json`; `AnsweredSpellingQuestion.vue`, `MatchedNoteLinkOffer.vue`, `Modal.vue`/`PopButton.vue`, breadcrumb/title helpers
- Backend: `MemoryTrackerService`, `AnsweredQuestion.matchedNotes` (`NoteTopology`), `FrontmatterAliases`
- ADR 0003 — accidental match vs declared overlap scheduling
- `.planning/PROJECT.md` — v1.2 locked goals
- Unit/E2E: `AnsweredSpellingQuestionAccidentalMatch.spec.ts`, `accidental_match_reveal.feature`, `overlap_try_again.feature`

### Secondary (MEDIUM confidence)
- Context7 DaisyUI 5 modal docs — native `dialog.showModal()`
- Obsidian/Logseq duplicate-name / path disambiguation discourse
- RemNote leech/interference guidance; Anki sibling bury / confuse-pair practice
- Vue Router modal navigation community patterns (prefer CTA reopen)

### Tertiary (LOW confidence)
- None material for roadmap; SEED-001 and merge/distinguish-card remain speculative deferrals

---
*Research completed: 2026-08-05*
*Ready for roadmap: yes*
