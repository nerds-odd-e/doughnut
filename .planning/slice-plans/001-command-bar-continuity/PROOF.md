# Command-bar continuity proof

**Identity:** SEED-080#story-1
**Plan:** [Command-bar continuity](PLAN.md)

## Outside-in proof ownership

All browser unit proof mounts real components/routes and mocks only backend
HTTP or existing allowed device boundaries. Fixture state supplies starting
conditions, never the result the example claims to establish.

| Promise | Slice | Observable proof |
| --- | --- | --- |
| Ctrl/Cmd+F opens focused common global search; existing dialog and signed-out shortcut rules survive | 1 | Extend `DonutMenu.searchShortcut.spec.ts`: actual open dialog and focused input, both keys, menu-open entry, Escape/close, existing-modal and signed-out boundaries. Current shortcut tests alone do not distinguish a menu input from a dialog. |
| Both search-shaped targets close the popup and open that same dialog; truthful button semantics, focus indicator and touch preparation | 2 | Mounted menu keyboard/pointer interactions; coarse-pointer browser test observes primer focus followed by real dialog input focus. The owner waived native iPad observation as an acceptance dependency; actual software-keyboard visibility and typing remain unverified. |
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

