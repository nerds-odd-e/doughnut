# Phase 8: Match path and clickable titles - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research + Phase 7)

<domain>
## Phase Boundary

Each resolve-dialog match is identifiable by notebook path and reachable by title: every match row shows a notebook path/breadcrumb (identity only — no note body / peek), each match title is clickable (navigates toward that note), and the dialog still lists all current matches with title + path together.

**In scope:** AMR-04 — path/breadcrumb + clickable titles in `AccidentalMatchResolveDialog`.
**Out of this phase:** Build a link / readonly unload gates (Phase 9); overlap alias util / Add as overlapped (Phases 10–11); reopen-after-title-navigate polish and full E2E of AMR-05 (Phase 12).

</domain>

<decisions>
## Implementation Decisions

### Path data source
- **D-01:** Keep `answeredQuestion.matchedNotes` as `NoteTopology[]` (id + title). Do **not** widen `NoteTopology` or enrich the grade response for path. — **Reversibility:** reversible
- **D-02:** Load each match’s `NoteRealm` with existing `getNoteRealmRefAndLoadWhenNeeded(matched.id)` (same seam as `MatchedNoteLinkOffer`). Render path from `notebookRealm` + `ancestorFolders`. — **Reversibility:** reversible

### Path chrome and row layout
- **D-03:** Per row: clickable **title first**, notebook **breadcrumb under** it (title = primary identity; path = disambiguator). Never mount `NoteShow` / note body in the dialog. — **Reversibility:** reversible
- **D-04:** Use existing `BreadcrumbWithCircle` once the match realm is available (`notebookRealm` + `ancestorFolders`). Do not add a new path/breadcrumb package or a plain-string-only notebook label. — **Reversibility:** reversible
- **D-05:** Extract a thin per-match row component under `frontend/src/components/recall/` that owns realm hydrate + title + breadcrumb (dialog stays the list host). Sets up Phase 9 per-row actions without nested `PopButton`. — **Reversibility:** reversible

### Title navigation boundary
- **D-06:** Make each match title a `NoteTitleWithLink` (`noteShowLocation`) so click navigates to that note. Allow leaving recall; existing Modal route-change close applies. — **Reversibility:** reversible
- **D-07:** Do **not** implement AMR-05 reopen-after-return guarantees in this phase. Minimum bar: Resolve CTA remains available if the answered result is still mounted when the user returns. Full reopen / remount polish is Phase 12. — **Reversibility:** reversible

### Hydrate timing and loading UX
- **D-08:** Start realm hydrate when the dialog body mounts (on open), not before the CTA click and not blocked on all paths before show. Title is visible/clickable immediately from `NoteTopology`; breadcrumb appears when that row’s realm arrives (brief empty/missing path until then is OK). — **Reversibility:** reversible
- **D-09:** No skeleton/spinner requirement beyond existing store-load behavior; do not block the whole list on the slowest match. — **Reversibility:** reversible

### Test coverage for this phase
- **D-10:** Extend Vitest around the resolve dialog / answered spelling accidental-match boundary: each row shows title + path once realm fixtures are present; titles are links toward the match note; still no note body / peek. Prefer capability-named tests; use `makeMe` realm fixtures. — **Reversibility:** reversible
- **D-11:** Update accidental-match E2E (or page object) so dialog rows assert path identity + clickable title where fixtures already provide distinct notebooks/paths. Do not expand this phase into full AMR-05 navigate-away-and-reopen E2E (Phase 12). Keep overlap E2E uncoupled. — **Reversibility:** reversible

### Claude's Discretion
- Exact row component name and `data-testid`s (prefer capability names like `resolve-match-row-*`, path/breadcrumb testids).
- Visual density / DaisyUI breadcrumb classes inside the dialog list.
- Whether breadcrumb folder segments are clickable (follow `BreadcrumbWithCircle` defaults) vs display-only — prefer existing component behavior.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2 goal: optional resolve dialog; reviewed note primary
- `.planning/REQUIREMENTS.md` — AMR-04 (this phase); AMR-05 deferred to Phase 12; Out of Scope (no peek / no stacked bodies)
- `.planning/ROADMAP.md` — Phase 8 success criteria and phase boundaries
- `.planning/STATE.md` — current position; Phase 7 decisions carried forward

