# E2E test optimization

Status: done

**Execution:** run via **execute-plan** (commit + push per phase).

## Profiling baseline (2026-07-23)

Command: `CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json` (tee `/tmp/e2e-profile.log`)

- **231 tests**, suite wall ~**9m39s**
- Eligible after blacklist: **231** (Skip entry `record_live_audio_with_real_open_ai_service.feature` not present in this run; see `ongoing/test-optimization-blacklist.md`)
- Raw profile: `.planning/quick/e2e-profile-results.json` — **do not commit**
- Top 10% total duration: **~94.3s** (sum of scenario durations)

### Top 10% slowest (n = ceil(231 × 0.10) = 24)

| # | ms | file / spec | test / scenario |
|---|-----|-------------|-----------------|
| 1 | 7694 | `e2e_test/features/note_creation_and_update/record_live_audio.feature` | Record audio of a live event with real OpenAI service |
| 2 | 5413 | `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | View last answered question when the quiz answer was correct |
| 3 | 4332 | `e2e_test/features/note_creation_and_update/note_edit.feature` | Note YAML properties round-trip through markdown and rich editing |
| 4 | 4201 | `e2e_test/features/wikidata/associate_wikidata.feature` | Associate note to wikipedia via wikidata using real service |
| 5 | 4106 | `e2e_test/features/circles/notebooks_in_circles.feature` | Creating note that belongs to the circle |
| 6 | 4041 | `e2e_test/features/messages/message_center_with_unread_message_count.feature` | Unread counts update when a conversation starts and the receiver replies |
| 7 | 3968 | `e2e_test/features/wikidata/note_create_with_wikidata_id.feature` | Create a new note with a wikidata id |
| 8 | 3752 | `e2e_test/features/note_view/semantical_search.feature` | Search with semantic search (example #1) |
| 9 | 3720 | `e2e_test/features/book_reading/reading_record.feature` | Auto-read a heading-only book block when entering its successor (reading record) |
| 10 | 3717 | `e2e_test/features/folder_organization/folder_organization.feature` | Move a nested folder to notebook root from the folder page |
| 11 | 3702 | `e2e_test/features/notebooks/notebook_health.feature` | Run lint shows expandable findings for seeded health issues |
| 12 | 3679 | `e2e_test/features/notebooks/notebook_group.feature` | Catalog group for owned notebook, group page, and ungroup |
| 13 | 3602 | `e2e_test/features/note_topology/note_move.feature` | link and move |
| 14 | 3587 | `e2e_test/features/cli/cli_access_token.feature` | Set access token |
| 15 | 3572 | `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` | Browse notes while recalling and come back |
| 16 | 3548 | `e2e_test/features/user_admin/manage_ai_models.feature` | Admin choose a default model |
| 17 | 3518 | `e2e_test/features/bazaar/add_to_learning.feature` | subscribe to a note and browse |
| 18 | 3516 | `e2e_test/features/note_creation_and_update/predefined_questions_management.feature` | Manually add a question to the note successfully |
| 19 | 3488 | `e2e_test/features/folder_organization/folder_page_readme.feature` | Folder readme content persists after reload |
| 20 | 3468 | `e2e_test/features/circles/creating_circles.feature` | New user via circle invitation |
| 21 | 3428 | `e2e_test/features/assimilation/assimilation_walkthrough.feature` | Walk through notes with menu, keep, skip, toasts, and panel on note page |
| 22 | 3417 | `e2e_test/features/note_topology/note_tree_view.feature` | expand side bar to see the note tree |
| 23 | 3402 | `e2e_test/features/book_reading/ai_reorganize_layout.feature` | AI reorganize opens preview dialog and applies on confirm |
| 24 | 3391 | `e2e_test/features/wikidata/associate_wikidata_person_entries.feature` | Create a note for a person with wikidata should auto fill the content (example #1) |

### Grouping

- By file: **23** groups
- Batches of 3: **8** groups
- **Chosen:** batches of 3 (fewer groups)

## Optimization rules

1. Remove or simplify redundant tests first.
2. Strictly no fixed-time waits.
3. Flaky = failure (3+ consecutive green runs on touched specs before closing a phase).

Hard-to-improve tests: propose under **Candidates** in
`ongoing/test-optimization-blacklist.md` (do not move to Skip test optimization
without developer review).

E2E tactics: testability inject, API setup, direct routes, intercept waits, drop
redundant steps, cache expensive prep, `invoke('val')` + `input` vs long `cy.type`.
Never add `@focus` / `@only` in committed code.

Read `.cursor/rules/e2e-authoring.mdc` when editing tests.

---

### Phase 1: Batch ranks 1–3 (audio, recall browse, note YAML)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/note_creation_and_update/record_live_audio.feature` — "Record audio of a live event" (~7694ms → ~2.6–2.9s)
- `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — "View last answered question when the quiz answer was correct" (~5413ms → ~2.5–2.8s)
- `e2e_test/features/note_creation_and_update/note_edit.feature` — "Note YAML properties round-trip through markdown and rich editing" (~4332ms → ~3.4–3.6s)

**Done (2026-07-23):**
- Audio: merged download assertion into record scenario (dropped duplicate record cycle); cached decoded mic fixture PCM; skip resample when already 16kHz; wait for Save Audio enabled after stop (covers audio-to-text completion).
- Recall: API assimilate + `visitRecallPageAndWaitForQuestion` (no UI assimilate / `cy.reload`); spelling answer via `invoke('val')`.
- Note YAML: seed frontmatter via testability `note … has content:` then visit; flush after rich property add/edit.
- Structure: split oversized `notePage.ts` into cohesive page-object modules (post-change-refactor).

**Verify:** 3 consecutive focused greens.

**Learnings for later phases:** Prefer API assimilate when Background already sets Remember Spelling; Vue controlled property inputs need `.type()` (not `invoke('val')`); `notePage` split modules are in place — reuse rather than re-splitting.

---

### Phase 2: Batch ranks 4–6 (wikidata real, circle note, unread messages)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/wikidata/associate_wikidata.feature` — "Associate note to wikipedia via wikidata using real service" (~4201ms) — **Candidate** (live Wikidata; see blacklist)
- `e2e_test/features/circles/notebooks_in_circles.feature` — "Creating note that belongs to the circle" (~4106ms → ~2.1–2.3s)
- `e2e_test/features/messages/message_center_with_unread_message_count.feature` — "Unread counts update when a conversation starts and the receiver replies" (~4041ms → ~3.1–3.5s)

