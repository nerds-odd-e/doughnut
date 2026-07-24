# Phase 4: Offer link between notes - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning
**Mode:** `--auto` (all gray areas auto-selected; recommended option chosen for each; no interactive prompts)

<domain>
## Phase Boundary

After an **accidental match** (Phases 2–3 already grade `ACCIDENTAL_MATCH`, set `matchedNoteId`, populate `matchedNotes`, and reveal reviewed + matched notes in `AnsweredSpellingQuestion.vue`), let the user **build a link** between the **reviewed note** and a **matched note** through the **existing add-link UI** (`SearchForm` / `LinkInsertionChoice` / `AddRelationshipFinalize`), with the **matched note pre-selected**. The system **never auto-writes** a link — creation is always user-initiated and confirmed.

**In this phase:**
- Offer a clear link CTA on the accidental-match result surface (matched-notes section).
- Open the existing add-link flow with the matched note already selected as the target (skip the search step).
- Support **property link** and **relationship note** paths (AM-04 / PROJECT wording).
- Keep plain-wrong and non-accidental paths unchanged; no backend grading/SRS changes.

**Not in this phase (later phases):**
- Alias-as-wiki-link overlap declaration → Phase 5 (OVL-02, OVL-03).
- Overlap "try again, no credit" → Phase 6 (OVL-01).
- Auto-creating links without user choice — permanently out of scope.

</domain>

<decisions>
## Implementation Decisions

### CTA placement & multi-match targeting
- **D-01:** Place a **"Link to this note"** (or equivalent) control **under each** matched `NoteShow` in the existing `matched-notes-section` — one CTA per `matchedNotes` entry, not a single CTA only for `matchedNoteId`. — **Reversibility:** reversible — UI-only; can thin to primary-only later.
  - Rationale: Phase 3 surfaces **all** matches; AM-04 says "a matched note" with preselection — per-row CTAs let the user pick which confusion to connect without an extra picker.
- **D-02:** Link **direction** is **from the reviewed note → matched note**: source = reviewed (recalled) note, target = the matched note for that CTA. — **Reversibility:** reversible.
  - Rationale: Matches ROADMAP success criterion wording ("connect the reviewed note to a matched note") and mirrors NoteToolbar `SearchForm` where `note` is the source being linked from.

### Preselection entry into add-link UI
- **D-03:** Reuse the existing add-link stack (`SearchForm` + `LinkInsertionChoice` + `AddRelationshipFinalize`). Open it **already past search**: land on `LinkInsertionChoice` with the matched note as `targetNoteTopology` / pre-set `selectedSearchResult`. The user still chooses link type and confirms; **never** auto-submit or auto-create. — **Reversibility:** reversible — additive prop/wrapper around existing dialog; search path for toolbar remains unchanged.
  - Rationale: ROADMAP requires "matched note is pre-selected … with minimal effort" and "existing add-link UI"; skipping search is the preselection mechanic. PROJECT forbids auto-write.
- **D-04:** Obtain any missing fields needed to drive that UI (full source `Note` from reviewed id; `NoteSearchResult`-shaped target from matched `NoteTopology` + notebook metadata) via the **smallest existing fetch/build path** research finds — do **not** expand the `AnsweredQuestion` / `matchedNotes` contract for this phase unless research proves it is required. — **Reversibility:** reversible.
  - Rationale: Phase 1–3 already ship `matchedNotes: List<NoteTopology>` and `recalledNote`; keep the wire contract stable if possible.

### Link type options on recall result
- **D-05:** On the recall result surface, offer **property wiki link** and **relationship note** (the AM-04 / PROJECT pair). **Hide or skip "Insert as a wiki link"** when there is **no content cursor** (recall answer result has no editor). Relationship finalize via `AddRelationshipFinalize` remains available. If property-as-wiki needs write access without a cursor, prefer an existing API content/property update path (same spirit as dead-link resolve) rather than forcing the user into a separate edit session mid-recall. — **Reversibility:** reversible — which buttons render is presentation; can re-enable wiki-insert later if an editor context appears.
  - Rationale: Bare wiki-insert in `SearchForm` depends on `useContentCursorInserter`; it is a poor fit on the recall result page. PROJECT explicitly names property link and relationship note.

