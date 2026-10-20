# Command-bar continuity

**Identity:** SEED-080#story-1
**Source:** [Recover active navigation and finish command-bar interactions](../../seeds/SEED-080-command-bar-reconciliation.md#story-1)

## Goal and scope

A signed-in author recognizes the current activity, resumes recall directly,
and searches through the common dialog from a comfortable responsive command
menu. Preserve the source's activity precedence, counts, navigation destinations,
signed-out behavior, adaptive note commands, and shared search capabilities.
The owner accepted visible property-learning presentation as Assimilate context
on 2026-10-20, regardless of entry path.

This is preparation only; execution needs a separate instruction. Sidebar
toolbar relocation, breadcrumbs, note-reading layout, and off-route assimilation
priority remain deferred. No API, persistence, or scheduling-policy change is
needed. Counts and recall eligibility retain their existing domain rules.

## Architecture and reuse

- `useNavigationItems` owns navigation identity, precedence and counts;
  `NavigationItem` and `navigationActionItems` already distinguish context
  from direct actions. Recover first-active selection from historical
  `HorizontalMenu.vue` at `e4227c46a0fb8fcc2185769bb0ab9974588288aa`,
  using today's popup and shared header. Keep one classifier.
- `useResumeRecall` owns resume intent and spelling keyboard preparation;
  `RecallPage` consumes the intent. `useGoToNextAssimilation` owns next-unit
  loading, counts, messages, and note/property destinations. Call those owners.
- Change the existing assimilation presentation concept (`useAssimilationView`
  and its note/property presentation consumers) to expose the current visible
  activity to navigation. Reuse actual presentation eligibility and disclosure
  state. Merely being on `noteProperty`, mounting a panel hidden by `v-show`,
  or having a saved target note is insufficient. Do not add a parallel
  sequence-origin flag or duplicate property parsing in the menu.
- Reuse `SearchForNoteAndFolder`, `Modal`, `primeSoftKeyboard`, the focus
  directive, and the `note-search` shortcut. One dialog owner and opening
  operation serve both menu targets and the shortcut. The relationship
  `SearchForm` requires a note and is not the global-search entry.
- Keep note actions, their active state, and their measurements with
  `NoteToolbar`/`NoteToolbarMoreOptions`; adjust allocation/alignment in
  `GlobalBar` and the existing command row without moving their ownership.

Applicable Accepted decisions:
[ADR 0001 — domain vocabulary](../../../docs/adrs/0001-ubiquitous-language.md#assimilation),
[ADR 0005 — named locations and note route family](../../../docs/adrs/0005-web-routes-accepted.md),
and [ADR 0006 — failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage).
No conflict or exception. Existing
[North Star](../../NORTH-STAR.md) topics require no change for this local
presentation work. PFE found suitable owners above; the missing integration
is visible assimilation context reaching the existing navigation model.

## Outside-in proof ownership

All browser unit proof mounts real components/routes and mocks only backend
HTTP or existing allowed device boundaries. Fixture state supplies starting
conditions, never the result the example claims to establish.

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Ctrl/Cmd+F opens focused common global search; existing dialog and signed-out shortcut rules survive | 1 | Extend `DonutMenu.searchShortcut.spec.ts`: actual open dialog and focused input, both keys, menu-open entry, Escape/close, existing-modal and signed-out boundaries. Current shortcut tests alone do not distinguish a menu input from a dialog. |
| Both search-shaped targets close the popup and open that same dialog; truthful button semantics, focus indicator and touch preparation | 2 (native probe starts in 1) | Mounted menu keyboard/pointer interactions; coarse-pointer browser test observes primer focus followed by real dialog input focus. Native iPad observation below owns actual software-keyboard visibility and typing. |
| Recent/matching notes and folders, history, scope/list choices, keyboard result traversal, modified clicks and same-note selection retain their journey | 1–2 | Move the surviving `DonutMenu.search.spec.ts` scenarios to the dialog entry; retain `SearchResults*`, `SearchDialog.*`, and `NoteUnresolvedWikiLinkModal` coverage for shared consumers. Selecting the current note closes the dialog and exposes its content. Run `search_note.feature` after updating its entry helper. |
| Collapsed activity icon, applicable badges, ordinary-context menu opening, other destinations, fallback and signed-out entry | 3 | Mounted `DonutMenu`/`GlobalBar` and routed page fixtures: Note/Recall/Circles/Bazaar/Messages, no-active fallback, signed-out Login, counts and diligent styling. Assert the visible control's accessible identity and activation, not wrapper DOM. |
| Resume precedence, green state, exact paused/progress boundaries, direct action, preserved current question and independent menu opening | 3 | Extend menu Resume/count/keyboard cases, including paused loaded trackers at queue end. Start an actual recall journey, browse away, click the compact Resume, and observe the current question. Existing `RecallPage.answering` setup that sets `shouldResumeRecall` directly does not prove this click. Extend/run `browse_answer_and_notes_while_recalling.feature` through the compact action; spelling focus stays covered at the entry. |
| Current note/property assimilation, either property entry, visibility lifetime and next-unit destinations | 4 | Routed workspace tests open/close note assimilation and property learning, hide/reveal details, switch notes or leave the note family, and activate next assimilation. Observe icon/count/action and returned note/property; include ordinary missing/read-only/non-learning property browsing and Resume precedence. |
| Right alignment, order, measured overflow, pinned busy/open actions and group/content dividers | 5 | Extend `NotebookWorkspaceHeader` geometry at 320/390/768/1280 px; retained overflow and voice-input startup/recording tests plus actual active-panel resize. Inspect visible separators in bounded visual checks. |
| Spacious menu, footer separation and all commands reachable at narrow/short sizes | 6 | `DonutMenu.layout` measures panel containment and scroll reachability with expanded Account at 320/390/768/1280 px and short height; visual observation assesses padding, row separation and footer gap. |

### Consumer trace and removal reading

The search move reaches `donutMenuTestSupport.searchField`, search/keyboard/
shortcut/layout specs, `e2e_test/start/pageObjects/donutMenu.ts:startSearching`,
the search step definitions, and `search_note.feature`. The E2E helper currently
looks for an input inside the open menu. Update the entry and closure assertions
to observe the dialog; a closed menu alone does not prove search completion.
Keep result/scoping/duplicate-title consumers on shared search.

The context control reaches `donutMenuLaunchersIn`, `GlobalBar.spec.ts`, routed
notebook/book pages, recall progress aggregate text, and
`e2e_test/start/pageBase.ts` plus assimilation/recall/message helpers. Preserve
an explicit accessible menu opener and scope activity queries to the compact
control or popup so repeated labels do not make them ambiguous.

Deletion belongs to slice 2: embedded-menu search presentation, its exclusively
used quiet/entered gating if the final caller trace confirms exclusivity, and
dependent tests/helpers/styles/declarations. Preserve search history, global
scope, result lists, keyboard navigation and relationship/dead-link consumers.
Correct the inherited assertions per purpose: move surviving search journeys
into the dialog, retain real authorization boundaries, delete assertions whose
only subject is the retired presentation. Do not replace them with absence
tests.

At slice 2 acceptance, perform a one-time whole-product search for the exact
retired identifiers/call forms and embedded-search instructions discovered in
the diff, including frontend, E2E, backend, CLI, MCP, docs, guidance and generated
artifacts. Classify shared names and preserve dated observations with their
original provenance. Record this as a reading here, separate from proof; add
no permanent absence checker. Regenerate component declarations if component
files change.

## Premises and preparation observations

Checkout: `/Users/terryyin/git/doughnut/.worktrees/recover-active-navigation-and-finish-command-bar`.
Base: `c64063489126d137d32fb7f4ab3027a26756dd1f`.
The seed's dated investigation preserves literal commands, setup, results and
limits for the 34 navigation/search/header/next-unit tests, 16 property tests,
the reproduced missing compact Resume, modal search reuse and same-note close,
and the 352.5 px right-alignment gap. A scratch alignment change discriminated
the geometry check and passed 15 retained header/overflow cases.

Additional observations on 2026-10-20, at that same base:

- A scratch `DonutMenu` replaced embedded search with two buttons opening one
  real `Modal`/`SearchForNoteAndFolder`; the existing shortcut used the same
  operation. A temporary coarse-pointer test mounted the real keyboard primer,
  activated each button, observed primer then input focus and Recent notes,
  and closed the dialog. Both cases passed.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/layouts/propertyActivityPreparationProbe.spec.ts`
  ran that scratch candidate: 96 passed, 12 failed. All 12 failures were the
  11 embedded-search entry assumptions in `DonutMenu.search.spec.ts` and the
  search-input short-menu case in `DonutMenu.layout.spec.ts`. This identifies
  concrete consumers to migrate; no failed test was accepted as product proof.
  The 6 shortcut cases passed even after the presentation changed, confirming
  that they need the explicit dialog assertion specified above.
- In the same run, a real routed note with `topic: training` at
  `noteProperty` showed its learning controls, hid them when note details
  were collapsed, showed them again on reveal, and returned to ordinary note
  routing. The panel remains mounted while hidden: use disclosure state,
  not mount lifetime, for current activity.
- Removing the scratch opener's `primeSoftKeyboard()` call and running
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/searchPreparationProbe.touch.spec.ts`
  failed both cases at the primer observation. That proof discriminates the
  focus preparation, but Chromium touch emulation cannot prove an iPad OS
  keyboard. All scratch product and probe files were removed and
  `git diff --exit-code -- frontend` passed before this plan write.
- With original product code restored,
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/search_note.feature`
  passed all 6 scenarios in this checkout's isolated stack. It reached
  authenticated same-note Recent/Matches navigation, title search/ranking and
  duplicate-title feedback. This establishes the local proof route and
  baseline; slice 2 must run it again through the new entry and dialog-close
  observation.
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature`
  passed both scenarios on restored product code: browse away during a
  spelling recall and return to the current question, and remove/revive a
  tracker. The current Resume helper enters through the popup. Slice 3
  changes that trigger to the compact control and retains the observed
  question outcome; this baseline does not prove the proposed compact click.

Native iPad keyboard behavior is the remaining owner-device premise. Its cheap
focus path has been observed above. Slice 1 owns the permitted early native
probe: with the candidate common dialog reachable on an iPad, tap its entry,
observe the software keyboard, type a search and open a result. Use the isolated
unpaid stack described in `docs/worktree-browser-tests.md`; the developer owns
access to the device. In slice 1 a disposable local tap trigger can invoke the
same opening operation; remove it before delivery, and in slice 2 repeat on
both final menu targets. Failure or unavailable device evidence stops acceptance
of that touch promise and dependent launcher completion, and requires a plan
update; independent navigation/layout slices may proceed. Do not substitute
Chromium input focus or a screenshot of an empty dialog for keyboard operation.

## Ordered slices

### 1. Open focused global search in the common dialog
Type: Behavior
Status: planned
Proof: Shortcut/dialog/search cases in the proof table; early native input probe.

Behavior: Signed in on any supported page → press Ctrl/Cmd+F → the common
global search dialog opens focused, offers the existing search journey, and
closes through its normal controls or result navigation.

Use one opening operation and retain synchronous touch focus preparation for
the menu entries delivered next. Establish the native input route before
broadening dependent search work. Interim: menu search keeps its current
presentation until slice 2; record that interim obligation during execution,
owned by slice 2. Target 5–8 minutes of local work including focused checks;
owner-device access/wait is an explicit external-wait exception.

### 2. Open that search dialog from the popup menu
Type: Behavior
Status: planned
Proof: Both launcher targets, keyboard/touch access, dialog result journeys,
shared-search regression checks, `search_note.feature`, and separate removal reading.

Behavior: Open the menu → activate either the empty search-shaped button or
its search button → the popup closes and the same focused search dialog opens.
Complete the consumer migration and transitive deletion described above in this
slice. The apparent field is not an editable textbox. Target 5–8 minutes;
scrutinized because proof migration belongs to the same entry change. Slow
isolated Cypress startup is an external-wait exception, not permission to
leave its observation unrun.

### 3. Recognize the current activity and resume recall directly
Type: Behavior
Status: planned
Proof: Compact context, fallback, signed-out/menu tests and real recall continuity,
including `browse_answer_and_notes_while_recalling.feature`.

Behavior: Browse with the menu closed → see the existing active icon/count;
activate ordinary context to open navigation, or eligible green Resume to
return directly to the preserved current recall question. The explicit menu
opener works independently in either state.

Keep exact eligibility in `useNavigationItems`: paused cursor plus loaded
trackers, or progress beyond the first item off recall with remaining work.
Do not replace the paused-cursor branch with a remaining-count test. Preserve
the normal destinations, fallback, counts and diligent styling. Target 5–8
minutes; direct action and menu opening form one control contract. Existing
recall Cypress runtime may need the same external-wait exception.

### 4. Identify the currently presented assimilation activity
Type: Behavior
Status: planned
Proof: Routed note/property presentation and next-unit observations in the table.

Behavior: Open note assimilation or a property-learning panel by either entry
path → see Assimilate and its existing badge/direct next action; leave/hide
that presentation → see the appropriate ordinary context, with eligible Resume
still taking precedence.

Extend the existing shared presentation concept and its consumers as one
change. Actual property learning eligibility, detail disclosure, note identity,
and page lifetime determine current context. Include read-only/missing/
non-learning browsing and a next-unit transition from note to property. Target
5–10 minutes; scrutinized because two presentations share one domain outcome.
If exposing that common concept needs more than the hard limit, stop and
refine this slice around the newly evidenced responsibility before continuing.

### 5. Align and distinguish the command groups
Type: Behavior
Status: planned
Proof: Workspace header geometry, overflow and busy/open-operation retention,
plus bounded phone/iPad/desktop visual observation.

Behavior: Open/resize a note while an action is active → commands align to
the right of their allocated area, retain order and measured overflow, and
subtle navigation/group/content boundaries remain visible.

Keep the existing action state and measurements, including pinned busy/open
controls and ellipsis. Measure with the expanded activity controls now present.
Target 4–6 minutes; no sidebar-toolbar relocation or breadcrumb redesign.

### 6. Keep a spacious menu reachable on narrow and short screens
Type: Behavior
Status: planned
Proof: Panel geometry/scroll/keyboard reachability and bounded visual observation
of spacing, footer separation and group boundaries with final controls.

Behavior: Open the popup and expand Account on phone/iPad/desktop or a short
viewport → destinations, search launchers and the footer have comfortable
separation and remain reachable through the panel's scroll area.

Extend layout checks using the final search launchers and account contents;
keep the footer's extra vertical gap. Target 4–6 minutes. This slice owns
final combined visual acceptance, not unrelated layout work.

## Verification and delivery

Follow `AGENTS.md`, `unit-testing`, `frontend`, the matching frontend rules,
and `e2e-authoring` for changed Cypress consumers. Use the whole affected
toolbar suite (the observed run took 7 seconds), not a hand-picked subset
that hides shared helper failures:

```sh
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/layouts/NotebookWorkspaceHeader.spec.ts tests/notes/NoteToolbar.overflow.spec.ts tests/notes/NoteToolbar.voiceInputOverflow.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/search tests/wiki-link-or-relationship tests/notes/NoteUnresolvedWikiLinkModal.spec.ts
CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/search_note.feature
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
```

Add the new context and touch specs to their owning command. Run the affected
page/consumer suites identified above when their shared control changes; reuse
valid evidence for unchanged boundaries. Typechecks are required by the
frontend skill at acceptance, including generated types. Component additions
or deletions require the Vite build to regenerate `frontend/components.d.ts`.
No API generation is expected; use its normal workflow if a real contract
change becomes necessary. No full backend or paid-service run is required.

The active slice requests bounded manual visual observation using
`dough-manual-testing` at the listed widths; native iPad input has the separate
early probe. Save observed setup, screenshots/results and limitations through
the normal execution evidence, never claim unobserved OS behavior.

Each execution slice includes Jidoka, a fresh `dough-post-change-refactor`
agent, coordinator-only `./scripts/run.sh pnpm format:changed` once, plan update,
agent commit with check-only hook, push and owned CI follow-up under
`dough-execute-plan`. These are execution gates, not permission to implement
from this preparation. Local proof precedes publication.

The sequence uses one navigation model, one assimilation-presentation concept,
one search dialog and existing command ownership; it adds no per-slice product
abstractions. Each slice is safely useful before the following one, with the
search interim explicitly assigned. Target roughly five minutes; scrutinize
longer work, and stop/redecompose at ten minutes unless the stated external
test/device wait applies. Do not use that exception for growing implementation.

## Current decisions and learnings

- The owner's property-context choice is settled; no sequence-origin policy.
- Actual visible learning presentation, not property route or mounted DOM,
  is the shared context. Details disclosure uses `v-show`.
- Existing shortcut assertions do not distinguish menu search from a dialog;
  add that observation before accepting the move.
- Existing Resume route/intention checks alone do not establish continued
  question/cursor behavior; the real recall journey owns that observation.
- Search-presentation deletion and retained search behavior have different
  proof ownership; keep the one-time sweep separate.
- Preparation review found no unresolved slice-boundary or cumulative-design
  concern after these cuts. An additional slice-plan-refinement pass is not
  needed; the native device premise is explicitly bounded by the early probe.