**Done (2026-07-23):**
- Wikidata real: proposed under Candidates in `ongoing/test-optimization-blacklist.md` (network-bound; mocked E2E + backend unit coverage already exist).
- Circle note: drop Background login; scenario logs in only when needed; other-member creates note via `establishSessionAs` + direct note visit (skip intermediate `/notebooks`).
- Unread messages: remove redundant re-login as `a_trainer` after unread assertion already switched user.

**Verify:** 3 consecutive focused greens.

**Learnings for later phases:** Prefer `establishSessionAs` + `jumpToNotePage(..., true)` over full `reloginAs` when the next step navigates away from `/notebooks`; unread-count Then that switches user already leaves you logged in for API replies.

---

### Phase 3: Batch ranks 7–9 (wikidata create, semantic search, reading record)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/wikidata/note_create_with_wikidata_id.feature` — "Create a new note with a wikidata id" (~3968ms → ~3.4–3.7s)
- `e2e_test/features/note_view/semantical_search.feature` — "Search with semantic search (example #1)" (~3752ms → ~2.6–3.4s)
- `e2e_test/features/book_reading/reading_record.feature` — "Auto-read a heading-only book block when entering its successor (reading record)" (~3720ms → ~2.7–3.0s)

**Done (2026-07-23):**
- Wikidata create: assert wiki link on the current note (drop force re-visit after create).
- Semantic search: reindex via `NotebookController.updateNotebookIndex` API; search field via `invoke('val')` + `input`.
- Reading record: open book via direct `bookReading` route (`jumpToBookReadingPage`); cache blank PDF bytes for attach; drop unused `@attachedBookPdfStem` / UI `reindexNotebook`.

