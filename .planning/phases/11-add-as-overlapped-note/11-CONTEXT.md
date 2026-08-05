# Phase 11: Add as overlapped note - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research Pitfalls 4–5 + Phases 9–10)

<domain>
## Phase Boundary

From a resolve-dialog match row, the user can **Add as overlapped note**, which declares an overlap wiki-link alias on the **reviewed** note toward that match. After that action, the current accidental-match result does **not** show try-again and does **not** reclaim SRS credit; answer outcome stays `ACCIDENTAL_MATCH` and this answer’s schedule is unchanged.

**In scope:** AMR-08, AMR-09 (AMR-07 gate rules already decided in Phase 9 — apply them to this CTA).
**Out of this phase:** Title navigate / reopen resolve / full E2E polish for AMR-05 (Phase 12); AMR-10..13 quiet-state / keyboard polish (v2); SEED-001; any `AnswerOutcome` / SRS / backend grading change.

</domain>

<decisions>
## Implementation Decisions

### Per-row CTA and persist path
- **D-01:** Add a per-row **Add as overlapped note** CTA on `AccidentalMatchResolveRow` beside **Build a link** (locked capability copy). Clicking it declares overlap for that row’s match — do **not** introduce a nested Modal/PopButton. — **Reversibility:** reversible
- **D-02:** Persist by composing Phase 10’s `appendOverlapWikiLinkToNoteContent` on the **reviewed** note’s content, then `storedApi.updateTextField(reviewedNoteId, "edit content", …)` when the helper returns a non-null string. Never call `appendAliasToNoteContent` with a plain title for this action (Pitfall 5). — **Reversibility:** reversible
- **D-03:** Host the mutate handler in the resolve dialog host (`AccidentalMatchResolveDialog` or thin sibling under `recall/`), not by branching `AnsweredSpellingQuestion` outcome chrome. Keep ACCIDENTAL_MATCH vs OVERLAP templates outcome-discriminated (Pitfall 8). — **Reversibility:** reversible

### Post-success dialog / result behavior
- **D-04:** After a successful declare, remain on the accidental-match result (alert + Resolve CTA). Prefer **return to the match list inside the same resolve Modal** (multi-match friendly; mirrors Phase 9 Build-a-link stay-in-dialog). Do **not** force-dismiss unless a later polish phase requires it. — **Reversibility:** reversible
- **D-05:** If `appendOverlapWikiLinkToNoteContent` returns `null` (duplicate / unparseable / bad aliases shape), do **not** call `updateTextField`; stay on the list with no outcome/UI flip. Quiet “already overlapped” chrome is AMR-10 (deferred). — **Reversibility:** reversible

### AMR-09 — no try-again / no SRS reclaim (Pitfall 4)
- **D-06:** Content mutation only. Do **not** emit `retry`, do **not** re-submit the spelling answer, do **not** change `answer.outcome`, and do **not** offer credit reclaim / undo of the ACCIDENTAL_MATCH schedule for this answer. OVERLAP try-again remains exclusive to graded `outcome === "OVERLAP"`. — **Reversibility:** costly — coupling declare → retry would violate ADR 0003 and corrupt the session narrative
- **D-07:** Assert after declare: no `overlap-try-again` / `overlap-try-again-alert`; accidental-match chrome still present; memory-tracker schedule for this answer unchanged from the accidental-match grade. — **Reversibility:** reversible

### AMR-07 gates (reuse Phase 9)
- **D-08:** Hide **Add as overlapped note** under the same conditions as **Build a link**: reviewed notebook `readonly` **or** reviewed/matched realms required for the write not yet loaded. Prefer **hide** (not disabled). Titles + path remain visible. — **Reversibility:** reversible
- **D-09:** Prefer one shared gate helper (e.g. rename/generalize `canOfferBuildLink` → mutating-action gate) so both CTAs stay in lockstep; hydrate reviewed realm once at dialog host level. — **Reversibility:** reversible

### Test coverage for this phase
- **D-10:** Wave 1 — Vitest at the answered-spelling / resolve-dialog boundary: CTA visible when writable+loaded; omitted when readonly/unloaded; click runs append+`updateTextField` with a wiki-link token (not plain alias); after success still ACCIDENTAL_MATCH chrome and **no** overlap try-again. Prefer capability-named specs; reuse `makeMe` accidental-match fixtures. — **Reversibility:** reversible
- **D-11:** Wave 2 — Targeted E2E for Add as overlapped (capability-named; no phase numbers): open Resolve → Add as overlapped → still on accidental-match result; no try-again. Keep `overlap_try_again` green and uncoupled. Prefer page-object updates over rewriting Gherkin where possible. Full reopen-after-navigate polish stays Phase 12. — **Reversibility:** reversible

