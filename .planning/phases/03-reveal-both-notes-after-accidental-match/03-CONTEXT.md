# Phase 3: Reveal both notes after accidental match - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning
**Mode:** `--auto` (all gray areas auto-selected; recommended option chosen for each; no interactive prompts)

<domain>
## Phase Boundary

After an **accidental match** (Phase 2 already grades `outcome = ACCIDENTAL_MATCH` and sets singular `matchedNoteId`), make the confusion **visible**: populate `AnsweredQuestion.matchedNotes` with **all** readable matched notes and show the **reviewed note and the matched note(s)** together in the spelling answer result UI (`AnsweredSpellingQuestion.vue`).

**In this phase:**
- Collect **all** readable accidental matches (not only the first) and return them as `matchedNotes: List<NoteTopology>`.
- Keep `Answer.matchedNoteId` as the first match (lowest id) for Phase 4 preselection continuity.
- Update the spelling answer-result UI so accidental match reveals the reviewed note **and** each matched note.
- Use a distinct accidental-match result message (not the plain "incorrect" copy alone).
- Leave the plain-wrong path UI unchanged when there is no accidental match.

**Not in this phase (later phases):**
- Offer add-link UI with matched note pre-selected → Phase 4 (AM-04).
- Alias-as-wiki-link overlap declaration → Phase 5; overlap "try again" → Phase 6.

</domain>

<decisions>
## Implementation Decisions

### Matched-notes collection (all matches)
- **D-01:** Extend the accidental-match lookup so grading can obtain **all** readable matches — title **∪** alias candidates, exact case-insensitive, across all notebooks the viewer can read, excluding the reviewed note, **deduped by note id**, ordered by **id ascending**. Prefer an additive `findAllAccidentalMatches` (or equivalent) on `WikiLinkResolver`; keep existing `findAccidentalMatch` as first-of-list / lowest-id for backward compatibility. — **Reversibility:** reversible — additive method + list population; singular `matchedNoteId` path stays.
  - Rationale: Phase 3 success criterion #2 requires surfacing **all** matched notes. Phase 2's title-then-alias **first** match would miss alias-only siblings when a title match exists first, and misses additional title matches. Union + dedupe is the correct "all matches" semantics.
- **D-02:** On accidental match, populate `AnsweredQuestion.matchedNotes` with `NoteTopology` for every match from D-01. Keep writing `Answer.matchedNoteId` to the **first** list entry (lowest id) and `outcome = ACCIDENTAL_MATCH`. Do **not** leave `matchedNotes` null/empty when an accidental match occurred. — **Reversibility:** reversible — fills the Phase 1 contract field Phase 2 intentionally left empty.
  - Rationale: Phase 2 D-05 deferred list population to Phase 3; Phase 1 already shipped `matchedNotes: List<NoteTopology>` on the OpenAPI client.

### Reveal depth & layout (UI)
- **D-03:** Reveal each matched note with a full **`NoteShow`** (same reveal depth as the reviewed note already shown in `AnsweredSpellingQuestion.vue`), not title-only links. — **Reversibility:** reversible — presentation-only; can thin later without contract change.
  - Rationale: PROJECT/ROADMAP goal is that "confusion becomes visible" — titles alone do not reveal what the user confused. Reuse existing `NoteShow` (`:expand-children="false"`) for consistency.
- **D-04:** Layout is a **vertical stack**: keep the existing reviewed-note block first (`NoteUnderQuestion` + `NoteShow` for the recalled note), then a labeled **"Matched note(s)"** section that renders one `NoteShow` per entry in `matchedNotes` (order = D-01 id ascending). No side-by-side columns. — **Reversibility:** reversible — layout-only.
  - Rationale: Matches current recall result flow (single column); scales when multiple notes match; reviewed note stays primary context.

### Accidental-match messaging
- **D-05:** When `answer.outcome === 'ACCIDENTAL_MATCH'`, show a **distinct** result message that the typed answer named **another note** (still not correct for the reviewed note). Keep non-success alert styling (error/warning — it remains a miss for the reviewed note). Plain wrong answers (`correct === false` without accidental match) keep today's "`Your answer … is incorrect.`" copy unchanged. — **Reversibility:** reversible — copy/branch in the Vue component.
  - Rationale: Visibility of *why* this outcome differs from a plain wrong answer is core user value for AM-03; Phase 4 will sit on the same result surface.

### Scope guard for wrong path & Phase 4
- **D-06:** No add-link / `LinkInsertionChoice` / preselection UI in this phase. Structure the matched-notes section so Phase 4 can attach actions later, but do not ship link-building controls now. — **Reversibility:** reversible.
  - Rationale: stop-safe / one observable behavior per phase; AM-04 is Phase 4.

### Claude's Discretion
- Exact English wording of the accidental-match alert (as long as it clearly distinguishes from plain incorrect).
- Whether `AnsweredQuestion.from(RecallPrompt)` gains a helper to attach `matchedNotes`, or the service/controller sets the list after `from()` — prefer the smallest seam that keeps authorization filtering with the lookup.
- Exact label text for the matched-notes section ("Matched note(s)" vs similar).
- Frontend unit vs E2E coverage split (phase has UI hint: yes — include observable UI coverage; prefer capability-named tests, not phase numbers).
- Whether to keep calling `findAccidentalMatch` internally as `findAll…().stream().findFirst()` or dual-path — either is fine if list semantics match D-01/D-02.

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project / requirements / roadmap
- `.planning/PROJECT.md` — core value ("both notes revealed"); Constraints (match scope all readable notebooks; exact case-insensitive; link-building deferred to user-initiated Phase 4); Current State (Phase 2 writes `matchedNoteId`/`ACCIDENTAL_MATCH`; Phase 3 reveals).
- `.planning/REQUIREMENTS.md` — **AM-03**: After an accidental match, the reviewed note and the matched note(s) are revealed together.
- `.planning/ROADMAP.md` §"Phase 3: Reveal both notes after accidental match" — goal + success criteria (both shown; when multiple match, all surfaced). UI hint: yes.

