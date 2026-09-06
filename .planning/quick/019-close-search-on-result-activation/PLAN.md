# Close search when the user opens its current note

## Source and goal

Status: planned; ready for direct execution. Planning only; no implementation yet.

The user selected both findings from the 2026-09-06 manual UAT for fixes.
This plan owns finding 2. Finding 1 (access-denial classification) is complete;
this independent search fix is ready for execution.

As a learner, activating a search result reveals the selected note even when I
am already viewing it, so I do not have to dismiss the search overlay separately.

Reproduction at `f0fd4bed63`: view Second, open global Search, and click Second
in Recent. The route does not change and the overlay remains. Clicking First
navigates and closes it. The same problem occurs with the current note in
Matches, including a Unicode title. The original report is
`/tmp/donut-uat-multinote.PhXs01/REPORT.md`; use independently created fixtures.

## Scope and current decisions

- A normal result-title activation in the current tab closes global search,
  whether the destination is the current route or a different route. Cover
  Recent and Matches and native keyboard activation of the result anchor.
- Keep real named RouterLink navigation, native anchor semantics, and existing
  search ranking/history. Modified clicks that open another tab must not trigger
  current-tab dismissal. Do not force reloads or fabricate route changes.
- Close from the search-result navigation action using the existing popup
  closer. Keep generic `Modal` route-change handling for other dialogs.
- Distinguish title navigation from `selected`, which currently means **Use this
  note** in relationship/wiki-link selection. That action must still show its
  choice step; move controls, search-mode toggles, and non-link row clicks must
  not be mistaken for navigation. Use an explicitly named navigation event or
  scoped callback, not a catch-all click handler on the entire dialog.
- Note/folder/notebook title links share a result renderer. Keep their existing
  hrefs and navigation intact; if the navigation signal is shared, carry it
  consistently through those links. Do not add new result kinds or selector flows.
- [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) keeps named SPA
  destinations authoritative. No route-table or content-link change is needed.
- Exclude the unrelated modal extraneous-attribute warning, a modal framework
  rewrite, focus-management redesign, search performance, and mobile layout work.

## Execution context

- `GlobalBar.vue` opens `SearchForm.vue` with the real popup `closer`.
- `SearchForNoteAndFolder.vue` owns the search dialog content and already receives
  `modalCloser`; `SearchResults.vue` renders both Recent and Matches through
  `SearchResultList.vue` and `SearchResultListItem.vue`.
- Result note titles use `NoteTitleWithLink.vue`; folder/notebook hits use
  RouterLinks. Scope any activation signal to search rather than changing how
  every note-title link in the application behaves.
- `Modal.vue` watches `route.fullPath`. That explains why current-route selection
  cannot rely on route changes to close.
- `searchDialogKeyboard.ts` moves focus among real links; Enter should retain
  native anchor activation, with no second keyboard-only navigation system.
- Existing `GlobalBar.spec.ts` mocks `useRoute`/`useRouter`; that setup cannot
  by itself prove current-route dismissal. Use a mounted popup with real router
  where needed, following existing helpers, and mock only backend APIs.

## Ordered slices

### 1. Reveal the selected note when search chooses the current route

Type: Behavior
Status: planned
Behavior: Global search is open on a note also present in its results → the user
activates that note's title → search closes and the same note remains visible,
without a reload or mutation.
Proof: An existing search-feature regression asserts actual dialog dismissal on
an unchanged route; mounted component cases protect activation semantics.

1. Add the failing user path to
   `e2e_test/features/note_view/search_note.feature`: view a seeded note, open
   search, select that same note in Recent, and assert the search dialog is gone
   while its note content is still visible. Cover Matches with the same current
   note via an explicit variant or concise outline. Reuse `search.ts` step glue
   and existing search page objects; title navigation is a distinct action from
   **Use this note**. Observe visible dismissal, not merely a click or route ID.
2. Introduce a result-title navigation signal through the existing search
   components and connect it to the existing closer. Preserve RouterLink default
   behavior. Do not close from `selected` or modify the generic modal watcher.
   Run the regression red-to-green in the same slice.
3. Extend the appropriate mounted search/popup test with a real router for
   current-note activation. Cover Enter on the focused link, a normal different
   result, and a modified click remaining open. Reuse existing relationship
   action tests to prove **Use this note** still reaches the choice panel; keep
   folder/notebook href tests green. Avoid a full cross-product of equivalent
   mouse/keyboard, mode, and result-kind cases.
4. Run the focused affected frontend files while iterating, then
   `CURSOR_DEV=true nix develop -c pnpm frontend:test` per frontend rules. Run
   `CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_view/search_note.feature`.
   Use existing debounce/busy synchronization; no fixed sleeps or full Cypress
   suite. Keep any multi-beat E2E `@wip` until green and remove it before commit.
5. Manually repeat current-note selection in Recent and Matches and use Enter
   once. Confirm another note still opens, and the relationship selection action
   remains usable. Record the observed outcome in this active plan.

Sizing: roughly 5–8 minutes active work, medium confidence; one activation outcome
through an existing component chain. Focused browser-suite runtime may extend
elapsed time. No preceding Structure slice is needed. If event propagation or
fixture changes cease to be one cohesive beat, or active work exceeds ten minutes,
refine this plan in place under the normal learning-escalation rule.

## Promise ownership and wrap-up

| Promise | Evidence owned by slice 1 |
| --- | --- |
| Current note is revealed from Recent and Matches | Search E2E plus manual same-note check |
| Keyboard activation works | Mounted real-popup/real-router Enter case and manual check |
| Ordinary and modified link semantics survive | Different-result and modified-click cases; existing href tests |
| Choice/move actions do not accidentally dismiss | Existing `SearchDialog.actions.spec.ts` and appropriate mounted action regression if missing |
| Other modal and route behavior remains intact | No generic modal/route change; existing frontend suite |

Execute with `execute-plan` and its per-slice wrap-up: Jidoka, fresh
post-change-refactor agent, one coordinator-run
`./scripts/run.sh pnpm format:changed`, plan update, commit, push and asynchronous
CI observation. No API generation is expected. Preserve this plan while active;
remove spent planning history once its behavior and proof are delivered.