### Claude's Discretion
- Exact button classes / layout density next to Build a link (DaisyUI `btn-sm` consistent with resolve chrome).
- Whether null-append is silent or uses an existing toast pattern — silent is fine for this phase.
- Whether Vitest stubs `updateTextField` or drives StoredApi with clean storage — follow Phase 9 patterns.
- Exact E2E scenario placement (extend `accidental_match_reveal.feature` vs sibling capability feature).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2: per match **Add as overlapped note**; overlap skips try-again / credit reclaim
- `.planning/REQUIREMENTS.md` — AMR-08, AMR-09 (this phase); AMR-07 already Complete with Phase 9
- `.planning/ROADMAP.md` — Phase 11 Behavior success criteria 1–3
- `.planning/STATE.md` — Phase 11 ready; blocker: do not conflate dialog overlap with OVERLAP try-again / ADR 0003

### Prior phase context
- `.planning/phases/10-overlap-alias-append-util/10-CONTEXT.md` — `appendOverlapWikiLinkToNoteContent` shipped; Phase 11 wires CTA + save
- `.planning/phases/09-build-a-link-from-resolve-dialog/09-CONTEXT.md` — single-Modal step; AMR-07 hide gates; stay-on-result patterns
- `.planning/phases/08-match-path-and-clickable-titles/08-CONTEXT.md` — row hydrate / title+path
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-CONTEXT.md` — resolve shell; OVERLAP try-again untouched

### Research (v1.2) — critical for this phase
- `.planning/research/SUMMARY.md` — Add as overlapped = wiki-link frontmatter write; must not re-grade
- `.planning/research/ARCHITECTURE.md` — declare path: append + `updateTextField`; leave ACCIDENTAL_MATCH; try-again only for graded OVERLAP
- `.planning/research/STACK.md` — `appendOverlap`/`appendAlias` + `updateTextField`; zero new libraries; do not emit `retry`
- `.planning/research/PITFALLS.md` — **Pitfall 4** (declare → try-again/reclaim); **Pitfall 5** (plain alias); Pitfall 8 (chrome coupling)
- `.planning/research/FEATURES.md` — locked verb **Add as overlapped note**; no try-again / reclaim on this result

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — ACCIDENTAL_MATCH vs OVERLAP; UI content mutation only; no SRS math change
- `.cursor/rules/planning.mdc` — Behavior phase; one observable behavior; stop-safe
- `.cursor/rules/unit-testing.mdc` — small-test style
- `.cursor/rules/frontend-testing.mdc` — Vitest browser / capability-named tests

### Existing implementation to change / reuse
- `frontend/src/utils/appendOverlapWikiLinkToNoteContent.ts` — Phase 10 util (must use)
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — list/link step host; add mutate handler + gate reuse
- `frontend/src/components/recall/AccidentalMatchResolveRow.vue` — add gated Add as overlapped CTA
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — ACCIDENTAL_MATCH vs OVERLAP chrome; do not wire retry from declare
- `frontend/src/store/StoredApiCollection.ts` — `updateTextField(..., "edit content", …)`
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — reference pattern for content compose + `updateTextField`
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — extend for overlap declare + gates + no try-again
- `frontend/tests/utils/appendOverlapWikiLinkToNoteContent.spec.ts` — util already covered; dialog tests own wiring
- `e2e_test/features/recall/accidental_match_reveal.feature` — extend or sibling for Add as overlapped
- `e2e_test/features/recall/overlap_try_again.feature` — must stay green / uncoupled
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — Resolve → Add as overlapped helpers
- `backend/src/main/java/com/odde/doughnut/algorithms/FrontmatterAliases.java` — overlap wiki-link token contract (read; no Java change expected)

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `appendOverlapWikiLinkToNoteContent`: wiki-link alias append; returns `null` on no-op/invalid
- `AccidentalMatchResolveDialog` + `AccidentalMatchResolveRow`: list host + per-row Build a link + AMR-07 `canOfferBuildLink`
- `storedApi.updateTextField` / Reviewed realm content: same seam as MatchedNoteLinkOffer property write
- Phase 10 Vitest: locks wiki-link token shape at util boundary

### Established Patterns
- Outcome-discriminated UI: resolve UX only for `ACCIDENTAL_MATCH`; try-again only for `OVERLAP`
- Mutating CTAs hide when readonly or realms unloaded (Phase 9)
- Single Modal — never nest PopButton for secondary flows
- Structure util already shipped; this phase is Behavior wiring only

### Integration Points
- Row emit → dialog host: append on reviewed content → `updateTextField` → return to list
- Do not touch answer grading APIs or `emit('retry')`
- Vitest then targeted E2E; Phase 12 owns reopen-after-title-navigate

</code_context>

<specifics>
## Specific Ideas

- Locked CTA copy: **Add as overlapped note** (FEATURES / REQUIREMENTS).
- Highest-risk pitfall for this milestone: Pitfall 4 (declare → try-again / reclaim) — tests must lock the negative.
- Phase 10 already solved Pitfall 5 at the util layer; Phase 11 must call that util, not plain `appendAliasToNoteContent`.

</specifics>

<deferred>
## Deferred Ideas

- Title navigate, reopen resolve, E2E polish — Phase 12 (AMR-05)
- AMR-10 quiet state when already linked/overlapped; AMR-11..13 polish — v2
- SEED-001 MCQ / fuzzy / `Notebook:Title` spelling — parked seed

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 11-Add as overlapped note*
*Context gathered: 2026-08-05*
