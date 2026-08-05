# Phase 9: Build a link from resolve dialog - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research + Phases 7–8)

<domain>
## Phase Boundary

From a resolve-dialog match row, the user can **Build a link** to that matched note using the existing property/relationship offer as a **single Modal step** (not a nested PopButton), remain on the accidental-match result afterward, and see Build-a-link unavailable when the reviewed notebook is readonly or required note data is not loaded.

**In scope:** AMR-06, AMR-07 (gates for Build a link now; Add-as-overlapped CTA itself is Phase 11, but the same gate rules apply when that CTA arrives).
**Out of this phase:** Overlap alias append util (Phase 10); Add as overlapped note / no try-again / no credit reclaim (Phase 11); title navigate reopen polish and AMR-05 E2E (Phase 12).

</domain>

<decisions>
## Implementation Decisions

### Single-Modal link offer host
- **D-01:** Never nest a second `PopButton`/`Modal` around `MatchedNoteLinkOffer` inside the resolve dialog. Host Build a link as a **step swap** in the same Modal opened by **Resolve accidental match**. — **Reversibility:** costly — nested modals reintroduce Pitfall 3 (focus/close fights) already documented for this milestone
- **D-02:** Own the list ↔ offer step state in the resolve Modal content (prefer extending `AccidentalMatchResolveDialog` — or a thin sibling host under `recall/` — so `AnsweredSpellingQuestion` stays CTA + `PopButton` only). Pass `reviewedNoteId` from the answered recalled note into the dialog host. — **Reversibility:** reversible
- **D-03:** Per-row **Build a link** CTA on `AccidentalMatchResolveRow` (capability copy locked). Clicking it switches the Modal body to `MatchedNoteLinkOffer` with that row’s `matchedNoteId`. Do **not** add **Add as overlapped note** in this phase. — **Reversibility:** reversible

### Stay-on-result and offer exit semantics
- **D-04:** Keep `MatchedNoteLinkOffer` stay-on-result semantics (`navigate-on-success=false`; property/relationship writes do not leave recall). After a successful link or offer go-back/`closeDialog`, **return to the match list inside the same resolve Modal** (do not force-dismiss the outer resolve dialog). User dismisses resolve via existing Modal closer only. — **Reversibility:** reversible
- **D-05:** Reuse `MatchedNoteLinkOffer` as-is for the offer pipeline (`LinkInsertionChoice` → property append or `AddRelationshipFinalize`). Prefer adapting the host/step wiring over rewriting the offer. Preserve existing `link-to-matched-note-*` (or equivalent capability) testids so E2E page objects stay callable. — **Reversibility:** reversible

### Readonly / unload gates (AMR-07)
- **D-06:** Port v1.1-style gates: omit **Build a link** when the reviewed notebook is `readonly` **or** when reviewed and/or matched realms required for the offer are not yet loaded. Prefer **hide** (not disabled-looking) to match prior accidental-match link CTAs. Titles + path remain visible when topology is available. — **Reversibility:** reversible
- **D-07:** Hydrate reviewed + matched realms as needed for the gate and offer (existing `getNoteRealmRefAndLoadWhenNeeded`). Do not invent a new capability API or OpenAPI change for gating. — **Reversibility:** reversible

### Test coverage for this phase
- **D-08:** Extend Vitest at the answered-spelling / resolve-dialog boundary: Build a link appears when realms are writable+loaded; omitted when reviewed notebook readonly or realms unloaded; opening offer is a step in the same Modal (no nested dialog); after link success user still on accidental-match result chrome. Prefer capability-named tests; reuse `makeMe` / existing accidental-match fixtures. — **Reversibility:** reversible
- **D-09:** Untag `@wip` on the two accidental-match link E2E scenarios once green. Prefer **page-object** updates (open Resolve → Build a link → existing link helpers) over rewriting Gherkin step text. Keep `overlap_try_again` uncoupled. — **Reversibility:** reversible

