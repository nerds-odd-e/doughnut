# Replace frontend unit-test mocks of internal code with the real modules

**Identity:** SEED-039#internal-mocks-to-real-modules

## Source

[Replace frontend unit-test mocks of internal code with the real modules](../../seeds/SEED-039-faster-ci-feedback.md#internal-mocks-to-real-modules),
refined on 2026-10-04. The story holds the inventory, the allowed mocks, and
the key examples.

## Goal and scope

Contributors can trust a passing or failing frontend unit test: the 52 spec
and support files in `frontend/tests` that `vi.mock` in-process code run the
real modules instead, and each test keeps the behavior it checked.

- **Included:** removing `vi.mock` of `vue-router`, `usePopups`,
  `useGoToNextAssimilation`, `vue-toastification`, `useToast`,
  `timezoneParam`, and `loginOrRegisterAndHaltThisThread`; shared test support
  for a clean start; deleting `src/components/commons/Popups/__mocks__/usePopups.ts`
  and support code only a removed mock used; naming the allowed mocks in
  `.agents/skills/frontend-testing/SKILL.md`.
- **Excluded:** the allowed mocks (AI reply stream, audio recorder, recorder
  worklet, wake locker, `file-saver`, `pdfjs-dist`); `vi.spyOn` on real
  objects; a lint rule or count limit; production code changes, unless a real
  module cannot run in a spec without one (stop and report if that happens).
- **Assumption:** every slice changes test files only, so behavior is
  preserved when the same tests pass with unchanged test names and without
  weaker assertions.

## Outside-in proof

- **Focused check per slice:**
  `env -u NODE_ENV CI=true CURSOR_DEV=true nix develop -c pnpm -C frontend test <the slice's spec files>`,
  then the typecheck the `frontend` skill's "Frontend proof" rule requires.
  No test may pass only on retry.
- **No mock left in the slice's files:**
  `grep -nE 'vi\.mock\("(vue-router|vue-toastification|@/components/commons/Popups/usePopups|@/composables/(useGoToNextAssimilation|useToast)|@/managedApi/window/)' <the slice's files>`
  prints nothing.
- **Whole suite (slice 13):**
  `env -u NODE_ENV CURSOR_DEV=true nix develop -c pnpm frontend:test`. It is
  required once at the end because slices 1 and 6 add shared helpers and
  cleanup that other specs load.
- **Hosted CI:** both `Frontend Unit Tests` shards pass after publication.

| Promise | Slice | Proof |
| --- | --- | --- |
| Popups: real popup stack, spec reads and answers the popup | 1–4 | focused checks pass; no `usePopups` mock in those files |
| Clean start for popups in one helper | 1 | helper in `tests/helpers`; existing ad hoc resets use it |
| Routing: real router, navigation read from current location (ADR 0005) | 3, 4, 6, 9–12 | focused checks pass; no `vue-router` mock in those files |
| Next assimilation: real composable with `AssimilationController.next` through `mockSdkService` | 2, 5 | focused checks pass; no mock in those files |
| Toasts: real library, message read on the page | 6, 7, 8 | focused checks pass without retry; no toast mock in those files |
| Time zone through `mockBrowserTimeZone` | 6 | `useGoToNextAssimilation.spec.ts` passes |
| Sign-in redirect observed at `browserLocation` | 8 | `clientSetup.spec.ts` passes |
| `__mocks__/usePopups.ts` deleted | 13 | file gone; whole suite passes |
| Frontend testing skill names every allowed mock | 13 | skill text; `grep -rhoE 'vi\.mock\("[^"]+' frontend/tests \| sort -u` lists only those modules |
| Speed reported | 13 | wall time compared with the baseline below |

**Baseline (2026-10-04, `e50b41cdae`, 16-core macOS, idle, no `CI`):** the
whole-suite command above ran 320 files, 2014 tests, all passing, in 59.8 s
and 63.5 s wall time (Vitest duration 53.3 s and 60.4 s).

## Observed premises

| Premise | Consumed by | Observation (2026-10-04, `e50b41cdae`) | Result |
| --- | --- | --- | --- |
| A `useRoute`/`useRouter` stub that only silences can be replaced by the helper's real router | slices 3, 4, 9–12 | In `WikidataSearchByLabel.spec.ts`, removed the `vue-router` mock and added `.withRouter()` before `.mount`; ran the spec | 7 tests pass |
| With the toast plugin on the mounted app, the message is on the page | slices 6–8 | Throwaway spec: `render(C, { global: { plugins: [[Toast, {}]] } })`, call `showSuccessToast`, `expect.element(page.getByText(...))` | Text found, but the first attempt timed out and passed on retry |
| Toasts do not leak between tests | slices 6–8 | Same throwaway spec, second test mounting without the plugin | **False.** The container stays on the page and earlier and later toasts accumulate. The clean start must clear toasts, and slice 6 must make the assertion pass without retry |
| Specs can read and answer the real popup stack with no popup component mounted | slices 1–4 | `usePopups().popups.peek()` / `.done(...)` already used in `NoteRefinement.extractNote.spec.ts`, `assimilationPanelTestSupport.ts`, `folderPageTestSupport.ts` | Holds |
| Bare `usePopups` mocks are not only silencing | slices 1, 3 | Removed the 13 bare mock lines and ran those specs | Fails at `vi.mocked(usePopups).mockReturnValue` in `setupPopupsMock` (`noteEditableContentTestSupport.ts`) and in `RecallPage.answering.spec.ts`; those callers must be rewritten, not only the line removed |
| The real sign-in redirect never resolves | slice 8 | Read `loginOrRegisterAndHaltThisThread.ts`; `clientSetup.ts:185` calls it without awaiting; `browserLocation.assign` is spied in `loginOrRegisterAndHaltThisThread.spec.ts` | Observe `browserLocation.assign`; it also calls `healthcheckPing`, which the spec must answer through its fetch mock |

## Slices

Every slice is Type: Structure. It removes one weakness, a module mock of
in-process code, from one family of specs and proves the same tests still
pass. Slices are independent after slice 1 (popup helper) and slice 6 (toast
helper); any one is a safe stopping point. Sizing: a slice covers one spec
family because its files share one support file and one focused check;
splitting a family would leave that support file half converted. Slices 1 and
3 are the largest (seven and six specs) and may pass the 10-minute mark for
that reason.

### 1. NoteEditableContent specs answer the real popup stack
Type: Structure
Status: planned
Proof: the seven `tests/notes/NoteEditableContent*.spec.ts` files pass; no
`usePopups` mock in them or in `noteEditableContentTestSupport.ts`.

- Add one helper in `tests/helpers` that empties the real popup stack before
  each test (PFE: the pattern exists ad hoc as
  `usePopups().popups.register({ popupInfo: [] })` in
  `noteMoreOptionsTrashTestSupport.ts` and as a `done(false)` loop in
  `NotebookAttachedBookSection.spec.ts`; move both onto the helper).
- Replace `setupPopupsMock`: where a test set a `confirm`/`options` answer,
  the test triggers the action, reads the pending popup (type, message,
  options) from `peek()`, and answers with `done(...)`. "Not called" becomes
  "the stack is empty".

### 2. Property memory-tracking specs run real popups and next assimilation
Type: Structure
Status: planned
Proof: `usePropertyMemoryTrackerGuard.spec.ts`,
`usePropertyMemoryTrackerGuard.listProperty.spec.ts`,
`RichMarkdownEditor.propertyMemoryTracking.spec.ts`, and
`RichMarkdownEditor.listPropertyMemoryTracking.spec.ts` pass with no mock of
`usePopups` or `useGoToNextAssimilation`.

These factories answer `confirm` with a fixed value; answer the real popup
instead. Where the real `goToNextAssimilation` can run, give
`AssimilationController.next` through `mockSdkService` and mount with the
helper's router.

### 3. RecallPage specs run a real router and real popups
Type: Structure
Status: planned
Proof: the six `tests/pages/RecallPage*.spec.ts` files pass with no mock of
`vue-router` or `usePopups`.

Mount with a real router placed at the `recall` location (in
`recallPageTestSupport.ts`). The frequent-failure warning tests in
`RecallPage.answering.spec.ts` read the alert or confirm from the real stack.

### 4. Note-creation specs run a real router and real popups
Type: Structure
Status: planned
Proof: `NoteNewForm.spec.ts`, `NoteNewForm.submit.spec.ts`,
`NoteNewButton.spec.ts`, `FolderNewForm.spec.ts`, and
`NoteUnresolvedWikiLinkModal.spec.ts` pass with no mock of `vue-router` or
`usePopups`.

Navigation after submit is asserted as the router's current named location
(`noteShowLocation` and the like), not as a captured `push`.

### 5. MainMenu and AssimilationPanel specs run the real next-assimilation action
Type: Structure
Status: planned
Proof: the three `tests/toolbars/MainMenu*.spec.ts` files,
`AssimilationPanel.spec.ts`, and `AssimilationPanel.trackers.spec.ts` pass
with no mock of `useGoToNextAssimilation`; `mainMenuTestSupport.ts` no longer
calls `vi.mocked(useGoToNextAssimilation)`.

A test that asserted the action was called asserts its result: the router is
at the next note's location, given through
`mockSdkService(AssimilationController, "next", ...)`. The
`AiReplyEventSource` mock in the MainMenu specs is an allowed mock and stays.

### 6. useGoToNextAssimilation spec runs the real router, toast, and time zone
Type: Structure
Status: planned
Proof: `tests/composables/useGoToNextAssimilation.spec.ts` passes on the first
attempt with none of its three mocks.

- Time zone: `mockBrowserTimeZone("Asia/Shanghai", beforeEach, afterEach)`.
- Toast: add one helper in `tests/helpers` that installs the toast plugin on
  the mounted app and clears toasts before each test. Assert the message text
  on the page. This slice is the probe for the toast premise: if the
  assertion cannot pass without retry, stop and report before slices 7 and 8.
- Navigation: the router's current location.

### 7. Component specs read real toasts on the page
Type: Structure
Status: planned
Proof: `NoteMoreOptionsForm.spec.ts` (with `noteMoreOptionsTrashTestSupport.ts`
and the specs that load it), `NoteRefinement.cancel.spec.ts`,
`BookReadingPdfAiReorganize.spec.ts`, and `BookReadingPdfUndoShortcut.spec.ts`
pass with no mock of `vue-toastification`.

Use slice 6's helper. `BookReadingPdfUndoShortcut.spec.ts` only silences the
toast; remove the mock.

### 8. clientSetup spec observes real toasts and the real sign-in redirect
Type: Structure
Status: planned
Proof: `tests/managedApi/clientSetup.spec.ts` passes with no `vi.mock`.

`clientSetup` shows toasts outside a component, so read them from the page
with slice 6's helper installed. The redirect is observed as
`browserLocation.assign` (a spy on the browser boundary, as
`loginOrRegisterAndHaltThisThread.spec.ts` does); answer `healthcheckPing`
through the spec's fetch mock.

### 9. NoteUndoButton specs run a real router
Type: Structure
Status: planned
Proof: the three `tests/toolbars/NoteUndoButton*.spec.ts` files pass with no
`vue-router` mock; `noteUndoButtonRouterMockExports` is deleted.

### 10. Wikidata, question-export, and relationship specs run a real router
Type: Structure
Status: planned
Proof: the three `WikidataAssociationDialog*.spec.ts` files,
`WikidataSearchByLabel.spec.ts`, `QuestionExportDialog.spec.ts`, and
`AddRelationship.spec.ts` pass with no `vue-router` mock.

`AddRelationship.spec.ts` and `WikidataAssociationDialog.titleActions.spec.ts`
assert `replace`/`push`; assert the current location instead.

### 11. Recall component specs run a real router
Type: Structure
Status: planned
Proof: `NoteRefinement.extractNote.spec.ts`, `ViewMemoryTrackerLink.spec.ts`,
`RecallSessionOptionsDialog.spec.ts`,
`MatchedNoteWikiLinkOrRelationshipOffer.spec.ts`, and
`tests/pages/MemoryTrackerPage.spec.ts` pass with no `vue-router` mock.

### 12. Conversation and global-bar specs run a real router
Type: Structure
Status: planned
Proof: `ConversationComponent.spec.ts`, `NoteConversation.spec.ts`,
`GlobalBar.spec.ts`, and every spec that loads
`tests/toolbars/horizontalMenuTestSupport.ts` pass with no `vue-router` mock.

### 13. Only the named allowed mocks remain
Type: Structure
Status: planned
Proof: `src/components/commons/Popups/__mocks__/usePopups.ts` is deleted; the
frontend testing skill names the allowed mocks from the story with one reason
each; the `sort -u` listing in *Outside-in proof* shows only those modules;
the whole suite passes; wall time is reported against the baseline under the
same conditions (two runs). A material slowdown is reported for an owner
decision, not tuned away here.

## Current decisions

- An in-process library that renders or navigates inside the page
  (`vue-router`, `vue-toastification`) is internal (story's boundary
  assumption).
- Routing assertions follow ADR 0005 and the frontend testing skill's routing
  rule: production `routes` or `dummyRouteRecordsFromMetadata`, named
  locations. No North Star topic applies and none is added.
- Run frontend tests with `env -u NODE_ENV`; the owner's shell sets
  `NODE_ENV=production`.
- Local gates follow the `unit-testing` and `frontend` skills and the
  execution wrap-up in `CLAUDE.md`.

## Learnings

None yet.
