# Phase 12: Title navigate, reopen, E2E polish - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research Pitfall 6 + Phases 8–11)

<domain>
## Phase Boundary

After navigating away via a matched title from the resolve dialog, the user can return to the accidental-match result, open the resolve dialog again, and see the same matches. Targeted E2E covers resolve open/dismiss, multi-match identity where fixtures support it, and reopen-after-navigate (capability-named). Existing `overlap_try_again` coverage stays green.

**In scope:** AMR-05 (title navigate → return → reopen resolve with same matches) + E2E polish for the resolve UX checklist.
**Out of this phase:** New resolve capabilities (AMR-10..13 quiet-state / keyboard polish — v2); SEED-001; OpenAPI/backend enrichment of `matchedNotes` unless research proves previouslyAnswered/session cannot restore the list; auto-reopen of the dialog without user CTA click; SRS / grading changes.

</domain>

<decisions>
## Implementation Decisions

### Reopen affordance (Pitfall 6)
- **D-01:** Allow title navigation to leave recall (existing `NoteTitleWithLink` → `noteShowLocation`). Do **not** `preventDefault` or otherwise block title clicks to keep the dialog mounted. Modal closing on route change is expected. — **Reversibility:** reversible
- **D-02:** Minimum bar is **manual reopen**: after return to the accidental-match result, the same **Resolve accidental match** CTA must remain available; user opens the dialog again and sees the same match titles (and paths once hydrated). Do **not** auto-reopen the dialog via global store/query unless a later polish phase explicitly specs it + E2E. — **Reversibility:** reversible

### Return path and match-list persistence
- **D-03:** Canonical return for product + E2E is **history back** (browser back / equivalent) to the accidental-match result under recall — not a new “Back to result” chrome on note show, and not requiring Recently Recalled as the only path. — **Reversibility:** reversible
- **D-04:** Prefer restoring matches from the existing answered-question payload (`answeredQuestion.matchedNotes` on the in-session `previousAnsweredQuestions` list and/or `RecallsController.previouslyAnswered`). Do **not** add OpenAPI/backend match-list enrichment in this phase unless plan-time research proves the remount path drops matches and no client-side fix restores them. — **Reversibility:** costly — API enrichment would widen Answer DTOs / history without a proven need
- **D-05:** If remount clears the live answered cursor, restore enough recall state so the accidental-match result (alert + Resolve CTA + `matchedNotes`) is visible again after return — researcher/planner choose the smallest seam (e.g. keep-alive, cursor restore, or previouslyAnswered fidelity). Do not invent a dedicated “resolve session” store for auto-open state. — **Reversibility:** reversible

### E2E polish and coverage shape
- **D-06:** Add a capability-named E2E scenario for reopen-after-title-navigate: open Resolve → click matched title → leave → return → Resolve CTA → dialog lists the same match(es). Prefer page-object helpers over rewriting existing Gherkin step text where possible. — **Reversibility:** reversible
- **D-07:** Treat open/dismiss and multi-match path identity as **must stay green** (extend asserts/page objects if gaps remain); do not rewrite the whole accidental-match feature file. Keep `overlap_try_again` uncoupled and green. — **Reversibility:** reversible
- **D-08:** Wave 1 — Vitest at the recall / answered-spelling boundary for remount-or-return seams that prove CTA + same `matchedNotes` after simulated leave/return (only if a client fix is needed). Wave 2 — targeted E2E round-trip. Skip Vitest-only wave if research shows pure E2E + tiny client fix is enough — planner decides after research. — **Reversibility:** reversible

### Claude's Discretion
- Exact E2E return helper (`cy.go('back')` vs navigate to `/recall` then show last answered) as long as D-03’s history-back intent holds.
- Whether multi-match reopen uses a new fixture notebook pair or reuses Phase 8 two-match fixtures.
- Whether any keep-alive / cursor restore lives on `RecallPage` vs a thinner composable — prefer smallest change that makes AMR-05 true.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2: after title navigate, return and reopen resolve
- `.planning/REQUIREMENTS.md` — AMR-05 (this phase; only remaining v1.2 AMR)
- `.planning/ROADMAP.md` — Phase 12 Behavior success criteria 1–3
- `.planning/STATE.md` — Phase 12 ready; blocker note: answer remount/session may need plan-time research

