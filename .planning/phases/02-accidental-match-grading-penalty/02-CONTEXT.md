# Phase 2: Accidental-match grading & penalty - Context

**Gathered:** 2026-07-24
**Status:** Ready for planning
**Mode:** `--auto` (all gray areas auto-selected; recommended option chosen for each; no interactive prompts)

<domain>
## Phase Boundary

Detect an **accidental match** at the spelling-answer grading moment and apply a lighter-than-wrong spaced-repetition penalty. Specifically: in `MemoryTrackerService.answerSpelling`, when a spelling answer is **wrong for the reviewed note** (`Note.matchAnswer` returns `false`) but **exactly matches the title or alias of a different note** in any notebook the current user can read, the system grades it as an accidental match — records `Answer.matchedNoteId` + `Answer.outcome = ACCIDENTAL_MATCH` (the Phase 1 contract fields, which currently have 0 writers) and applies a lighter SRS penalty than a plain wrong answer.

**In this phase (backend behavior only):**
- Widen the title/alias lookup to search **all readable notebooks** (broader than the notebook-scoped `WikiLinkResolver`).
- Set `matchedNoteId` (singular) + `outcome = ACCIDENTAL_MATCH` on the `Answer`.
- Apply a lighter SRS penalty via `updateForgettingCurve` (no 12h override).
- Skip the accidental-match search entirely when the answer already matches the reviewed note (overlap is declared, not auto-detected — Phases 5/6).
- Gate everything behind the existing `assertReadAuthorization` gate; the wider search must only return notes the user can read (matched-note data first crosses the trust boundary here — OWASP ASVS V4 Access Control re-check).

**Not in this phase (later phases):**
- Revealing both notes in the UI → Phase 3 (populates `AnsweredQuestion.matchedNotes: List<NoteTopology>`).
- Offering the add-link UI with the matched note pre-selected → Phase 4.
- Alias-as-wiki-link overlap declaration → Phase 5; "try again, no credit" overlap response → Phase 6.

</domain>

<decisions>
## Implementation Decisions

