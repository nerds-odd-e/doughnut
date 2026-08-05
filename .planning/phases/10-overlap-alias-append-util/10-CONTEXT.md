# Phase 10: Overlap alias append util - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research + Pitfall 5 + Phases 7–9)

<domain>
## Phase Boundary

A pure, unit-tested frontend helper appends an **overlap wiki-link alias token** (not a plain alias) into note frontmatter `aliases`, suitable for Phase 11’s **Add as overlapped note** without bloating the resolve dialog. Existing accidental-match and OVERLAP try-again user flows stay observably unchanged. No new user-facing **Add as overlapped note** action yet.

**In scope:** Structure-only overlap wiki-link append util + Vitest (roadmap success criteria 1–3).
**Out of this phase:** Wire CTA / `updateTextField` / no try-again / no SRS reclaim (Phase 11 — AMR-08, AMR-09); title navigate reopen polish (Phase 12); SEED-001 / AMR-10..13.

</domain>

<decisions>
## Implementation Decisions

### Helper packaging (avoid Pitfall 5)
- **D-01:** Add a **named sibling util** (capability name e.g. `appendOverlapWikiLinkToNoteContent` under `frontend/src/utils/`) that always produces a wiki-link overlap item. Do **not** teach call sites to pass plain titles into `appendAliasToNoteContent`. Keep Wikidata / plain-alias `appendAliasToNoteContent` semantics unchanged. — **Reversibility:** reversible
- **D-02:** Implement by composing existing pieces: `buildWikiLinkText(...)` → then merge via existing frontmatter append path (`appendAliasToNoteContent` with the `[[…]]` token, or equivalent reuse of `parseNoteContentMarkdown` / `mergeAliasIntoList` / `composeNoteContentMarkdown`). Prefer composition over a second frontmatter parser. — **Reversibility:** reversible

### Wiki-link token shape
- **D-03:** Reuse `buildWikiLinkText` qualification rules: same-notebook → `[[Title]]`; cross-notebook when `notebookName` available → `[[Notebook:Title]]`. Pass reviewed notebook id as `source.notebookId` and match realm/topology as `target` (same shape MatchedNoteLinkOffer uses). — **Reversibility:** reversible
- **D-04:** Do **not** pass `displayText` for overlap declaration (avoid `[[Title|display]]` pipe form). Overlap items should be whole-item wiki-link tokens matching `FrontmatterAliases` / `authoredAliasesValidation` wiki-link rules. — **Reversibility:** reversible

### Merge / idempotency contract
- **D-05:** Preserve the same contract as plain alias append: return updated markdown, or `null` when unchanged / unparseable / `aliases` present but not a YAML list / empty token. Dedupe via existing `mergeAliasIntoList` / `normalizedLookupKey` so repeating the same wiki-link token is a no-op. — **Reversibility:** reversible
- **D-06:** Preserve existing plain alias list items when appending a wiki-link overlap item (mixed `aliases` lists are already supported by backend + authored validation). — **Reversibility:** reversible

### Scope fence and verification
- **D-07:** **No UI wiring this phase** — do not add **Add as overlapped note**, do not call `updateTextField`, do not change `AnsweredSpellingQuestion` / resolve dialog / OVERLAP try-again chrome. Structure only. — **Reversibility:** reversible
- **D-08:** Cover with Vitest at the util boundary (capability-named `*.spec.ts`): appends a well-formed `[[…]]` wiki-link list item (not a bare title); creates `aliases` when absent; merges into existing list; returns `null` on duplicate / bad aliases shape; cross-notebook uses `Notebook:Title` when names/ids warrant it. Prefer asserting positive wiki-link shape + authoredAliasesValidation acceptance over mocking. No E2E required for this Structure phase. — **Reversibility:** reversible
- **D-09:** No backend / OpenAPI / `AnswerOutcome` / SRS changes. Downstream Phase 11 will persist via existing content-edit seam and must not re-grade. — **Reversibility:** reversible

### Claude's Discretion
- Exact util filename/export name (prefer capability clarity: overlap wiki-link append).
- Whether the sibling wraps `appendAliasToNoteContent` in one line or inlines the same merge helpers for clarity — either is fine if Pitfall 5 is avoided and tests lock the wiki-link shape.
- Fixture shape for `buildWikiLinkText` target (minimal stub vs small makeMe-like object) inside the util tests.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2: per match **Add as overlapped note**; overlap skips try-again / credit reclaim
- `.planning/REQUIREMENTS.md` — AMR-08/AMR-09 deferred to Phase 11; Phase 10 is Structure (no requirement ID)
- `.planning/ROADMAP.md` — Phase 10 Structure success criteria; enables Phase 11
- `.planning/STATE.md` — Phase 10 ready; Pitfall: do not conflate dialog overlap with OVERLAP try-again / ADR 0003

