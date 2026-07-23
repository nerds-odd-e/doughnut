# E2E test optimization

Status: in-progress

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
Status: planned

**Tests:**
- `e2e_test/features/note_topology/note_move.feature` — "link and move" (~3602ms)
- `e2e_test/features/cli/cli_access_token.feature` — "Set access token" (~3587ms)
- `e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature` — "Browse notes while recalling and come back" (~3572ms)

**Goals:** Speed up these scenarios. Note: recall file may already be touched in Phase 1 — optimize only this scenario here; avoid conflicting rewrites of shared helpers without reading current HEAD.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_move.feature,e2e_test/features/cli/cli_access_token.feature,e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 6: Batch ranks 16–18 (AI models, bazaar subscribe, predefined questions)
Type: Behavior
Status: planned

**Tests:**
- `e2e_test/features/user_admin/manage_ai_models.feature` — "Admin choose a default model" (~3548ms)
- `e2e_test/features/bazaar/add_to_learning.feature` — "subscribe to a note and browse" (~3518ms)
- `e2e_test/features/note_creation_and_update/predefined_questions_management.feature` — "Manually add a question to the note successfully" (~3516ms)

**Goals:** Speed up these scenarios.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/user_admin/manage_ai_models.feature,e2e_test/features/bazaar/add_to_learning.feature,e2e_test/features/note_creation_and_update/predefined_questions_management.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 7: Batch ranks 19–21 (folder readme, circle invite, assimilation)
Type: Behavior
Status: planned

**Tests:**
- `e2e_test/features/folder_organization/folder_page_readme.feature` — "Folder readme content persists after reload" (~3488ms)
- `e2e_test/features/circles/creating_circles.feature` — "New user via circle invitation" (~3468ms)
- `e2e_test/features/assimilation/assimilation_walkthrough.feature` — "Walk through notes with menu, keep, skip, toasts, and panel on note page" (~3428ms)

**Goals:** Speed up these scenarios.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/folder_organization/folder_page_readme.feature,e2e_test/features/circles/creating_circles.feature,e2e_test/features/assimilation/assimilation_walkthrough.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 8: Batch ranks 22–24 (note tree, AI reorganize, wikidata person)
Type: Behavior
Status: planned

**Tests:**
- `e2e_test/features/note_topology/note_tree_view.feature` — "expand side bar to see the note tree" (~3417ms)
- `e2e_test/features/book_reading/ai_reorganize_layout.feature` — "AI reorganize opens preview dialog and applies on confirm" (~3402ms)
- `e2e_test/features/wikidata/associate_wikidata_person_entries.feature` — "Create a note for a person with wikidata should auto fill the content (example #1)" (~3391ms)

**Goals:** Speed up these scenarios.

**Verify:**

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/note_tree_view.feature,e2e_test/features/book_reading/ai_reorganize_layout.feature,e2e_test/features/wikidata/associate_wikidata_person_entries.feature
```

Run focused specs **3+ consecutive greens** before closing.

---

### Phase 9: Re-profile and close
Type: Behavior
Status: planned

Re-run the same profile command as baseline. Record metrics below. Propose any remaining hard cases under **Candidates**. Mark plan **Status: done**. Clean up spent plan history per test-optimization `planning_cleanup` (keep blacklist; do not commit profile JSON).

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run-on-sut --reporter json 2>&1 | tee /tmp/e2e-profile-after.log
```

| Metric | Before | After |
|--------|--------|-------|
| Test count | 231 | |
| Suite wall | ~9m39s | |
| Top 10% total time | ~94.3s | |

**Candidates proposed this run:** (none / list)

**Commits:**
- Phase 1: `perf(e2e): speed up audio/recall/note-yaml scenarios`
- Phase 2: `perf(e2e): speed up circle-note and unread-message scenarios`
- Phase 3: `perf(e2e): speed up wikidata-create, semantic-search, reading-record`
