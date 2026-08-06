# Refine note after answered MCQ

**Status:** done
**Goal:** From an answered MCQ, open Refine note beside View Memory Tracker; reuse assimilation refinement; pass the question into layout breakdown so question-led points are separate items and preselected for extract/remove.

## Locked design

| ID | Decision |
|----|----------|
| D-01 | Reuse assimilation `NoteRefinement` via shared `RefineNoteModal`. |
| D-02 | Schema flag `ledToQuestion` on `NoteRefinementLayoutItem`; frontend preselects those items after layout load when question context is present. |
| D-03 | Optional `NoteRefinementQuestionContextDTO` on generate/export POST. Absent body = assimilation path unchanged. |
| D-04 | Scope: answered **MCQ** only. Spelling answered refine deferred. |
| D-05 | Load full `Note` via `getNoteRealmRefAndLoadWhenNeeded`; hide Refine when content blank. |

## Deferred

- Refine on answered spelling questions
- Extra UI badge for `ledToQuestion` beyond checkbox preselection
- Passing question context into extract/remove prompts

## Phases

| # | Type | Status | Outcome |
|---|------|--------|---------|
| 1 | Behavior | done | Refine note entry from answered MCQ |
| 2 | Structure | done | Shared `RefineNoteModal` |
| 3 | Structure | done | `ledToQuestion` on layout item schema |
| 4 | Structure | done | Optional MCQ context on layout generate API |
| 5 | Behavior | done | Pass question + preselect `ledToQuestion` |
| 6 | Behavior | done | E2E `e2e_test/features/recall/refine_note_after_mcq.feature` |

## E2E note (Phase 6)

Stub refinement layout **before** MCQ/evaluation stubs — layout Given restarts the OpenAI imposter.