### Permissions & post-link behavior
- **D-06:** Show the link CTA only when the user **can write/link from the reviewed note** (same class of gate as NoteToolbar’s link button / non-readonly). If the reviewed note is read-only for this user, still show matched notes (Phase 3) but **omit** link CTAs. — **Reversibility:** reversible.
  - Rationale: Relationship/property creation mutates the user’s graph; offering a CTA that will fail authorization is worse UX.
- **D-07:** After a **successful** link (dialog `@closeDialog` / `@success`), **close the dialog** and **remain on the accidental-match recall result**. Do not auto-advance to the next recall card. — **Reversibility:** reversible.
  - Rationale: User may want to link another matched note or re-read the pair; advancing would discard that context.

### Claude's Discretion
- Exact CTA label copy ("Link to this note" vs similar) and button styling (primary/secondary/ghost) consistent with DaisyUI recall chrome.
- Whether to embed `SearchForm` with new optional `initialSelectedSearchResult` (or equivalent) props vs a thin recall-specific wrapper that mounts `LinkInsertionChoice` / `AddRelationshipFinalize` directly — prefer least duplication of the choice/finalize UX.
- How to resolve source `Note` and target `NoteSearchResult` (fetch-by-id, storageAccessor, or construct from topologies) — smallest correct path.
- Exact write-permission probe (reuse existing note realm / authorization flags already used by NoteShow/toolbar).
- Frontend unit vs E2E coverage split (phase has UI hint: yes — include observable UI/E2E coverage; capability-named tests, not phase numbers). Prefer extending the Phase 3 accidental-match E2E rather than inventing a parallel feature name.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project / requirements / roadmap
- `.planning/PROJECT.md` — core value ("a link offered"); Validated add-link UI assets; Active AM-04 offer; Out of Scope "Auto-creating links without user choice"; Constraints (link-building user-initiated); Key Decisions "Reuse … `LinkInsertionChoice`".
- `.planning/REQUIREMENTS.md` — **AM-04**: After an accidental match, the user can build a link between the reviewed note and a matched note via the existing add-link UI (property link or relationship note), with the matched note pre-selected.
- `.planning/ROADMAP.md` §"Phase 4: Offer link between notes" — goal + success criteria (existing add-link UI; matched pre-selected; never auto-write). UI hint: yes.

### Prior phase locks (carry forward — do not re-litigate)
- `.planning/phases/03-reveal-both-notes-after-accidental-match/03-CONTEXT.md` — D-03–D-05 reveal UI + distinct alert; **D-06** deferred add-link to Phase 4; `matchedNotes` list + `matchedNoteId` = first-of-list.
- `.planning/phases/02-accidental-match-grading-penalty/02-CONTEXT.md` — grading + `ACCIDENTAL_MATCH` + singular `matchedNoteId`; readability/IDOR filter.
- `.planning/phases/01-extend-answer-outcome-api/01-01-SUMMARY.md` — contract shapes for `AnsweredQuestion.matchedNotes`, `Answer.matchedNoteId` / `outcome`.

### Codebase maps
- `.planning/codebase/CONVENTIONS.md` — Vue PascalCase; `data-testid` / recall `data-test`; capability-named tests.
- `.planning/codebase/STRUCTURE.md` — frontend recall under `frontend/src/components/recall/`; links under `frontend/src/components/links/`.
- `.planning/codebase/CONCERNS.md` — application-level auth only; do not invent client-only permission bypasses for link creation.