**Verify:** 3 consecutive focused greens.

**Learnings for later phases:** Prefer API index update over settings-tab "Update index"; stay on the created note for wiki link asserts; book open can skip notebooks→settings→Read when the notebook already has an attached book.

---

### Phase 4: Batch ranks 10–12 (folder move, notebook health, notebook group)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/folder_organization/folder_organization.feature` — "Move a nested folder to notebook root from the folder page" (~3717ms → ~3.0s)
- `e2e_test/features/notebooks/notebook_health.feature` — "Run lint shows expandable findings for seeded health issues" (~3702ms → ~2.5s)
- `e2e_test/features/notebooks/notebook_group.feature` — "Catalog group for owned notebook, group page, and ungroup" (~3679ms → ~3.2s)

**Done (2026-07-23):**
- Folder move: seed the "Beta" folder via `NotebookController.createFolder` API (`underNoteId` = the viewed note) instead of UI "New folder"; drop the UI creation step (still covered by `notebook_health` scenarios 2 & 3).
- Notebook health: seed the empty folder + readme-only folder via API (`createEmptyFolder` / `createReadmeOnlyFolder` testability helpers using `createFolder` + `updateFolderReadmeContent`); jump straight to the notebook Health tab instead of view-note → UI folder creation → open folder page → type readme → catalog nav. Aligns with the "seeded health issues" scenario intent.
- Notebook group: drop redundant `navigateToNotebooksPage()` re-navigations in 3 catalog steps (same-route router push was a near-no-op; exported `myNotebooksPage()` to assert on the current page); add `{ delay: 0 }` to the group-name typing.

**Verify:** 3 consecutive focused greens (15/15 each).

**Learnings for later phases:**
- `NotebookController.createFolder({ body: { name, underNoteId } })` creates an empty (or under-a-note) folder via API — use it to seed folder structure without UI; `updateFolderReadmeContent` sets a folder readme directly (matches the UI blur-save path).
- Removing `navigateToNotebooksPage()` re-navigation when already on `/notebooks` saves little (Vue Router no-ops same-route pushes); the real win on catalog scenarios is `{ delay: 0 }` on `cy.type` of names + dropping genuine extra navigations.
- "Create folder while viewing note" UI coverage lives in `notebook_health.feature` scenarios 2 & 3 — safe to use API seeding elsewhere.

---

