# API-driven blocking loading plan

## Goal

Make whole-UI blocking spinners a cohesive API-call concern instead of local component state. The first user-facing target is assimilation: the UI should be blocked while fetching the next item to assimilate and while keep/skip assimilation calls are in flight.

## Discoveries

- `apiCallWithLoading` already owns API loading and error-toast behavior, but it only drives the thin global bar through `apiStatus.states`.
- Whole-UI blocking exists today through ad hoc `LoadingModal` refs in individual components.
- Assimilation has two async paths:
  - start/find next: `useGoToNextAssimilation()` calls `AssimilationController.next`
  - keep/skip: `useAssimilateUnit()` calls `AssimilationController.assimilate`, then calls `goToNextAssimilation()`
- Starting assimilation from the menu, horizontal menu, and home learning flow all go through `useGoToNextAssimilation()`.
- Phase 1 introduced API-managed blocking state by tokenizing `apiStatus.states`; the thin loading bar still keys off `states.length`, while the global modal reads the most recent blocking state.
- The assimilation walkthrough now observes the start-assimilation blocker by delaying `/api/assimilation/next` once in the menu-start page object.
- Existing whole-screen blocking users include relationship-note creation, note deletion/reduction, book upload, and note refinement AI actions.
- Subtle partial blockers exist in book reading layout mutation/reorganization. They are API waits, but currently block only a row or the book-layout area; include them only if the product decision is that every mutating API wait should block the whole UI.

## Key design decisions

- Extend the managed API loading system with a blocking channel rather than introducing another component-level wrapper.
- Prefer an option on `apiCallWithLoading`, such as `{ blockUi: true, message?: string }`, so call sites keep error handling, thin-bar loading, and blocking behavior in one place.
- Keep stack/token semantics for concurrent calls. The current `states.push/pop` pattern works for counts but should not rely on blind pop if blocking entries need messages.
- Render one global `LoadingModal` from `DoughnutApp.vue` based on API status. Component-specific `LoadingModal` instances should disappear as callers move to the API option.
- Keep local inline spinners for intentionally non-blocking surfaces such as search, export refresh, pagination, form-field upload, and recall prompt internals.

## Phases

### Phase 1 - Block while starting assimilation from user entry points

Status: done

Behavior: When the user starts assimilation from the menu/home flow, the whole UI shows the blocking spinner until `AssimilationController.next` resolves and the target note is shown or the no-more-notes toast is emitted.

Implementation notes:
- Add blocking state support to `ApiStatus` / `ApiStatusHandler`.
- Add `blockUi` and optional `message` support to `apiCallWithLoading`.
- Mount global `LoadingModal` in `DoughnutApp.vue`.
- Mark the `AssimilationController.next` call in `useGoToNextAssimilation()` as blocking.

Tests:
- Extend `frontend/tests/managedApi/clientSetup.spec.ts` for blocking state, nested calls, concurrent calls, and cleanup on errors.
- Add or extend a mounted frontend test around `useGoToNextAssimilation()` or menu entry to assert the global modal appears while the mocked `next` API is pending.
- If E2E step support is practical, extend `e2e_test/features/assimilation/assimilation_walkthrough.feature` with an observable blocking-spinner step for "start assimilation from the menu".

### Phase 2 - Move assimilation keep/skip blocking onto the API mechanism

Status: planned

Behavior: After clicking keep-for-recall or skip-recall on the assimilation panel, the whole UI remains blocked through the assimilate API and the next-assimilation API. The next item is not interactable before it is fully shown.

Implementation notes:
- Mark `AssimilationController.assimilate` in `useAssimilateUnit()` as blocking.
- Keep `goToNextAssimilation()` blocking from Phase 1 so the post-assimilate next fetch is covered by the same mechanism.
- Remove `isAssimilating` and the local `LoadingModal` from `AssimilationPanel.vue`.
- Preserve `assimilatingPropertyKey` only if it is still needed for row-level disabled state; otherwise simplify it separately only if tests prove it is redundant.

Tests:
- Update `frontend/tests/components/recall/AssimilationPanel.loadingModal.spec.ts` to assert the global modal behavior rather than component-local state.
- Extend existing assimilation panel tests for both keep and skip if one path is uncovered.
- Run targeted E2E for `e2e_test/features/assimilation/assimilation_walkthrough.feature`.

### Phase 3 - Migrate existing whole-screen blockers to the same API option

Status: planned

Behavior: Existing whole-screen blocking interactions still block the UI, but the blocking is driven by API-call options rather than manual local refs.

Scope:
- Relationship-note creation in `AddRelationshipFinalize.vue`.
- Note delete / relationship reduce in `useNoteDeleteFlow.ts` and `NoteMoreOptionsActions.vue`.
- Book attach upload in `NotebookAttachedBookSection.vue`.
- Note refinement AI extract/remove in `NoteRefinement.vue`.

Implementation notes:
- Replace local `isCreatingRelationshipNote`, `isDeletingNote`, `isAttachingBook`, `isExtractingNote`, and `isRemovingSuggestions` modal state with blocking API calls or a small composite helper when the operation spans multiple API calls.
- Preserve operation-specific messages.
- If an operation has validation or confirmation before the API call, show the blocker only after the user confirms and immediately before the API begins.

Tests:
- Update existing tests that already assert `.loading-modal-mask`:
  - `frontend/tests/links/AddRelationship.spec.ts`
  - `frontend/tests/notes/NoteMoreOptionsForm.deleteNote.relationship.spec.ts`
  - `frontend/tests/components/notebook/NotebookAttachedBookSection.spec.ts`
  - `frontend/tests/components/recall/NoteRefinement.extractNote.spec.ts`
  - `frontend/tests/components/recall/NoteRefinement.removeSuggestions.spec.ts`
- Prefer assertions from the user surface: modal visible while API is pending and gone afterward.

### Phase 4 - Decide and migrate subtle partial blockers

Status: planned

Behavior: If the desired product rule is "mutating API calls block the whole UI", book-layout row/panel blockers move to the same global API blocking mechanism. If the desired rule is narrower, document why these remain local.

Scope candidates:
- Book layout indent/outdent/cancel in `useBookLayoutMutations.ts`.
- AI book layout suggestion/apply in `useBookLayoutAiReorganize.ts`.

Implementation notes:
- `cancelBookBlock` currently bypasses `apiCallWithLoading`; wrap it consistently even if it remains local.
- Avoid removing useful row-specific affordances unless the global blocker fully replaces them from a UX perspective.

Tests:
- Update `frontend/tests/components/book-reading/BookReadingBookLayout.spec.ts` only if local overlays are intentionally removed.
- Add managed API or mounted component coverage for any newly global blocking calls.

### Phase 5 - Documentation and guardrails

Status: planned

Behavior: Future frontend API work has a clear convention for when to use blocking versus non-blocking loading.

Implementation notes:
- Update `.cursor/rules/frontend-api.mdc` to document `apiCallWithLoading(..., { blockUi: true })`.
- Include guidance that local spinners are for non-blocking inline progress, while whole-UI blockers should be API-driven.

Tests:
- No new behavior tests expected; run relevant frontend lint/test targets touched by the documentation examples if code snippets are type-checked elsewhere.