### Accidental-match lookup mechanism & home
- **D-01:** Extend `WikiLinkResolver` with a new wider-lookup method (e.g. `findAccidentalMatch(String answer, Note reviewedNote, User viewer)`) that searches by **title then alias**, **exact + case-insensitive**, across **all notebooks the viewer can read**, **excluding the reviewed note**. Inject `WikiLinkResolver` into `MemoryTrackerService`. — **Reversibility:** reversible — adds a method + a constructor-injected dependency; `MemoryTrackerService` already has injected collaborators, and the new method is additive.
  - Rationale: matches the locked PROJECT.md constraint "Reuse `WikiLinkResolver`" while satisfying "needs a wider lookup than the existing unqualified resolver." Keeps all title/alias match logic cohesive in the resolver instead of duplicating it in the service.
  - The existing `resolveWikiLinkToken` is **not** the right fit as-is: it parses `[[wiki-link]]` token syntax via `WikiLinkTargetReference.forToken` (extracts a notebook qualifier from the focus note's notebook) and is notebook-scoped. The answer string is a bare title/alias, not a wiki-link token, and the search must cross notebook boundaries.
  - Reuse the existing `noteCandidates(notebookName, noteTitle)` / `aliasTargetCandidates` building blocks and the `authorizationService.userMayReadNotebook(viewer, notebook)` readability filter already used by `firstReadableNotebookMatch` — but with **no notebook-name scoping** (search all readable notebooks).

### SRS penalty magnitude & `correct`/threshold semantics
- **D-02:** An accidental match records `Answer.correct = false` (it is wrong for the reviewed note; `correct` stays the sole SRS-credit signal per the Phase 1 lock) and `Answer.outcome = ACCIDENTAL_MATCH`. — **Reversibility:** reversible — `outcome` is `@Transient`/non-persisted; `correct=false` matches today's wrong path.
- **D-03:** The accidental-match SRS penalty is `updateForgettingCurve(memoryTracker, -10.0f)` — i.e. **half of the plain-wrong penalty** (wrong = `ForgettingCurve.failed()` = `add(-DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT * 2)` = **-20** to the index, plus a 12h `nextRecallAt` override). The accidental match applies **-10** and **no 12h override** (let `MemoryTracker.calculateNextRecallAt()` recompute from the new index). — **Reversibility:** reversible — the magnitude is a single `float` literal; no schema/contract impact (`outcome` is `@Transient`).
  - Rationale: matches the PROJECT.md constraint hint "`updateForgettingCurve(-10)`-style, no 12h override" and the key decision "lighter than a plain wrong answer." -10 is exactly half of the -20 wrong penalty and reuses the existing `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (10) constant.
- **D-04:** The accidental match **still counts toward the wrong-answer re-assimilation threshold** (the existing 5-wrong-in-14-days rule in `hasExceededWrongAnswerThreshold`). The threshold logic itself is unchanged (out-of-scope: "existing wrong-answer threshold behavior unchanged"). — **Reversibility:** reversible.
  - Rationale: an accidental match is still a wrong answer for the reviewed note; the user did not recall it. The out-of-scope item forbids changing the threshold, so the safest behavior is to let it count (consistent with `correct=false`). If the planner finds the threshold check is coupled to the wrong-path penalty application, it must preserve the threshold check while applying the lighter penalty.
  - Mechanic is a planning detail: the lighter penalty must NOT go through `MemoryTracker.recallFailed` (which applies -20 + 12h). Likely a dedicated accidental-match path that: increments `recallCount`, applies `updateForgettingCurve(-10)` (no 12h), and still runs `hasExceededWrongAnswerThreshold`. Research/planner to confirm the exact seam.

### Matched-note selection (single vs all)
- **D-05:** Phase 2 captures the **first readable match** (lowest `id`, excluding the reviewed note) and sets `Answer.matchedNoteId` to that single id. It leaves `AnsweredQuestion.matchedNotes` (the `List<NoteTopology>`) **empty/null** — Phase 3 owns populating that list and revealing all matched notes. — **Reversibility:** reversible.
  - Rationale: stop-safe / one-observable-behavior-per-phase. Phase 2's success criteria only require *detecting* an accidental match (singular `matchedNoteId`); Phase 3's explicit scope is "when multiple notes match, all matched notes are surfaced." Front-running the list in Phase 2 would blur the phase boundary.
  - `AnsweredQuestion.from(recallPrompt)` embeds the full `Answer` entity (`setAnswer(recallPrompt.getAnswer())`), so `matchedNoteId` + `outcome` surface in the response automatically — **no DTO change needed in Phase 2**. `AnsweredQuestion.matchedNotes`/`overlap` stay null (Phase 3 / Phase 6 fill them).

### Skip-when-matches-reviewed-note & shared-title/alias edge case
- **D-06:** If `Note.matchAnswer` returns `correct=true` (the answer matches the reviewed note's title or alias), the accidental-match search is **skipped entirely** — even if another readable note shares that same title or alias. That shared-title situation is the **overlap** case, which is **declared** (Phase 5 alias-as-wiki-link) and **not auto-detected**. — **Reversibility:** reversible.
  - Rationale: directly satisfies Phase 2 success criterion #3 and the locked decision "Overlap is declared, not auto-detected." Auto-flagging a correct answer that also names another note would conflate accidental match with overlap and front-run Phase 6.

### Claude's Discretion
- Exact method name and signature for the new `WikiLinkResolver` wider-lookup method.
- The precise code seam for applying the lighter penalty while preserving the threshold check (research/planner to confirm against `MemoryTracker.markAsRecalled` / `recallFailed`).
- Whether the wider lookup is one combined query or a title-then-alias fallback across readable notebooks (follow the existing `noteCandidates` title-then-alias ordering).
- Test placement and naming (follow existing `RecallPromptControllerTests` / `RecallsControllerTests` patterns; backend unit/integration tests only — no E2E in Phase 2, since there is no UI).

</decisions>

<canonical_refs>
## Canonical References

**Downstream agents MUST read these before planning or implementing.**

### Project / requirements / roadmap
- `.planning/PROJECT.md` — "What This Is", "Constraints" (match scope all readable notebooks; exact case-insensitive title/alias; lighter-than-wrong penalty; reuse `WikiLinkResolver`/`Note.matchAnswer`/`updateForgettingCurve`; safety: link-building user-initiated), "Key Decisions", and "Current State" (Phase 2 writes `matchedNoteId` + `outcome=ACCIDENTAL_MATCH` behind `assertReadAuthorization`; re-check OWASP ASVS V4).
- `.planning/REQUIREMENTS.md` — AM-01 (detect accidental match across all readable notebooks) and AM-02 (lighter-than-wrong third SRS outcome, no 12h override) are the two requirements this phase delivers.
- `.planning/ROADMAP.md` §"Phase 2: Accidental-match grading & penalty" — goal + 3 success criteria (detect distinct from wrong; lighter penalty via `updateForgettingCurve` no 12h; skip search when answer matches reviewed note).

### Phase 1 foundation (direct dependency)
- `.planning/phases/01-extend-answer-outcome-api/01-01-SUMMARY.md` — the contract this phase writes to: `AnswerOutcome` enum (CORRECT/WRONG/ACCIDENTAL_MATCH/OVERLAP), `Answer.matchedNoteId` (@Transient Long) + `Answer.outcome` (@Transient AnswerOutcome), `AnsweredQuestion.overlap` + `AnsweredQuestion.matchedNotes: List<NoteTopology>`. Locked: `correct` stays @NotNull/sole SRS-credit signal; 0 writers in Phase 1; no Flyway migration (fields are @Transient). A1 pattern: JPA `@Transient` surfaces in OpenAPI via Jackson/springdoc.

### Codebase maps (security + architecture patterns)
- `.planning/codebase/CONCERNS.md` — "HTTP security is permit-all; auth is manual per controller" and "Application-level auth only": authorization is enforced **only** when controllers call `AuthorizationService`. The wider accidental-match search must filter by readability (IDOR / horizontal privilege risk: a `matchedNoteId` must never leak a note in a notebook the user cannot read).
- `.planning/codebase/ARCHITECTURE.md` — `AuthorizationService` is the single read/write gate (`assertReadAuthorization`); controllers stay thin and authorize first; `EntityPersister`/repositories for persistence; `@Transient` contract fields are non-persisted.

### Source files (integration points — read before editing)
- `backend/src/main/java/com/odde/doughnut/services/MemoryTrackerService.java` — `answerSpelling(RecallPrompt, …)` (lines 255–281) is the grading seam; `markAsRecalled` (152–165) drives the wrong/correct path + threshold; `updateForgettingCurve` (138–142) is the partial-adjustment API for the lighter penalty; `hasExceededWrongAnswerThreshold` (283–290) is the re-assimilation threshold.
- `backend/src/main/java/com/odde/doughnut/services/WikiLinkResolver.java` — `resolveWikiLinkToken` (notebook-scoped, wiki-link-syntax) is the reuse target; `noteCandidates` / `aliasTargetCandidates` / `firstReadableNotebookMatch` (`authorizationService.userMayReadNotebook`) are the building blocks for the new wider lookup.
- `backend/src/main/java/com/odde/doughnut/entities/Note.java` — `matchAnswer` (135–140): title match via `getNoteTitle().matchesForRecall` OR alias match via `FrontmatterAliases.matchesFromNoteContent`.
- `backend/src/main/java/com/odde/doughnut/entities/Answer.java` — `@Transient matchedNoteId` + `@Transient outcome` (Phase 1 fields this phase writes); `correct` is `@NotNull`.
- `backend/src/main/java/com/odde/doughnut/entities/AnswerOutcome.java` — enum CORRECT/WRONG/ACCIDENTAL_MATCH/OVERLAP.
- `backend/src/main/java/com/odde/doughnut/entities/ForgettingCurve.java` — `failed()` = `add(-10*2)` = -20 (wrong penalty); `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT = 10`; grounds the -10 "half-of-wrong" accidental-match penalty.
- `backend/src/main/java/com/odde/doughnut/entities/MemoryTracker.java` — `recallFailed` (124–127) applies `failed()` + 12h override (the path to AVOID for accidental match); `markAsRecalled` (139–147) increments `recallCount` and branches.
- `backend/src/main/java/com/odde/doughnut/controllers/RecallPromptController.java` — `answerSpelling` endpoint (81–95) calls `assertCanMutateRecallPrompt` → `assertLoggedIn` + `assertReadAuthorization(memoryTracker)`; the existing auth gate Phase 2 relies on.
- `backend/src/main/java/com/odde/doughnut/controllers/dto/AnsweredQuestion.java` — `from(recallPrompt)` embeds the full `Answer` (so `matchedNoteId`/`outcome` surface with no DTO edit); `matchedNotes`/`overlap` left null for Phases 3/6.
- `backend/src/main/java/com/odde/doughnut/entities/repositories/NoteRepository.java` & `NoteAliasIndexRepository` — existing title/alias query methods (e.g. `findByNotebookNameAndNoteTitleOrderByIdAsc`); the wider lookup may add a non-notebook-scoped variant or reuse candidates + Java-side readability filtering.

</canonical_refs>

<code_context>
## Existing Code Insights

### Reusable Assets
- `WikiLinkResolver` — already has `noteCandidates` (title-then-alias fallback) and `aliasTargetCandidates` plus the `authorizationService.userMayReadNotebook(viewer, notebook)` readability filter. The new wider lookup reuses these building blocks with notebook-scoping removed.
- `Note.matchAnswer` — unchanged; remains the sole judge of whether the answer matches the reviewed note (`correct`). Phase 2 only adds the "if not correct, search for an accidental match" branch after it.
- `MemoryTrackerService.updateForgettingCurve(memoryTracker, float adjustment)` — the existing partial-adjustment API; directly applies `adjustment` to `forgettingCurveIndex` and recomputes `nextRecallAt` via `calculateNextRecallAt()` (no 12h override). This is the exact seam for the lighter -10 penalty.
- `AnsweredQuestion.from(recallPrompt)` — embeds the full `Answer`, so `matchedNoteId`/`outcome` ride along with zero DTO edits.
- `NoteTopology` (id + title) — already the shape for `matchedNotes`; Phase 2 does not need it (singular `matchedNoteId` only), but Phase 3 will.

### Established Patterns
- **Authorization-first controllers:** `RecallPromptController.answerSpelling` already calls `assertCanMutateRecallPrompt` → `assertLoggedIn` + `assertReadAuthorization(memoryTracker)`. Phase 2 relies on this existing gate; the new wider search must not bypass `AuthorizationService` (CONCERNS.md: HTTP security is permit-all; auth is manual per controller).
- **Readability filtering in Java, not the DB:** `firstReadableNotebookMatch` iterates candidates and calls `userMayReadNotebook` — there is no DB-level readability predicate. The wider lookup should follow the same pattern to avoid leaking `matchedNoteId` for unreadable notebooks (IDOR).
- **`@Transient` contract fields are non-persisted:** no Flyway migration for `matchedNoteId`/`outcome` (Phase 1 lock). Phase 2 writes them at grading time; they are not persisted.
- **`correct` is the sole SRS-credit signal:** `AnsweredQuestion.from` and the SRS path key off `correct`. `outcome` is metadata alongside it, not a replacement.

### Integration Points
- `MemoryTrackerService.answerSpelling(RecallPrompt, AnswerSpellingDTO, User, Timestamp)` (lines 255–281): after `Boolean correct = note.matchAnswer(spellingAnswer)`, add the accidental-match branch — when `!correct`, call the new `WikiLinkResolver` wider lookup; if a match is found, set `answer.setMatchedNoteId(id)` + `answer.setOutcome(AnswerOutcome.ACCIDENTAL_MATCH)` and apply the lighter penalty instead of the full wrong path.
- `WikiLinkResolver` constructor: add `WikiLinkResolver` as an injected dependency of `MemoryTrackerService` (new constructor param).
- No controller, DTO, OpenAPI, or frontend changes in Phase 2 (contract already exists from Phase 1; `outcome`/`matchedNoteId` already in `types.gen.ts`).

</code_context>

<specifics>
## Specific Ideas

- Penalty magnitude anchored to the existing constant: -10 = `DEFAULT_FORGETTING_CURVE_INDEX_INCREMENT` (10) = exactly half of the wrong penalty (`failed()` = -20). This keeps the accidental-match penalty expressible in terms of the existing SRS constants rather than a magic number.
- "Lighter than wrong" is interpreted as: half the index penalty AND dropping the 12h `nextRecallAt` override (so the next recall is recomputed from the new index, which is sooner than a correct answer but not forcibly clamped to +12h like a wrong answer).
- Match semantics: exact, case-insensitive title or alias match (reuse `FrontmatterAliases.normalizedLookupKey` for alias normalization, consistent with the existing alias index lookup).
- The reviewed note itself is excluded from the accidental-match candidates (a self-match is not an accidental match).

</specifics>

<deferred>
## Deferred Ideas

- **Reveal both notes (reviewed + matched) in the UI** — Phase 3 (AM-03); will populate `AnsweredQuestion.matchedNotes: List<NoteTopology>` and surface them in `AnsweredSpellingQuestion.vue`.
- **Surfacing ALL matched notes (plural)** — Phase 3 success criterion #2 ("when multiple notes match, all matched notes are surfaced"). Phase 2 only captures the first match for `matchedNoteId`.
- **Offer the add-link UI with the matched note pre-selected** — Phase 4 (AM-04); reuses `LinkInsertionChoice` / `AddRelationshipFinalize`.
- **Alias-as-wiki-link overlap declaration** — Phase 5 (OVL-02, OVL-03); the known alias-blast-radius spike lives there, not here.
- **Overlap "try again, no credit" response** — Phase 6 (OVL-01); `AnsweredQuestion.overlap` flag + `outcome = OVERLAP`.
- **MCQ accidental-match / fuzzy matching / cross-notebook qualified `Notebook:Title` typing** — v2, explicitly out of scope for v1.

None of the above were folded into Phase 2; discussion stayed within the accidental-match grading & penalty scope.

</deferred>

---

*Phase: 2-accidental-match-grading-penalty*
*Context gathered: 2026-07-24*

