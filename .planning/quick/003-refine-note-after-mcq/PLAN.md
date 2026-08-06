# Refine note after answered MCQ

**Status:** in progress  
**Goal:** From an answered MCQ, open Refine note beside View Memory Tracker; reuse assimilation refinement; pass the question into layout breakdown so question-led points are separate items and preselected for extract/remove.

## Locked design

| ID | Decision |
|----|----------|
| D-01 | Reuse assimilation `NoteRefinement` via shared modal chrome (extract only when duplication exists — Phase 2). |
| D-02 | Schema flag `ledToQuestion` on `NoteRefinementLayoutItem` (parallel to `alreadyExtracted`). AI sets it when question context is present; otherwise false. Frontend preselects those items after layout load. |
| D-03 | Optional request body `NoteRefinementQuestionContextDTO` (stem + choices; optional correct index / tested focus) on generate (and export if needed). Absent body = assimilation path unchanged. |
| D-04 | Scope: answered **MCQ** only (`AnsweredQuestionComponent`). Spelling answered refine deferred. |
| D-05 | Load full `Note` via existing `getNoteRealmRefAndLoadWhenNeeded(recalledNote.noteTopology.id)`. Hide Refine when content blank (match assimilation). |

## Deferred

- Refine on answered spelling questions
- Extra UI badge for `ledToQuestion` beyond checkbox preselection
- Passing question context into extract/remove prompts (breakdown only for this plan)

## Experience that shaped this plan

Prior `/gsd-quick` packed button + shared modal + DTO + AI schema + preselect + E2E into **one vertical slice** (~37 files). That blew the ~5 min / stop-safe budget and mixed structure with multiple behaviors. This plan splits by **observable outcome**, with structure phases only for the **immediate next** behavior.

---

### Phase 1: Refine note entry from answered MCQ
**Type:** Behavior  
**Status:** done

**Pre-condition:** Learner has answered an MCQ; recalled note has non-blank content.  
**Trigger:** Opens answered question view; clicks **Refine note** next to **View Memory Tracker**.  
**Post-condition:** Same refine modal/`NoteRefinement` flow as assimilation opens (layout generate with **no** question context yet — interim OK).

**Tests:** Mounted `AnsweredQuestionComponent` unit test (button + opens refine). Extend assimilation E2E patterns later; no new E2E required this phase if unit covers the entry.

**Touch:** `AnsweredQuestionComponent.vue` (+spec); dialog chrome copied from `AssimilationSettings.vue` (extract in Phase 2).

**Learnings:** Seed note realm in storage for unit tests (`refreshNoteRealm`); attachTo `document.body` for Teleport modal; unmount wrappers before clearing body (Quill in QuestionStem). Refine visibility keys off loaded note content after `getNoteRealmRefAndLoadWhenNeeded`.

---

### Phase 2: Shared RefineNoteModal
**Type:** Structure  
**Status:** planned

**Structure change:** Extract dialog chrome shared by assimilation and answered MCQ so both call one `RefineNoteModal` wrapping `NoteRefinement`.  
**Unlocks:** Phase 3+ can add optional question props in one place without forked UIs.  
**Verify:** Existing assimilation refine unit/E2E still pass; answered entry still opens refine.

---

### Phase 3: `ledToQuestion` on layout item schema
**Type:** Structure  
**Status:** planned

**Structure change:** Add required boolean `ledToQuestion` to `NoteRefinementLayoutItem`; update constructors/fixtures/validators as needed; regenerate API client. Without question context, AI/tooling still yields `false` (or tests stub false).  
**Unlocks:** Phase 4–5 can mark and preselect.  
**Verify:** Backend + frontend layout tests still green; assimilation refine unchanged externally.

---

### Phase 4: Optional MCQ context on layout generate API
**Type:** Structure  
**Status:** planned

**Structure change:** Optional `NoteRefinementQuestionContextDTO` on `POST generate-refinement-suggestions` (and matching export builder if GET cannot carry body — prefer POST). When body present, append tool instructions: separate question-led points at proper levels and set `ledToQuestion=true`. When absent, identical to today.  
**Unlocks:** Phase 5 frontend can pass context and rely on the flag.  
**Verify:** Controller tests with/without body; regenerate client. No answered-page wiring yet.

---

### Phase 5: Pass question + preselect `ledToQuestion`
**Type:** Behavior  
**Status:** planned

**Pre-condition:** Phases 1–4 done; answered MCQ refine open.  
**Trigger:** Layout generation runs from answered MCQ refine.  
**Post-condition:** Generate request includes MCQ context; items with `ledToQuestion=true` are selected by default (user can extract or remove). Assimilation refine (no context) still starts with empty selection.

**Tests:** `NoteRefinement` / layout selection unit tests + `AnsweredQuestionComponent` asserting request body.

---

### Phase 6: E2E refine-after-MCQ with mocked OpenAI
**Type:** Behavior  
**Status:** planned

**Pre-condition:** Phases 1–5 done.  
**Trigger:** E2E answers MCQ on contentful note, opens Refine note.  
**Post-condition:** Mocked layout shows question-led points already selected.

**Artifact:** `e2e_test/features/recall/refine_note_after_mcq.feature` (capability-named). Tag `@wip` only while red.

---

## Success criteria (whole plan)

- Stop after any phase: value ≥ phases completed (Phase 1 alone is useful).
- Assimilation refine unchanged when no question context.
- No phase numbers in product/test names.
- Spelling refine not implemented.