### Prior phase locks (carry forward — do not re-litigate)
- `.planning/phases/02-accidental-match-grading-penalty/02-CONTEXT.md` — D-05 deferred list population + UI reveal to Phase 3; D-01 wider lookup on `WikiLinkResolver`; D-02 `correct=false` + `outcome=ACCIDENTAL_MATCH`; readability filter / IDOR constraints.
- `.planning/phases/01-extend-answer-outcome-api/01-01-SUMMARY.md` — contract: `AnsweredQuestion.matchedNotes: List<NoteTopology>`; `Answer.matchedNoteId` + `AnswerOutcome`; `NoteTopology` is id+title (no new DTO).

### Codebase maps
- `.planning/codebase/CONVENTIONS.md` — Vue PascalCase components; `data-testid` / existing `data-test` for recall; capability-named tests; generated API types.
- `.planning/codebase/STRUCTURE.md` — frontend recall under `frontend/src/components/recall/`; backend services/controllers layout.
- `.planning/codebase/CONCERNS.md` — application-level auth only; matched-note data must never leak unreadable notebooks (reuse Phase 2 readability filter when collecting the full list).

### Source files (integration points — read before editing)
- `frontend/src/components/recall/AnsweredSpellingQuestion.vue` — current result UI (incorrect alert + reviewed `NoteUnderQuestion` + `NoteShow`); primary UI integration point.
- `frontend/src/pages/RecallPage.vue` — mounts `AnsweredSpellingQuestion` for spelling results.
- `frontend/src/components/notes/NoteShow.vue` — reuse for each matched note reveal.
- `frontend/src/components/recall/NoteUnderQuestion.vue` / `NoteTitleWithLink.vue` — existing reviewed-note chrome patterns (optional for matched-note labels).
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` — `matchedNotes` field exists; `from(RecallPrompt)` does not populate it yet.
- `backend/src/main/java/com/odde/doughnut/controllers/dto/NoteTopology.java` — id + title shape for list entries.
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — `findAccidentalMatch` (first only); extend for all-matches collection.
- `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — `answerSpelling` (~260–297) sets singular match today; wire list population here (or immediately adjacent DTO assembly).
- `backend/src/main/java/com/odde/doughnut/controllers/RecallPromptController.java` — returns `AnsweredQuestion` from answer endpoint; ensure `matchedNotes` reaches the client.
- `packages/generated/doughnut-backend-api/types.gen.ts` — `matchedNotes?: Array<NoteTopology>` and `outcome?: '…' | 'ACCIDENTAL_MATCH' | …` already present (no contract regen required unless DTO assembly changes force it).

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `AnsweredQuestion.matchedNotes` + generated TS types — already on the wire contract from Phase 1; Phase 3 fills them.
- `NoteShow` — already used for the reviewed note in `AnsweredSpellingQuestion.vue`; reuse per matched note id.
- `WikiLinkResolver.findAccidentalMatch` + readability helpers (`userMayReadNotebook`, title/alias candidate queries) — extend to return the full readable set.
- `Note.getNoteTopology()` — maps entity → `NoteTopology` for the list.
- Phase 2 grading path in `MemoryTrackerService.answerSpelling` — already sets `matchedNoteId` + `ACCIDENTAL_MATCH`; extend to set `matchedNotes` on the response DTO.

### Established Patterns
- Spelling answer result is a single-column stack (alert → note-under-question → memory-tracker link → `NoteShow`).
- DaisyUI alert success/error for correct vs incorrect.
- Authorization filtering for accidental match is Java-side readability checks (Phase 2) — all-matches collection must use the same filter (IDOR).
- `@Transient` answer fields ride along when `AnsweredQuestion.from` embeds `Answer`; `matchedNotes` is on the DTO itself and must be set explicitly.

### Integration Points
- Backend: after accidental-match detection in `answerSpelling`, collect all matches → set `matchedNoteId` (first) + populate `AnsweredQuestion.matchedNotes` before returning to the client.
- Frontend: `AnsweredSpellingQuestion.vue` branches on `answer.outcome === 'ACCIDENTAL_MATCH'` for copy + renders `matchedNotes` with `NoteShow`.
- No OpenAPI shape change expected; verify types already cover the fields.

</code_context>

<specifics>
## Specific Ideas

- "Reveal" means **content-visible** (`NoteShow`), not merely linking to the matched note title.
- Multi-match ordering mirrors Phase 2's lowest-id preference: sort by id ascending; first entry remains `matchedNoteId`.
- Title∪alias union matters: if note A matches by title and note B by alias for the same answer string, **both** appear in `matchedNotes`.
- Plain wrong UI must not regress — only the accidental-match branch gains new chrome/copy.

</specifics>

<deferred>
## Deferred Ideas

- **Offer add-link UI with matched note pre-selected** — Phase 4 (AM-04); reuses `LinkInsertionChoice` / `AddRelationshipFinalize`.
- **Alias-as-wiki-link overlap declaration** — Phase 5 (OVL-02, OVL-03).
- **Overlap "try again, no credit" response** — Phase 6 (OVL-01).
- **MCQ accidental-match / fuzzy matching / qualified Notebook:Title typing** — v2, out of scope.

None of the above were folded into Phase 3; discussion stayed within AM-03 reveal scope.

</deferred>

---

*Phase: 3-reveal-both-notes-after-accidental-match*
*Context gathered: 2026-07-24*