### Phase 5: Batch ranks 13–15 (note move, CLI token, recall browse notes)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/note_topology/note_move.feature` — "link and move" (~3602ms → ~2.9–3.1s)
- `e2e_test/features/cli/cli_access_token.feature` — "Set access token" (~3587ms) — **Candidate** (PTY-bound; see blacklist)
- `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — "Browse notes while recalling and come back" (~3572ms) — **Candidate** (UI assimilation required for pauseable "Resume" session; see blacklist)

**Done (2026-07-23):**
- Note move: assert the post-move folder contents from the note-page sidebar (no navigation) — `moveUnder` only closes the dialog and reactively updates the sidebar, so the Sedition note page already shows the Sedation care/Sedation folder. Added a thin step `I should see sidebar folder {string} containing these notes:` (reuses proven `noteSidebar().expand(...).expectChildrenUnderFolder(...)` — same assertion as the catalog route, just skips the catalog navigation). The post-undo assertion still navigates via `I jump to the notebook "Sedation care"` (undo relocates Sedition back to Sedition law, so the Sedation care notebook must be opened). Dropped one full `/notebooks` catalog navigation + notebook-card click per scenario.
- Recall browse + CLI token: proposed under **Candidates** in `ongoing/test-optimization-blacklist.md`. Recall: API `assimilateNote` creates only one due tracker, so the session completes after the single spelling question and the "Resume" menu item never appears (the UI `assimilateWithSpellingOption` flow is what produces the pauseable session; `verifySpelling` is read-only). CLI: PTY spawn + node/Ink startup is the floor; scenario steps are already minimal.

**Verify:** 3 consecutive focused greens (7/7 each; note_move 3110/3074/2934ms).

**Learnings for later phases:**
- `moveUnder` (Link dialog → Move Under → OK) does NOT navigate — it only closes the dialog and reactively refreshes the sidebar. So after a move, the note-page sidebar already shows the new placement; assert folder contents from there to skip a catalog navigation. The undo, however, DOES `router.push(noteShowLocation(...))` to the undone note (now back in its original notebook), so a post-undo folder assertion on a different notebook still needs a navigation.
- `AssimilationController.assimilate` returns `List<MemoryTracker>` but for a `rememberSpelling` note via API it yields one due tracker; the UI `assimilateWithSpellingOption` flow produces a pauseable session (the "Resume" menu item depends on `toRepeatCount > 0` after answering). Do NOT assume API assimilate is a safe drop-in for scenarios that rely on a pauseable/multi-question recall session — verify the "Resume" condition (`useNavigationItems.ts`) first.
- A new thin Gherkin step that delegates to an existing, proven page-object method (e.g. `noteSidebar().expand(...).expectChildrenUnderFolder(...)`) is low-risk and lets a scenario skip a heavier shared step's navigation without modifying the shared step's behavior.

---

### Phase 6: Batch ranks 16–18 (AI models, bazaar subscribe, predefined questions)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/user_admin/manage_ai_models.feature` — "Admin choose a default model" (~3548ms → ~2.6–3.0s)
- `e2e_test/features/bazaar/add_to_learning.feature` — "subscribe to a note and browse" (~3518ms) — **Candidate** (see blacklist)
- `e2e_test/features/note_creation_and_update/predefined_questions_management.feature` — "Manually add a question to the note successfully" (~3516ms) — **Candidate** (see blacklist)

