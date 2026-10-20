# Command-bar continuity preparation evidence

**Identity:** SEED-080#story-1
**Plan:** [Command-bar continuity](PLAN.md)

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
  that they need the explicit dialog assertion specified in [PROOF.md](PROOF.md#outside-in-proof-ownership).
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

## Execution observation — 2026-10-20

Slice 1 candidate, on established claim `c02920b649348640cdb8dab5b6d91ca1bd04e6c9`:

- `DonutMenu.vue` owns the common `Modal`/`SearchForNoteAndFolder` and one
  `openIntoSearch` operation that synchronously primes the keyboard, closes the
  menu, and opens the dialog. The embedded menu search remains pending slice 2.
- `DonutMenu.searchShortcut.spec.ts` observes actual open dialog and input focus
  for Ctrl/Cmd+F, entry while menu-open, Escape/Close teardown, and real result
  clicks for new/current notes. Visitor and existing-dialog boundaries remain.
  Fixture data supplies only backend search results; real router, focus and Modal
  establish the outcome.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars/DonutMenu.searchShortcut.spec.ts`
  passed 10 tests. `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/components/search tests/wiki-link-or-relationship tests/notes/NoteUnresolvedWikiLinkModal.spec.ts`
  passed 194 tests in 25 files. `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` both passed.
- Fresh post-change refactor found the product change already clean. It split
  this evidence and PROOF.md from PLAN.md without changing product/test code;
  accepted automated proof stays valid. Whitespace check passed.
- Only after automated proof, a marked disposable “Native search probe” button
  was added to call that same operation. Remove the marked comment/button in
  `DonutMenu.vue` before delivery, returning to the exact automated candidate.
- `CURSOR_DEV=true nix develop -c pnpm e2e:hold` owns the unpaid isolated stack,
  browser origin `http://127.0.0.1:50012`, LAN `http://192.168.33.5:50012`.
  Session `52361` remains held for owner-device observation. Test account
  `old_learner` / `password` has notebook “Search probe” and note “Keyboard sample”.
  A local Chrome probe observed the common focused dialog and search result;
  this supplies no native OS keyboard evidence.
- Requested owner observation: on iPad open `/users/identify`, sign in, tap
  “Native search probe”, observe software keyboard, type “Keyboard”, and open
  “Keyboard sample”; retain iPadOS/browser version and result. No observation
  has been received. Native input and dependent launcher acceptance stay open.
- No slice is marked done; no increment has been committed or pushed.

### LAN login setup correction — 2026-10-20

The owner reported login failing at the LAN HTTP URL. The stack was healthy;
Basic authentication returned HTTP 200 but `Set-Cookie` included `Secure`.
The application's default cookie configuration requires HTTPS, so the LAN HTTP
browser cannot retain the authenticated session. The earlier loopback Chrome
observation did not establish iPad/LAN authentication. No product login change
is part of this story.

Stopped the original owned hold runner (PID 45286) with SIGINT and observed
terminal exit 0. Restarted this isolated unpaid stack only with
`SERVER_SERVLET_SESSION_COOKIE_SECURE=false CURSOR_DEV=true nix develop -c pnpm e2e:hold`;
session `41600` supersedes `52361`, retaining the same allocated ports/database.
The hold reset test data; recreated “Search probe” / “Keyboard sample”. Verified
the LAN login has a session cookie without `Secure`, and a cookie-only follow-up
to `/api/user/current-user-info` identifies `old_learner`. A real Chrome login
through `http://192.168.33.5:50012/users/identify` reached “Welcome Old Learner”
and successfully created the sample content. Native iPad keyboard proof remains
pending; the temporary runtime override does not alter source configuration.

### Owner acceptance update — 2026-10-20

Terry tested desktop Chrome successfully and instructed: “Please proceed without
depending on me.” Native software-keyboard testing is unavailable for now. This
supersedes the native device acceptance prerequisite above, as recorded in the
source story's native input decision. Native iPad keyboard behavior remains
unverified; retain automated coarse-pointer focus checks and synchronous keyboard
preparation. Removed the disposable probe button, restoring the exact previously
tested product candidate. The completed product refactor and 194-test/typecheck
proof remain applicable. No additional owner observation is required to proceed.


### Slice 2 implementation observation — 2026-10-20

Candidate over slice 1 `ac60f83ff17e67c1a4fe25e6f3fc00813a07fdb2`:

- Both popup targets are actual buttons, labelled Open search and Search,
  calling the existing `openIntoSearch`. Keyboard Tab/Enter/Space reaches the
  empty field-shaped launcher with a visible outline, then the focused common
  dialog. Mounted tests observe Recent, history, Matches, scope/list switches,
  current-note selection, keyboard traversal and modified clicks in that dialog.
  Signed-out search remains an enforced authorization boundary.
- `DonutMenu.search.touch.spec.ts` mounts the actual shared primer and observes
  its focus event synchronously during each launcher click under real Chromium
  coarse-pointer emulation, then observes the real dialog input's focus.
  Native iPad keyboard behavior remains unverified under the owner's decision.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/components/search tests/wiki-link-or-relationship tests/notes/NoteUnresolvedWikiLinkModal.spec.ts`
  passed 195 tests in 26 files. After restoring the explicit signed-out launcher
  refusal assertion, `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars`
  passed 111 tests in 17 files; shared-search production code was unchanged.
- `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit`
  both passed against final frontend content.
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_view/search_note.feature`
  passed all 6 scenarios in the isolated stack at `http://127.0.0.1:50012`;
  the wrapper reached terminal exit 0 and owns teardown. Same-note Recent and
  Matches selection both close the actual dialog and reveal content; title
  matching, ordering and duplicate feedback pass. The subsequently deleted
  exclusively unused menu-close helper/export had no caller in this run.
- Initial migrated unit proof found two test setup gaps: the history journey
  had not activated its new launcher, and result-key traversal ran before
  scheduled modal autofocus settled. Actual launcher activation and the normal
  autofocus settling helper resolved those gaps; the complete affected suites
  then passed. No production behavior workaround was needed.
- Caller trace and one-time removal reading are recorded in PROOF.md, separately
  from behavioral proof. No component or API generation trigger applies.
  Implementation remains uncommitted for independent refactoring and delivery.

### Slice 3 implementation observation — 2026-10-20

Candidate over slice 2 `39503b5d1da3d55f2d7b23f8e62693d7c8fc68ff`:

- `DonutMenu` selects the first active item from the existing combined
  `useNavigationItems` lists. Its compact `NavigationItem` retains the existing
  icon, label, badges and styling. Ordinary contexts open the existing popup;
  action names preserve the existing Resume and next-assimilation owners.
  The adjacent chevron independently opens the menu, with the existing fallback
  icon and signed-out Login entry retained. Navigation eligibility is unchanged,
  including the paused loaded queue-end boundary.
- The mounted activity fixtures cover Note, Recall, Circles, Bazaar and Messages,
  ordinary activation, fallback, direct green Resume with diligent count, and
  independent menu opening. Existing destination and signed-out tests remain.
  Popup Resume queries are scoped to the panel, retaining an unambiguous compact
  action. The shared dropdown exposes its existing open state so the compact
  ordinary control reports its actual expanded state.
- The first run revealed the compact sibling's opening click immediately
  reaching the dropdown's document outside-click handler; stop that click at
  the compact control. Subsequent synthetic-click iterations raced native
  details toggle scheduling. Real browser `userEvent.click` observes completed
  interaction, and the final complete affected run passed all cases. Disposable
  trace instrumentation was removed before the final proof.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/toolbars tests/pages/RecallPage.spec.ts tests/pages/RecallPage.answering.spec.ts tests/pages/RecallPage.spelling.spec.ts tests/pages/NotebooksPage.spec.ts tests/pages/NotebookGroupPage.spec.ts tests/pages/BookReadingPage.spec.ts tests/composables/useAutoCollapseDetails.spec.ts`
  passed 168 tests in 25 files. The page consumers of
  `donutMenuLaunchersIn` keep the independent accessible menu opener. Recall
  progress aggregate text still passes with the compact activity present.
- `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/composables/useRecallData.spec.ts`
  passed 6 tests, retaining the existing resume owner's spelling keyboard focus
  preparation. Both `CURSOR_DEV=true nix develop -c pnpm -C frontend exec vue-tsc --noEmit`
  and `CURSOR_DEV=true nix develop -c pnpm -C frontend exec tsc --noEmit` passed
  against the final frontend candidate.
- The recall page object now clicks the compact Resume in the shared page bar.
  The journey reveals and remembers the real current question text and progress,
  reopens the last answered question through the existing UI, browses medical,
  resumes directly, and compares the restored question and progress. The test
  supplies neither the current index nor the expected question through store
  setup. The isolated Cypress wrapper owns the local stack and teardown at
  `http://127.0.0.1:50012`; no paid service is required.
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature`
  passed both scenarios against the final journey, including the answered-cursor
  browse-away and exact saved-question/progress comparison; wrapper exit 0.
- No component or API generation trigger applies. Native iPad software-keyboard
  behavior remains unverified under the owner's acceptance update. Changes are
  uncommitted pending independent refactoring and coordinator delivery.
- Proof acceptance found the Daily probe's ordinary-question entry sharing
  `expectCurrentQuestion`. Retained that generic visible-question assertion for
  `daily_probe.ts` and named the continuity comparison
  `expectRememberedQuestion`. A whole-E2E caller reading confirms the generic
  method has only the Daily probe caller, the remembered method only the recall
  continuity caller, and `rememberCurrentQuestion` only its explicit capture
  step. Daily probe requires no saved-question aliases.
- `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature,e2e_test/features/recall/daily_probe.feature`
  passed all 5 scenarios after that correction: 2 continuity/tracker scenarios
  and 3 Daily probe scenarios, wrapper exit 0. Both assertion contracts are
  exercised against the final E2E content.
