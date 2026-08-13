# Remember spelling as a memory tracker

Status: in-progress (Phase 4 done)
Plan state: [STATE.md](./STATE.md) (this folder only — do not edit `.planning/STATE.md`)

Each phase is one commit. Type is Behavior or Structure. One observable behavior (or one structure change for the immediate next behavior).

## Design decisions

- **UI copy:** caret item **Remember spelling**. Same assimilate-options caret as commissioned.
- **API:** `assimilateAsSpelling`, parallel to `assimilateAsCommissioned`.
- **Queue / daily count:** understanding note-level trackers only. Spelling does not consume the queue or daily count. Stay on the note after Remember spelling.
- **Verification:** existing title popup, attached to Remember spelling (Phase 3). Phase 2 may ship the action without verification (interim).
- **Checkbox stays** until Phase 6. Do not remove the note field before the action exists.
- **Tests:** delete option-only tests in Phase 6. Keep spelling behavior; move fixtures off the note flag in Phases 4–5.
- **ADRs:** 0001 and 0003 describe the current model only. Status stays Proposed.
- **No** notebook-level default. **No** migration of old `remember_spelling` values.

---

### Phase 1: Assimilate API can create a spelling tracker

Type: Behavior  
Status: done

**Pre-condition:** Note has no spelling tracker.

**Trigger:** `POST /api/assimilation` with `assimilateAsSpelling: true`.

**Post-condition:** One `SPELLING` tracker exists. The note is still due for ordinary (understanding) assimilation. Daily assimilation count is unchanged. Empty result if a spelling tracker already exists or `propertyKey` is set.

Shipped: `assimilateAsSpelling`; ordinary due/daily count are understanding-only (`JPA_WHERE_UNDERSTANDING_*`). Testability `assimilateNoteAsSpelling`. Checkbox path still creates understanding+spelling.

**Learning:** Commissioned and spelling share `assimilateAsNoteLevelType`. Daily count for a remember-spelling checkbox assimilate is now 1 (understanding only), not 2.

---

### Phase 2: Assimilation offers Remember spelling

Type: Behavior  
Status: done

**Pre-condition:** Phase 1 API exists. Learner is assimilating a normal note with content and no spelling tracker.

**Trigger:** Choose **Remember spelling** from the assimilate-options caret.

**Post-condition:** A spelling tracker is created. UI stays on this note. Assimilate (understanding) stays enabled. Daily count in the UI does not bump. Checkbox still present.

Shipped: **Remember spelling** on `assimilate-options-caret`. Checkbox still present.

**Learning:** Caret `data-test` is `assimilate-options-caret` (shared with commissioned). Note-level tracker predicates live in `noteLevelMemoryTrackers.ts`.

---

### Phase 3: Remember spelling verifies the title first

Type: Behavior  
Status: done

**Pre-condition:** Remember spelling action exists (Phase 2).

**Trigger:** Choose Remember spelling and submit a spelling attempt.

**Post-condition:** Tracker is created only after a correct title (or alias). Wrong spelling stays on the popup with the existing error. Cancel creates nothing.

Shipped: Remember spelling opens `SpellingVerificationPopup`; verified posts `assimilateAsSpelling`. Checkbox still verifies on Assimilate.

**Learning:** One pending-assimilate after the popup distinguishes caret vs checkbox.

---

### Phase 4: Remember spelling is offered only when it applies

Type: Behavior  
Status: done

**Pre-condition:** Remember spelling action exists.

**Trigger:** Open assimilation for a note that cannot take a spelling tracker.

**Post-condition:** Remember spelling is absent when: no content, relationship/link note, spelling tracker already exists, or the row is a property. Commissioned-only notes still show Remember spelling if spelling does not exist.

Shipped in `showSpellingOption` (`hasNoteContent`, `isLinkNote`, no existing SPELLING tracker). Property rows never get the option.

---

### Phase 5: Spelling tests do not use the note flag

Type: Structure  
Status: planned

Unlocks Phase 6 (removing the flag must not drop spelling coverage).

No product change. Move fixtures off `rememberSpelling` / Gherkin `Remember Spelling` / `NoteBuilder.rememberSpelling()` for tests that assert spelling *behavior*:

- Recall E2E: `recall_quiz_spelling_question`, `accidental_match_reveal`, `overlap_try_again`, `browse_answer_and_notes_while_recalling`, CLI spelling rule → `assimilateNoteAsSpelling` (and understanding assimilate only when the scenario needs both).
- Backend: `RecallPromptOverlapTryAgainTests`, `RecallPromptControllerTestBase` → `aMemoryTrackerFor(note).spelling()`.
- `AiQuestionGeneratorTests`: drop unused `.rememberSpelling()`.
- Step `I assimilate the note … with the option of remembering spelling` → Remember spelling + verify (wikilink-stem scenario stays).

Do **not** delete option-only tests yet (Phase 6). Existing tests still pass.

**Verify:** `pnpm backend:test_only`; Cypress specs listed above.

---

### Phase 6: Remove the Remember Spelling note setting

Type: Behavior  
Status: planned

**Pre-condition:** Phases 2–5 done (action exists; spelling tests do not need the flag).

**Trigger:** Open assimilation / note recall settings.

**Post-condition:** No Remember Spelling checkbox. Ordinary Assimilate never creates a spelling tracker. Spelling trackers come only from Remember spelling / `assimilateAsSpelling`.

**Delete** (option-only — do not retarget):

- E2E outline **Remembering spelling availability depends on note content**
- `NoteRecallSettingForm.spec.ts` checkbox cases
- AssimilationPanel add-spelling-only-via-flag cases
- `shouldCreateTwoMemoryTrackersWhenRememberSpellingIsTrue`
- `shouldAddOnlySpellingTrackerWhenAddSpellingOnly…`
- `PredefinedQuestionTest.SpellingQuiz.shouldAlwaysChooseAIQuestionIfConfigured`

Remove checkbox-driven verification steps and add-spelling-only UI that reads the flag. Drop `NotesTestData` **Remember Spelling** column and `NoteBuilder.rememberSpelling()` if unused.

**Verify:** backend assimilate tests; frontend `NoteRecallSettingForm` + assimilation panel; `assimilate_with_remembering_spelling.feature` and `recall_quiz_spelling_question.feature`.

---

### Phase 7: ADRs describe spelling as a tracker

Type: Structure  
Status: planned

Unlocks: none further for product — records the Phase 6 decision in Proposed ADRs (requested). No historical “checkbox used to exist” text. Do not mark Accepted.

- `docs/adrs/0001-ubiquitous-language.md`: **Spelling memory tracker**, **Remember spelling** (learner action at assimilation, verifies title). Drop glossary that calls it a note option. Ordinary assimilation due = understanding tracker.
- `docs/adrs/0003-spaced-repetition-scheduling-policy.md`: spelling tracker is learner-created; assimilation due/daily count are understanding-only. Keep accidental-match / overlap grading as they are.

**Verify:** read-through only (no product tests).

---

### Phase 8: Remove `rememberSpelling` from the note setting type

Type: Structure  
Status: planned

Unlocks Phase 9 (column unused in code).

No user-visible change. Remove the property from `NoteRecallSetting`, API client (`pnpm generateTypeScript`), and leftover wiring. Existing tests pass.

**Verify:** `pnpm backend:test_only`; frontend recall-setting tests.

---

### Phase 9: Drop `remember_spelling` column

Type: Structure  
Status: planned

Unlocks: schema matches Phase 8.

Flyway version `> 300000247`. Do not edit old migrations. No backfill. Existing `SPELLING` trackers unchanged; unread flags discarded.

**Verify:** `pnpm backend:test_only`; `scripts/check_diff_whitespace.sh`.

---

## Stop-safe

| Stop after | User value | Waste if later phases never run |
|--|--|--|
| 1 | API/testability can add a spelling tracker without consuming understanding due | No web UI |
| 2 | Learners can Remember spelling from assimilation | No title check; checkbox still there |
| 3 | Title check on the new action | Checkbox still there |
| 4 | Action hidden when it cannot apply | Checkbox still there |
| 5 | None (tests only) | Test wiring slightly ahead of UI removal |
| 6 | Intended product (no note setting) | Dead column + Java field |
| 7 | Glossary matches the product | Dead column |
| 8 | Code matches the product | Dead column |
| 9 | Clean schema | None |

Phase 5 is waste-if-stopped (test-only). It exists so Phase 6 cannot accidentally delete spelling coverage. Do not skip it.

## Out of scope

- Notebook-level default
- Migrating old `remember_spelling` flags into trackers
- Changing spelling recall grading
- Renaming “Assimilate as commissioned”
