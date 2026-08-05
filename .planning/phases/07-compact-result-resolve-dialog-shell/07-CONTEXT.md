# Phase 7: Compact result + Resolve dialog shell - Context

**Gathered:** 2026-08-05
**Status:** Ready for planning
**Mode:** `--auto` (recommended defaults from roadmap + v1.2 research)

<domain>
## Phase Boundary

On an accidental-match spelling result, drop stacked matched-note `NoteShow` bodies so the reviewed note stays primary; when matches exist, show a **Resolve accidental match** CTA under the alert that opens a dismissible dialog listing match titles only (no note body). OVERLAP try-again chrome stays outcome-gated and unchanged.

**In scope:** AMR-01, AMR-02, AMR-03 — compact result + optional resolve dialog shell.
**Out of this phase:** notebook path/breadcrumb and clickable titles (Phase 8); Build a link (Phase 9); overlap alias util / Add as overlapped (Phases 10–11); reopen-after-navigate E2E polish (Phase 12).

</domain>

<decisions>
## Implementation Decisions

### Dialog host and component shape
- **D-01:** Open the resolve UI with in-repo `PopButton` → `Modal` (native `<dialog showModal>`), CTA label **Resolve accidental match**, gated to `ACCIDENTAL_MATCH` with non-empty `matchedNotes`. — **Reversibility:** reversible
- **D-02:** Place the CTA immediately under the accidental-match alert (before `NoteUnderQuestion` / reviewed `NoteShow`). — **Reversibility:** reversible
- **D-03:** Extract match-list body into a new `AccidentalMatchResolveDialog` under `frontend/src/components/recall/` (keep chrome/CTA ownership in `AnsweredSpellingQuestion`). — **Reversibility:** reversible
- **D-04:** Reuse existing Modal dismiss behavior (close button, backdrop/`close_request`, modal stack ESC, route-change close). Do not add a new modal library.

### Phase 7 dialog list contents
- **D-05:** Dialog lists match **titles only** from `answeredQuestion.matchedNotes` (`NoteTopology` title/id). No note body, no notebook path/breadcrumb, no clickable title navigation, no per-row actions in this phase. — **Reversibility:** reversible
- **D-06:** Present multiple matches as a simple vertical list (one title per row). Multi-match progress cues are v2 (AMR-12).

### Interim link-offer removal
- **D-07:** Remove the stacked matched-notes section **including** per-match `MatchedNoteLinkOffer` / `link-to-matched-note-*` CTAs in this phase. Build-a-link returns in Phase 9 as an in-dialog step (single Modal; never nested `PopButton`). Temporary loss of link-from-result until Phase 9 is an accepted stop-safe trade for compact reviewed-note focus. — **Reversibility:** costly — undoing without Phase 9 would reintroduce stacked bodies or a parallel link surface

### Test coverage for this phase
- **D-08:** Rewrite `AnsweredSpellingQuestionAccidentalMatch` unit tests for: no matched `NoteShow`s / no `matched-notes-section`; CTA present when matches exist; opening dialog shows titles; omit CTA when `matchedNotes` empty; OVERLAP scenarios unchanged (no resolve CTA). — **Reversibility:** reversible
- **D-09:** Update E2E reveal coverage so accidental-match scenarios assert CTA + dialog titles instead of stacked reveal. Tag current link-from-result E2E scenarios `@wip` until Phase 9 restores Build a link. Keep `overlap_try_again` green and uncoupled. Capability-named specs only (no phase numbers in product tests). — **Reversibility:** reversible

### Claude's Discretion
- Exact `data-testid` names for CTA/dialog/rows (prefer capability names like `resolve-accidental-match`, resolve-dialog container, per-title rows).
- Visual density of the title list (DaisyUI/Tailwind classes) within existing recall chrome.
- Whether Phase 7 dialog is a pure presentational list component or receives `closer` from `PopButton` slot (follow existing `PopButton` `#default="{ closer }"` pattern).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Milestone scope
- `.planning/PROJECT.md` — v1.2 goal: optional resolve dialog; reviewed note primary
- `.planning/REQUIREMENTS.md` — AMR-01..03 (this phase); AMR-04+ deferred; Out of Scope (no stacked bodies, no peek, optional resolve)
- `.planning/ROADMAP.md` — Phase 7 success criteria and phase boundaries
- `.planning/STATE.md` — current position; deferred SEED-001 / AMR-10..13