### Prior phase context
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-CONTEXT.md` — dialog shell, titles-only interim, deferred path/click to Phase 8
- `.planning/phases/07-compact-result-resolve-dialog-shell/07-02-SUMMARY.md` — E2E CTA/dialog testids and page-object patterns

### Research (v1.2)
- `.planning/research/SUMMARY.md` — client realm hydrate preferred over API enrichment; Phase 2 = path + clickable titles
- `.planning/research/ARCHITECTURE.md` — Pattern 2: `getNoteRealmRefAndLoadWhenNeeded` + `Breadcrumb` / `NoteTitleWithLink`
- `.planning/research/STACK.md` — `BreadcrumbWithCircle` / `Breadcrumb` / `BasicBreadcrumb`; zero new libraries
- `.planning/research/PITFALLS.md` — Pitfall 6 (dialog state on title navigate → Phase 12); avoid mounting `NoteShow`; load on dialog open
- `.planning/research/FEATURES.md` — clickable title + notebook breadcrumb/path; no body peek

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — UI-only; do not change ACCIDENTAL_MATCH / OVERLAP grading or SRS
- `.cursor/rules/planning.mdc` — Behavior phase; one observable behavior; stop-safe
- `.cursor/rules/unit-testing.mdc` — small-test style for Vitest
- `.cursor/rules/frontend-testing.mdc` — Vitest browser, `data-testid`, `makeMe`

### Existing implementation to change / reuse
- `frontend/src/components/recall/AccidentalMatchResolveDialog.vue` — current titles-only list
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — hosts resolve `PopButton` / dialog
- `frontend/src/components/notes/NoteTitleWithLink.vue` — clickable title → `noteShowLocation`
- `frontend/src/components/toolbars/BreadcrumbWithCircle.vue` — notebook + folder path chrome
- `frontend/src/store/StoredApiCollection.ts` — `getNoteRealmRefAndLoadWhenNeeded`
- `frontend/src/components/recall/MatchedNoteLinkOffer.vue` — reference hydrate pattern (unused in UI until Phase 9)
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — extend for path + link
- `e2e_test/features/recall/accidental_match_reveal.feature` — extend path/title asserts carefully
- `e2e_test/start/pageObjects/AnsweredQuestionPage.ts` — resolve dialog page-object helpers

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AccidentalMatchResolveDialog`: list host with `resolve-match-row-*` / `accidental-match-resolve-dialog` testids
- `NoteTitleWithLink` + `noteShowLocation`: title navigation without `NoteShow`
- `BreadcrumbWithCircle`: notebook name + circle/bazaar + `ancestorFolders`
- `getNoteRealmRefAndLoadWhenNeeded`: async path source; already used by `MatchedNoteLinkOffer`
- Test support: `accidentalMatchWithTwoMatchedNotes` / mount helpers in `answeredSpellingQuestionTestSupport.ts`

### Established Patterns
- `NoteTopology` is intentionally id/title only — path lives on `NoteRealm`
- Modal closes on route change — title navigation will dismiss the open dialog (expected)
- Phase 7 kept link offers out of the dialog; Phase 9 adds actions as single-Modal steps (never nested `PopButton`)

### Integration Points
- Enhance dialog list rows (new thin row component) to hydrate + render title link + breadcrumb
- Vitest: provide note-realm fixtures for matched ids so path asserts are deterministic
- E2E: assert path identity in dialog without requiring full navigate-and-reopen coverage yet

</code_context>

<specifics>
## Specific Ideas

- Research locked preference: client hydrate over OpenAPI enrichment unless history/offline path fidelity becomes a product requirement (it is not for Phase 8).
- Obsidian-style disambiguation: same title distinguished by path — title clickable, path visible, no peek.
- Phase 7 dialog already lists titles; this phase only adds path + clickability on those rows.

</specifics>

<deferred>
## Deferred Ideas

- Build a link / readonly unload gates — Phase 9 (AMR-06, AMR-07)
- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed
- API enrichment of `matchedNotes` with path — only if later product needs history path without N× `showNote`

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 8-Match path and clickable titles*
*Context gathered: 2026-08-05*