### Source files (integration points — read before editing)
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — Phase 3 matched-notes section; primary UI integration point for CTAs.
- `frontend/src/components/links/SearchForm.vue` — existing add-link orchestrator (search → `LinkInsertionChoice` → `AddRelationshipFinalize`).
- `frontend/src/components/links/LinkInsertionChoice.vue` — wiki / property / relationship choice buttons for a selected target.
- `frontend/src/components/links/AddRelationshipFinalize.vue` — relationship-note creation finalize.
- `frontend/src/components/notes/core/NoteToolbar.vue` — reference for how `SearchForm` is opened with source `note` + readonly gating.
- `frontend/src/composables/useContentCursorInserter.ts` — why bare wiki-insert is unavailable without an editor cursor (grounds D-05).
- `frontend/tests/components/recall/AnsweredSpellingQuestion.spec.ts` — existing ACCIDENTAL_MATCH / matched-notes unit coverage to extend.
- `e2e_test/` accidental-match reveal feature (Phase 3) — extend for offer-link observability; capability-named.
- `packages/doughnut-test-fixtures/src/NoteSearchResultBuilder.ts` / `makeMe.aNoteSearchResult` — fixture shape for preselected targets in unit tests.
- `packages/generated/doughnut-backend-api/types.gen.ts` — `AnsweredQuestion`, `RecalledNote`, `NoteTopology`, `NoteSearchResult` (prefer no contract change).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AnsweredSpellingQuestion.vue` `matched-notes-section` — Phase 3 stack of `NoteShow` per matched id; attach per-row link CTAs here.
- `SearchForm` / `LinkInsertionChoice` / `AddRelationshipFinalize` — production add-link UX from NoteToolbar; reuse rather than inventing a recall-only link form.
- `NoteTopology` on each `matchedNotes` entry + `recalledNote.noteTopology` — enough ids/titles to identify source/target; may need a small fetch to satisfy `SearchForm`’s `Note` / `NoteSearchResult` props.
- `makeMe.aNoteSearchResult` — unit-test preselection without hitting search APIs.
- Phase 3 Vitest + E2E accidental-match coverage — extend for CTA + preselected choice dialog.

### Established Patterns
- Add-link from NoteToolbar: `PopButton` → `SearchForm` with source `note`, hidden when `readonly`.
- `SearchForm` state machine: search → `selectedSearchResult` → `LinkInsertionChoice` → optional `targetSearchResult` → `AddRelationshipFinalize`.
- Wiki insert / property insert depend on content-cursor inserter; relationship path uses API create + navigate.
- Accidental-match UI is frontend-only branching on `answer.outcome === 'ACCIDENTAL_MATCH'` + `matchedNotes`.

### Integration Points
- Frontend: `AnsweredSpellingQuestion.vue` matched-note rows → open add-link dialog with reviewed as source + matched as preselected target.
- Prefer extending `SearchForm` (optional initial selection) or a thin wrapper; do not fork relationship finalize logic.
- Backend: **no grading/SRS changes expected**; link creation uses existing note/relationship APIs already used by the toolbar flow.
- Auth: gate CTA on write/link ability for the reviewed note; creation endpoints already enforce server-side auth.

</code_context>

<specifics>
## Specific Ideas

- Preselection means **skip search and show the target already chosen**, not auto-create the link.
- Multi-match: user may open the dialog separately for each matched note via that row’s CTA.
- "Existing add-link UI" means the same choice/finalize experience users already know from the note toolbar — not a new recall-specific link schema.
- Stay on the answer result after linking so the learning opportunity (both notes visible) remains.

</specifics>

<deferred>
## Deferred Ideas

- **Alias-as-wiki-link overlap declaration** — Phase 5 (OVL-02, OVL-03).
- **Overlap "try again, no credit" response** — Phase 6 (OVL-01).
- **Bare wiki-insert into note body from recall** — deferred unless an editor cursor exists; v1 recall offer focuses on property + relationship (D-05).
- **MCQ accidental-match / fuzzy matching / qualified Notebook:Title typing** — v2, out of scope.

None of the above were folded into Phase 4; discussion stayed within AM-04 offer-link scope.

</deferred>

---

*Phase: 4-offer-link-between-notes*
*Context gathered: 2026-07-24*