### Research (v1.2)
- `.planning/research/SUMMARY.md` — reuse-only stack; Phase 1 = shell + drop stacks
- `.planning/research/ARCHITECTURE.md` — single Modal; `AccidentalMatchResolveDialog`; drop stacks with CTA
- `.planning/research/STACK.md` — `PopButton`/`Modal`; no third-party dialog libs
- `.planning/research/PITFALLS.md` — Pitfall 1 (reveal lost), Pitfall 2 (weak/buried CTA), Pitfall 8 (OVERLAP coupling)
- `.planning/research/FEATURES.md` — locked CTA copy **Resolve accidental match**

### ADRs / product constraints
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md` — do not change ACCIDENTAL_MATCH / OVERLAP grading or SRS in this UI-only phase
- `.cursor/rules/planning.mdc` — Behavior phase; one observable behavior; stop-safe
- `.cursor/rules/unit-testing.mdc` — small-test style for Vitest updates
- `.cursor/rules/frontend-testing.mdc` — Vitest browser, `data-testid`, `makeMe`

### Existing implementation to change
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — stacked matches + link offers live here today
- `frontend/src/components/commons/Popups/PopButton.vue` — CTA → Modal host
- `frontend/src/components/commons/Modal.vue` — dismiss / route-close / modal stack
- `frontend/tests/components/recall/AnsweredSpellingQuestionAccidentalMatch.spec.ts` — current stacked-NoteShow asserts
- `e2e_test/features/recall/accidental_match_reveal.feature` — stacked reveal + link scenarios
- `e2e_test/features/recall/overlap_try_again.feature` — must stay green

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `PopButton` + `Modal`: standard optional dialog host (Teleport, ESC, route-close)
- `answeredQuestion.matchedNotes`: `NoteTopology[]` with id + title — enough for Phase 7 title list
- `showMatchedNotesSection` gate: same outcome + length check becomes Resolve CTA gate
- `MatchedNoteLinkOffer`: keep for Phase 9 reuse; unused in Phase 7 UI after stack removal
- Test support: `accidentalMatchWithTwoMatchedNotes` / `mountAnsweredSpellingQuestion` in `answeredSpellingQuestionTestSupport.ts`

### Established Patterns
- Outcome-discriminated chrome: `ACCIDENTAL_MATCH` vs `OVERLAP` vs correct/incorrect alerts
- Link offers currently nest inside per-match `PopButton` under stacked `NoteShow` — must not nest that pattern inside the resolve Modal later (Phase 9 steps into offer)
- E2E page object `AnsweredQuestionPage` asserts `matched-notes-section` / `accidental-match-alert` — update selectors with the new CTA/dialog

### Integration Points
- Modify `AnsweredSpellingQuestion.vue` template: remove matched-notes `<section>`; add Resolve `PopButton` under alert
- New recall component for dialog body list
- Frontend unit tests + accidental-match E2E; leave overlap E2E alone aside from asserting no accidental-match chrome bleed

</code_context>

<specifics>
## Specific Ideas

- Locked CTA copy: **Resolve accidental match** (REQUIREMENTS / FEATURES).
- Dialog is optional and dismissible anytime — no forced resolve before continuing recall.
- Architecture sketch uses `AccidentalMatchResolveDialog` (+ optional row extract later when actions arrive).

</specifics>

<deferred>
## Deferred Ideas

- Notebook path/breadcrumb + clickable titles — Phase 8 (AMR-04)
- Build a link / readonly unload gates — Phase 9 (AMR-06, AMR-07)
- Overlap alias append util — Phase 10 (Structure)
- Add as overlapped note (no try-again / no reclaim) — Phase 11 (AMR-08, AMR-09)
- Title navigate, reopen resolve, full E2E polish — Phase 12 (AMR-05)
- AMR-10..13 resolve polish and SEED-001 — v2 / parked seed

None — discussion stayed within phase scope (auto mode)

</deferred>

---

*Phase: 7-Compact result + Resolve dialog shell*
*Context gathered: 2026-08-05*