### Prior phase context
- `.planning/phases/08-match-path-and-clickable-titles/08-CONTEXT.md` — D-06/D-07: titles navigate; AMR-05 reopen deferred here
- `.planning/phases/11-add-as-overlapped-note/11-CONTEXT.md` — mutating CTAs complete; Phase 12 owns reopen E2E
- `.planning/phases/09-build-a-link-from-resolve-dialog/09-CONTEXT.md` — stay-on-result patterns; single Modal
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-CONTEXT.md` — Resolve CTA + Modal dismiss

### Research (v1.2) — critical for this phase
- `.planning/research/SUMMARY.md` — Phase 5 navigate+reopen; manual CTA reopen preferred; remount research flag
- `.planning/research/ARCHITECTURE.md` — history reopen; avoid API enrichment unless required
- `.planning/research/STACK.md` — vue-router + existing answered-question state; Modal closes on route change
- `.planning/research/PITFALLS.md` — **Pitfall 6** (dialog state dies on title navigate)
- `.planning/research/FEATURES.md` — reopen resolve after title navigation locked

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — UI-only; no ACCIDENTAL_MATCH / OVERLAP / SRS change
- `.cursor/rules/planning.mdc` — Behavior phase; one observable behavior; stop-safe; targeted E2E
- `.cursor/rules/unit-testing.mdc` — small-test style
- `.cursor/rules/frontend-testing.mdc` — Vitest browser / capability-named tests

### Existing implementation to change / reuse
- `frontend/src/components/notes/NoteTitleWithLink.vue` — title → `noteShowLocation` (keep navigable)
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` / `AccidentalMatchResolveRow.vue` — list + title links
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — hosts Resolve CTA on ACCIDENTAL_MATCH result
- `frontend/src/pages/RecallPage.vue` — `previousAnsweredQuestions` / cursor; remount on leave recall
- `frontend/src/components/commons/Popups/` (Modal) — closes on `route.fullPath` change
- `backend/.../RecallsController` / `previouslyAnswered` — history reload path; verify `matchedNotes` fidelity if remount uses it
- `e2e_test/features/recall/accidental_match_reveal.feature` — extend with reopen scenario
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — Resolve open/dismiss helpers; add navigate/reopen helpers
- `e2e_test/features/recall/overlap_try_again.feature` — must stay green / uncoupled

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `NoteTitleWithLink` + `noteShowLocation`: real router navigation from resolve rows
- `resolve-accidental-match` CTA + `accidental-match-resolve-dialog`: reopen entry
- `RecallPage` `previousAnsweredQuestions` + cursor: in-session answered spelling results including `matchedNotes` when answered live
- `AnsweredQuestionPage` page object: open Resolve, dismiss, assert match title/path

### Established Patterns
- Modal closes on route leave — dialog need not survive navigation
- Pitfall 6: manual CTA reopen is the product minimum; auto-reopen optional later
- Capability-named E2E; page-object preference over Gherkin churn; overlap suite uncoupled
- Wave Vitest → E2E used through Phases 7–11

### Integration Points
- Title click leaves `/recall` → return must re-show accidental-match result with Resolve CTA + same matches
- Plan-time research should confirm whether `previouslyAnswered` returns full `matchedNotes` or only first `matchedNoteId` on Answer
- No nested PopButton / no outcome chrome coupling while polishing E2E

</code_context>

<specifics>
## Specific Ideas

- Research lock: “reopen capability” ≠ auto-open dialog; prefer manual Resolve CTA.
- STATE blocker carried forward: answer remount/session on title navigate needs plan-time research before locking implementation tasks.
- Phase 8 already asserted clickable titles without round-trip; this phase owns the round-trip.

</specifics>

<deferred>
## Deferred Ideas

- AMR-10..13 resolve polish (quiet already-linked/overlapped, keyboard, etc.) — v2
- SEED-001 MCQ / fuzzy / `Notebook:Title` — parked seed
- Auto-reopen dialog via session/query — only if a later phase specs it + E2E
- OpenAPI enrichment of match path/history — only if client remount cannot restore matches

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 12-Title navigate, reopen, E2E polish*
*Context gathered: 2026-08-05*
