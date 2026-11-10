# Slice 1 implementation return

Uncommitted implementation complete, about 6 minutes active including proof. No commits, pushes, refactor, selective formatter or lint commands. Coordinator-owned PLAN/source-review edits preserved.

## Outcome and ownership

- `frontend/src/components/recall/NoteUnderQuestion.vue`: existing viewer-scoped store read supplies notebook metadata and full source folders; compact trail/title/property links remain while it loads. Header always precedes the default preview slot. Local wrapping rules constrain this recall presentation alone.
- `frontend/src/components/recall/NoteUnderQuestion.stories.ts`: fixture seeds matching note identity, notebook and folders into the existing store so stories display the full location.
- `frontend/tests/components/recall/NoteUnderQuestion.location.spec.ts`: five new real-CSS browser cases for all four contexts, narrow/deep/long trails, spelling header order and preview body, root location and real-router notebook/folder navigation.
- Existing specs adjusted: `AnsweredSpellingQuestion.spec.ts`, `NoteUnderQuestion.spec.ts`, `RecallPage.spelling.spec.ts`; retained actual spelling preview observations instead of incidental global title/notebook text counts.
- `frontend/tests/pages/memoryTrackerPageTestSupport.ts`, `memoryTrackerPageViewTestSupport.ts`: new header-read HTTP fixtures, without stubbing store or renderer. Tracker page API setup matches note identity/title.

Every edited/new implementation file is below 250 lines. No API signature/component identity changed, so no generation trigger.

## Accepted focused proof candidates

Command Q:

```sh
./scripts/run.sh pnpm frontend:test tests/components/recall/Answered tests/components/recall/NoteUnderQuestion tests/components/conversation/ConversationComponent tests/pages/RecallPage tests/pages/MemoryTrackerPage tests/notes/NoteContextReader.spec.ts tests/pages/NoteShowPage.propertyItem.spec.ts
```

Terminal result: **35 files / 155 tests passed**, no unhandled errors, 22.15s. Log `/tmp/seed117-slice1-tests.log`.

Setup: browser-mode Chromium with production CSS; only backend HTTP mocked through `mockSdkService`/`mockShowNote`. New geometry cases construct a 280px host and source realm with notebook `Learning notebook`, three ordered folders including 96-character unbroken final folder, and a long title. Actual mounted contexts are AnsweredQuestionComponent, AnsweredSpellingQuestion, MemoryTrackerPageView and ConversationComponent. Nothing substitutes the shared header, store or breadcrumb.

Observations mapped to source commitments:

| Ids | Trigger/setup → expected result → observing location |
| --- | --- |
| C01,C05,C06 | Open each of four contexts → notebook/folders/title in order → `NoteUnderQuestion.location.spec.ts`, `wraps the full deep location inside a narrow %s context`, exact anchor-text array. |
| C08,C09,C10,E01,E02 | 280px host, three folders with unbroken name, long title, actual conversation subject pane → every text range inside card and no horizontal trail overflow → same parameterized case, `expectReadableTrail` range/clientWidth assertions. |
| C07 | Click notebook and folder anchors through production router → respective notebook/folder destinations → `shows notebook and title at the root and follows notebook and folder destinations`. All folder anchors share BasicBreadcrumb's existing destination rule; title routing remains existing slice-2 interim behavior. |
| C11,E03 | Root source has no folders → notebook/title anchor array → same root/navigation case. |
| C27,C28,C29 | Existing spelling reader remains read-only with body, properties, image, inbound references and usable wiki links → selected `NoteContextReader.spec.ts`, `shows the note's title, location, properties, and body read-only` and `shows the note image, usable wiki links, and every inbound reference`; `RecallPage.spelling.spec.ts` retains real preview property/body/reference assertions. `AnsweredSpellingQuestionAccidentalMatchNoteContext.spec.ts` retains focused property and reviewed source observations. |
| C30,E08 | Whole-note spelling → full header above retained body → spelling branch of new geometry case asserts header bottom <= preview top and `Preserved preview body.`; `AnsweredSpellingQuestion.spec.ts` also checks header notebook/title plus actual preview body. |
| C31 | Existing result/tracker/conversation interactions → retained outcomes → all selected affected suites, notably AnsweredQuestionComponent refinement flow, MemoryTrackerPage/View removal/revive/history/prompt tests, ConversationComponent refinement/exclusion/maximize/minimize tests, AnsweredSpellingQuestion accidental-match resolve/reveal tests. |

Typecheck commands T, both terminal exit 0 on current content:

```sh
./scripts/run.sh pnpm -C frontend exec vue-tsc --noEmit
./scripts/run.sh pnpm -C frontend exec tsc --noEmit
```

`scripts/check_diff_whitespace.sh` terminal exit 0. Current source passed T after correcting an initially unknown Storybook args type via an explicit typed fixture cast. Geometry focused iteration separately passed 1 file / 5 tests, then all five were included in Q above.

## Diagnosis and consumer trace

Initial Q had all 150 existing tests pass but five unhandled header-load errors from MemoryTrackerPage tests. Cause was the newly required showNote HTTP response missing from that page's API setup. Added the matching fixture there; corrected Q passed 155 tests without errors. No failure was waived or attributed away.

[Generated actual-diff consumer trace](consumer-trace-slice1.md) includes supplied effect terms and dispositions for every listed consumer. Changed enclosing observations were inspected beyond selector matches. The E2E accidental-match helper checks alert/resolve and reviewed-source presence, not global title count; equivalent mounted observations remain green, so its observed boundary is unchanged. The property feature has distinct notebook/title names and retains existing title/property/raw identity behavior at this slice. No Cypress result is claimed; its changed source navigation is slice 2 proof.

Host: this Mac; load observed before additional proof 6.18/10.38/15.90, falling from earlier activity. Bounded Q and two typechecks were selected, no Cypress stack or other heavy owned work. All owned command results observed to completion. No deferred required slice-1 proof.

## Remaining scope

No uncovered slice-1 promise or new loss. Existing title-to-focused-property navigation remains for slice 2, and raw reviewed-value presentation remains for slice 3, exactly as the ordered plan's safe interim states specify. Source title/current note semantics were not broadened in this slice.