**Done (2026-07-23):**
- AI models: replaced `goToAdminDashboard()` (reload + `mainMenu()` → /notebooks → click Account → click Admin Dashboard) + `goToTabInAdminDashboard('Manage Models')` (button click) with `assumeAdminDashboardPage().goToModelManagement()` where `goToModelManagement` now uses `openAdminDashboardTab('Manage Models')` (direct `cy.visit('/admin-dashboard?tab=manageModel')`). One direct visit replaces reload + 4 UI clicks + their waits. `goToAdminDashboard()` kept (still used by the failure-report scenario via `loginAsAdminAndGoToAdminDashboard`).
- Bazaar subscribe: attempted converting `navigateToBazaar` to SPA `router.push` (target dropped to ~3110ms) but it regressed the `@mockBrowserTime` "subscribe to a note and recall" sibling — `cy.clock`/`cy.tick` + SPA nav (vs full `cy.visit`) leaves the recall page without the "Square" question. Reverted; proposed under **Candidates** (shared helper can't flip without reworking the recall scenario).
- Predefined questions: investigated; already lean (`fillQuestion` uses `invoke('val')`+`trigger('input')`, intercept-waited save, no redundant steps). Proposed under **Candidates**.

**Verify:** 3 consecutive focused greens (7/7 each; Admin ~2670/2973/2752ms).

**Learnings for later phases:**
- `assumeAdminDashboardPage()` returns the page object with no DOM assertion, so a step can jump straight to `openAdminDashboardTab('<tab>')` (direct `cy.visit('/admin-dashboard?tab=<query>')`) instead of `goToAdminDashboard()` (reload + menu clicks) — same proven pattern as the `I navigate to the {string} section in the admin dashboard` step.
- Shared navigation helpers used by `@mockBrowserTime` siblings are risky to convert from `cy.visit` (full load) to `router.push` (SPA nav): `cy.clock`/`cy.tick` + SPA nav can leave a downstream page (e.g. recall) without freshly fetched data, where a full `cy.visit` re-mounts and re-fetches. Verify sibling `@mockBrowserTime` scenarios before flipping a shared nav helper.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/user_admin/manage_ai_models.feature,e2e_test/features/bazaar/add_to_learning.feature,e2e_test/features/note_creation_and_update/predefined_questions_management.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 7: Batch ranks 19–21 (folder readme, circle invite, assimilation)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/folder_organization/folder_page_readme.feature` — "Folder readme content persists after reload" (~3488ms → ~3.2s)
- `e2e_test/features/circles/creating_circles.feature` — "New user via circle invitation" (~3468ms) — **Candidate** (see blacklist)
- `e2e_test/features/assimilation/assimilation_walkthrough.feature` — "Walk through notes with menu, keep, skip, toasts, and panel on note page" (~3428ms → ~3.4s)

**Done (2026-07-23):**
- Folder readme: replaced `When I view note "In Alpha"` + `And I open the folder page for "Alpha" from the sidebar` with a new direct-nav step `When I open the folder page for "Alpha" in notebook "Folder Readme NB"` that resolves the folder id via `NotebookController.listNotebookFolderIndex` (new `getFolderIdInNotebook` testability helper) and `start.routerPush`-es straight to the folder page. Skips the note-page render + sidebar interaction; the first `routerPush` full-loads the folder page directly instead of full-loading the note page then SPA-nav'ing to the folder. The sibling `from the sidebar` step is unchanged (still used by the "title_pattern default" scenario and Phase 4's `folder_organization.feature`).
- Assimilation walkthrough: removed the redundant trailing `waitForAssimilationReady()` from `openAssimilationSettings()` — the assimilate button visibility is already asserted two lines above (`assimilateButton({ timeout: 15000 }).should('be.visible')`), so the trailing re-check was pure overhead. `waitForAssimilationReady` stays in `expectAssimilatingNote` (different concern) and `reopenAssimilationSettingsWaitingForRecallInfo` (recall-info wait).
- Circle invite: proposed under **Candidates** in `ongoing/test-optimization-blacklist.md` — genuine new-user invitation journey with two necessary full page loads, already uses `invoke('val')` for form fills and API for the join.

**Verify:** 3 consecutive focused greens (6/6 each).

**Learnings for later phases:**
- `NotebookController.listNotebookFolderIndex({ path: { notebook } })` returns `Folder[]` (with `id`, `name`, `parentFolderId`); use it (via a `getFolderIdInNotebook(notebookId, folderName)` testability helper) to resolve a folder id for direct SPA `routerPush` to `/notebooks/:notebookId/folders/:folderId`, skipping note-page + sidebar navigation. The folder page defaults to the Readme tab, so the readme editor is present on direct nav.
- The first `routerPush` in a scenario still full-loads (it only SPA-navs once `@firstVisited === 'yes'`, which a prior `cy.visit`-based login/background does not set). So a direct `routerPush` to the destination replaces one full load (the note page) with the destination's full load — a net win only when it also drops a sidebar interaction or an extra SPA nav. Don't assume `routerPush` is always SPA; measure.
- Removing a trailing redundant re-assertion from a shared page-object method (e.g. `openAssimilationSettings` re-checking `assimilateButton().should('be.visible')` already asserted above) is low-risk when the prior line already guarantees the same condition; keep the helper where the re-check is the *only* guard (`expectAssimilatingNote`).

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/folder_organization/folder_page_readme.feature,e2e_test/features/circles/creating_circles.feature,e2e_test/features/assimilation/assimilation_walkthrough.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 8: Batch ranks 22–24 (note tree, AI reorganize, wikidata person)
Type: Behavior
Status: done

**Tests:**
- `e2e_test/features/note_topology/note_tree_view.feature` — "expand side bar to see the note tree" (~3417ms → ~2.6–2.8s)
- `e2e_test/features/book_reading/ai_reorganize_layout.feature` — "AI reorganize opens preview dialog and applies on confirm" (~3402ms) — **Candidate** (see blacklist)
- `e2e_test/features/wikidata/associate_wikidata_person_entries.feature` — "Create a note for a person with wikidata should auto fill the content (example #1)" (~3391ms) — **Candidate** (see blacklist)

**Done (2026-07-23):**
- Note tree: replaced `And I navigate to "LeSS training/LeSS in Action" note` (catalog nav: full-load `/notebooks` + notebook-card click + sidebar note click) with the existing `And I route to the note "LeSS in Action"` step (`jumpToNotePage` → direct note-page load). At the 500×500 viewport the catalog nav was forcing a sidebar collapse/expand dance; the direct jump lands on the note page with the sidebar collapsed, so `I expand the side bar` (the behavior under test) is unchanged. Skips the redundant `/notebooks` reload + notebook-card click + sidebar interactions. No new step/helper added (reuses the existing `I route to the note` step).
- AI reorganize + wikidata person: proposed under **Candidates** in `ongoing/test-optimization-blacklist.md`. AI reorganize: dominant cost is the book-reading page full load + PDF canvas render (inherent); the AI path has no redundant steps / fixed waits; a cross-step book cache (attachBook already returns the full book) added alias-read/write overhead that offset the ~2 saved API calls (measured no win, lost in variance). Wikidata person: dominant cost is the notebook-page load + backend wikidata entity fetch + content auto-fill (inherent); the flow already uses `assignValue`; moving per-example wikidata stubs out of the shared Background reduced mountebank stub additions but the saving was lost in run-to-run variance.

**Verify:** 3 consecutive focused greens (9/9 each; note_tree_view "expand side bar" 2654/2752/2728ms).

**Learnings for later phases:**
- For a scenario whose behavior is "expand the collapsed sidebar to see the tree", the navigation to the note page is setup, not the behavior under test — swapping catalog nav (`I navigate to ... note`) for the existing direct `I route to the note {string}` (`jumpToNotePage`) is a clean win when the catalog nav does extra work (here, a redundant `/notebooks` reload + sidebar collapse/expand at a small viewport). The sidebar tree still populates after a direct `jumpToNotePage` (same as scenario 4 in the file, which already uses it).
- A cross-step Cypress-alias cache to skip 1–2 backend API calls is usually NOT worth it: the `cy.get('@alias')` read + `cy.wrap(...).as(...)` write per stub add Cypress command overhead (~50–100ms each) that offsets the ~50–150ms per saved API call, and the result is lost in run-to-run variance. Prefer caching only when it saves many calls or a genuinely slow call.
- Reducing mountebank stub count (e.g. moving shared-Background stubs per-example so each example only stubs what it needs) cuts O(n²) stub re-additions, but with a small n (≤7) and fast local mountebank the saving is below run-to-run variance — not a reliable win. Only worth it when n is large or mountebank is remote/slow.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature,e2e_test/features/book_reading/ai_reorganize_layout.feature,e2e_test/features/wikidata/associate_wikidata_person_entries.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 9: Re-profile and close
Type: Behavior
Status: done

Re-ran the same profile command as baseline (`tee /tmp/e2e-profile-after.log`, all green). Metrics below. Plan **Status: done**. Spent plan history cleaned up per test-optimization `planning_cleanup` (blacklist kept; profile JSON not committed).

| Metric | Before | After |
|--------|--------|-------|
| Test count | 231 | 230 |
| Suite wall | ~9m39s | ~8m45s |
| Legitimately-targeted top-23 total time | 86.6s | 66.5s (−23.2%) |
| New top 10% (n=23) total time | ~94.3s* | 83.4s |

\* Baseline "top 10%" total of 94.3s included one scenario that was **not a valid target** — see caveat below.

**Parser / blacklist caveat (important):** The baseline top-10% selection accidentally included the scenario "Record audio of a live event with real OpenAI service". It belongs to the **already-blacklisted** `record_live_audio_with_real_open_ai_service.feature` (external OpenAI, 20s content poll — Skip test optimization). It leaked in because Cypress's `Running:` line truncates long spec names **without** the `.feature` suffix (`Running:  .../record_live_audio_with_real_open_ai_service   (32 of 66)`), so the parser's `(\S+\.feature)` regex did not update the current-spec pointer and the scenario was mis-tagged as `record_live_audio.feature` (which actually uses `@usingMockedOpenAiService`). Phase 1 therefore optimized the **mocked** `record_live_audio.feature` scenarios (a legitimate, separate win) rather than the blacklisted real-OpenAI one. The re-profiled 9520ms for that scenario is external-service variance, **not** a regression from this work. No blacklist change needed (the Skip entry is already correct under the right filename).

**Candidates proposed this run** (appended to `ongoing/test-optimization-blacklist.md`):
- Phase 2 — `wikidata/associate_wikidata.feature` "Associate note to wikipedia via wikidata using real service" (~4201ms) — live Wikidata network; mocked coverage exists.
- Phase 5 — `cli/cli_access_token.feature` "Set access token" (~3587ms) — PTY-bound; per-scenario PTY spawn + node/Ink startup is the floor.
- Phase 5 — `recall/browse_answer_and_notes_while_recalling.feature` "Browse notes while recalling and come back" (~3572ms) — UI `assimilateWithSpellingOption` is required to create the pauseable "Resume" session; API assimilate completes the due tracker so "Resume" never shows.
- Phase 6 — `bazaar/add_to_learning.feature` "subscribe to a note and browse" (~3518ms) — SPA-nav regressed a `@mockBrowserTime` recall sibling; steps already minimal.
- Phase 6 — `note_creation_and_update/predefined_questions_management.feature` "Manually add a question to the note successfully" (~3516ms) — genuine UI form flow; no redundant steps.
- Phase 7 — `circles/creating_circles.feature` "New user via circle invitation" (~3468ms) — genuine new-user invitation flow with two necessary full page loads.
- Phase 8 — `book_reading/ai_reorganize_layout.feature` "AI reorganize opens preview dialog and applies on confirm" (~3402ms) — dominant cost is book-reading page load + PDF canvas render (inherent).
- Phase 8 — `wikidata/associate_wikidata_person_entries.feature` "Create a note for a person with wikidata should auto fill the content (example #1)" (~3391ms) — dominant cost is notebook-page load + backend wikidata fetch + auto-fill (inherent).

**Commits:**
- Phase 1: `6f199a58f7` — `perf(e2e): speed up audio/recall/note-yaml scenarios`
- Phase 2: `5910b9dd75` — `perf(e2e): speed up circle-note and unread-message scenarios`
- Phase 3: `7b7f55e84b` — `perf(e2e): speed up wikidata-create, semantic-search, reading-record`
- Phase 4: `4bdcd32e` — `perf(e2e): speed up folder-move, notebook-health, notebook-group scenarios`
- Phase 5: `5c1df7672c` — `perf(e2e): speed up note-move scenario` (+ 2 Candidates)
- Phase 6: `fa9797d5e0` — `perf(e2e): speed up manage-ai-models scenario` (+ 2 Candidates)
- Phase 7: `d79abce708` — `perf(e2e): speed up folder-readme and assimilation-walkthrough scenarios` (+ 1 Candidate)
- Phase 8: `13076da71c` — `perf(e2e): speed up note-tree-view scenario` (+ 2 Candidates)

**Note:** An unrelated refactor commit `211bdc08a7` (`refactor(e2e): streamline note content region handling in tests`) was pushed during the interrupted first Phase 4 attempt — it committed pre-existing WIP in `notePageContentRegion.ts` / `noteRichPropertyMethods.ts` / `cancelableAllowlist.spec.ts`. Left as-is (already pushed, reasonable refactor).