### Prior phase context
- `.planning/phases/09-build-a-link-from-resolve-dialog/09-CONTEXT.md` — Build a link done; overlap util deferred here
- `.planning/phases/08-match-path-and-clickable-titles/08-CONTEXT.md` — match realm hydrate patterns Phase 11 will reuse
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-CONTEXT.md` — resolve dialog shell; OVERLAP try-again untouched

### Research (v1.2) — critical for this phase
- `.planning/research/SUMMARY.md` — Structure util before Add as overlapped; wiki-link token not plain alias
- `.planning/research/ARCHITECTURE.md` — build order step 1: pure helper `buildWikiLinkText` + append; Phase 11 wires save
- `.planning/research/STACK.md` — `buildWikiLinkText` + `appendAliasToNoteContent` + later `updateTextField`; zero new libraries
- `.planning/research/PITFALLS.md` — **Pitfall 5**: never write plain alias for overlap; assert wiki-link / `overlapWikiLinkTokensFrom*`
- `.planning/research/FEATURES.md` — locked verb **Add as overlapped note**; declare via aliases wiki-link

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — accidental match vs declared overlap; UI-only this phase; no SRS math change
- `.cursor/rules/planning.mdc` — Structure phase; stop-safe; enables only immediate next behavior (Phase 11)
- `.cursor/rules/unit-testing.mdc` — small-test style at stable util boundary
- `.cursor/rules/frontend-testing.mdc` — Vitest browser / capability-named tests

### Existing implementation to reuse
- `frontend/src/utils/buildWikiLinkText.ts` — wiki-link token (same- vs cross-notebook)
- `frontend/src/utils/wikidataTitleActions.ts` — `appendAliasToNoteContent` (plain-alias merge path; compose with `[[…]]` token only via named overlap helper)
- `frontend/src/utils/frontmatterAliases.ts` — `mergeAliasIntoList` / `normalizedLookupKey`
- `frontend/src/utils/authoredAliasesValidation.ts` — accepts well-formed wiki-link overlap items
- `frontend/src/utils/noteContentFrontmatter.ts` — parse/compose used by append
- `frontend/tests/utils/buildWikiLinkText.spec.ts` — pattern for util Vitest
- `frontend/tests/notes/WikidataAssociationDialog.spec.ts` — existing `appendAliasToNoteContent` cases (plain only)
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — `overlapWikiLinkTokensFrom*` contract (read for shape; no Java change this phase)
- `backend/src/test/java/com/odde/doughnut/algorithms/FrontmatterAliasesWikiLinkOverlapTest.java` — backend overlap token expectations

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `buildWikiLinkText`: already used by `MatchedNoteLinkOffer` / SearchForm for `[[Title]]` / `[[Notebook:Title]]`
- `appendAliasToNoteContent`: frontmatter `aliases` list merge; returns `null` when unchanged or invalid shape
- `authoredAliasesValidation`: frontend mirror of backend authored alias rules (plain + wiki-link items)
- `mergeAliasIntoList` / `normalizedLookupKey`: dedupe contract shared with Wikidata flows

### Established Patterns
- Overlap declaration in content is a **wiki-link list item** under `aliases`, not a plain string (Pitfall 5)
- Plain-alias helper is the wrong *call site* for overlap intent even if it can store `[[…]]` text when given one
- Structure phases ship util + unit tests with zero user-visible CTA change
- No new libraries; compose in-repo utils

### Integration Points
- Phase 11 will call this helper then `storedApi.updateTextField(..., "edit content", …)` from a resolve-dialog row
- Phase 11 must not emit `retry` or alter this answer’s ACCIDENTAL_MATCH / schedule (ADR 0003 / AMR-09)
- This phase stops at pure function + Vitest green

</code_context>

<specifics>
## Specific Ideas

- Research locked: Structure util before dialog “Add as overlapped note” so the dialog SFC stays thin.
- Pitfall 5 is the primary decision driver: named overlap helper that always builds wiki-link tokens.
- Roadmap: existing accidental-match + OVERLAP try-again flows must stay observably unchanged.

</specifics>

<deferred>
## Deferred Ideas

- Add as overlapped note (persist + no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 10-Overlap alias append util*
*Context gathered: 2026-08-05*