### Claude's Discretion
- Exact step-state shape (`ref`/`enum` for `list` vs `offer`) and whether go-back chrome lives in the dialog host or relies solely on `MatchedNoteLinkOffer`’s existing go-back.
- Whether reviewed-realm hydrate for AMR-07 lives on the dialog host vs each row (prefer one reviewed hydrate at host level to avoid N× duplicate loads).
- Visual density of the per-row Build a link button (DaisyUI size/classes consistent with resolve chrome).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2 goal: optional resolve dialog; per match Build a link
- `.planning/REQUIREMENTS.md` — AMR-06, AMR-07 (this phase); AMR-08+ deferred
- `.planning/ROADMAP.md` — Phase 9 success criteria and boundaries
- `.planning/STATE.md` — Phase 7–8 decisions; AMR-07 lives with first mutating action

### Prior phase context
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-CONTEXT.md` — keep `MatchedNoteLinkOffer` unused until Phase 9; never nest PopButton; `@wip` link E2E until restore
- `.planning/phases/08-match-path-and-clickable-titles/08-CONTEXT.md` — `AccidentalMatchResolveRow` hydrate + title/path; sets up per-row actions without nested PopButton

### Research (v1.2)
- `.planning/research/SUMMARY.md` — Phase 3 = Build a link as in-dialog step; stay-on-result; readonly gates
- `.planning/research/ARCHITECTURE.md` — single Modal stepped content; anti-pattern nested PopButton
- `.planning/research/STACK.md` — reuse `MatchedNoteLinkOffer`; zero new libraries
- `.planning/research/PITFALLS.md` — Pitfall 3 (nested PopButton); Pitfall 7 (readonly/unload gates)
- `.planning/research/FEATURES.md` — locked verb **Build a link**; stay-on-page after link

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — UI-only; do not change ACCIDENTAL_MATCH / OVERLAP grading or SRS
- `.cursor/rules/planning.mdc` — Behavior phase; one observable behavior; stop-safe
- `.cursor/rules/unit-testing.mdc` — small-test style for Vitest
- `.cursor/rules/frontend-testing.mdc` — Vitest browser, `data-testid`, `makeMe`

### Existing implementation to change / reuse
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — hosts Resolve `PopButton` / dialog; pass reviewed note id
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — list host; add step state + offer mount
- `frontend/src/components/recall/AccidentalMatchResolveRow.vue` — add gated Build a link CTA
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — reuse offer pipeline (`navigate-on-success=false`, `closeDialog`)
- `frontend/src/components/commons/Popups/PopButton.vue` — single Modal host (`#default="{ closer }"`)
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — extend for link + gates
- `frontend/tests/components/recall/MatchedNoteLinkOffer.spec.ts` — keep; drive via dialog if needed
- `e2e_test/features/recall/accidental_match_reveal.feature` — untag `@wip` link scenarios when green
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — open resolve then `link-to-matched-note-*`
- `e2e_test/step_definitions/recall.ts` — existing link steps; prefer page-object path

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `MatchedNoteLinkOffer`: property/relationship offer with `reviewedNoteId` + `matchedNoteId`; emits `closeDialog`; relationship uses `navigate-on-success=false`
- `AccidentalMatchResolveDialog` + `AccidentalMatchResolveRow`: list + path/title; row already hydrates match realm
- `PopButton` → `Modal`: one dialog from Resolve CTA; slot exposes `closer`
- E2E: `@wip` wiki-property and relationship scenarios already encode stay-on-result; page object still targets `link-to-matched-note-*`

### Established Patterns
- Phase 7–8: never nest PopButton inside resolve Modal — step swap only
- `NoteTopology` remains id/title; realms via `getNoteRealmRefAndLoadWhenNeeded`
- Outcome-gated ACCIDENTAL_MATCH chrome; OVERLAP try-again untouched

### Integration Points
- Pass reviewed note id into resolve dialog host; per-row Build a link → step to offer
- Map offer `closeDialog` to “back to list” (not necessarily Modal dismiss)
- Vitest: readonly + unloaded-realm gate cases; E2E: Resolve open then link helpers

</code_context>

<specifics>
## Specific Ideas

- Locked CTA copy: **Build a link** (FEATURES / REQUIREMENTS).
- Research locked preference: single Modal step over nested PopButton (Pitfall 3).
- Temporary Phase 7 loss of link-from-result is restored here; `@wip` E2E scenarios are the acceptance trail.

</specifics>

<deferred>
## Deferred Ideas

- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note (no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09); AMR-07 gate rules already cover that CTA when added
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 9-Build a link from resolve dialog*
*Context gathered: 2026-08-05*
