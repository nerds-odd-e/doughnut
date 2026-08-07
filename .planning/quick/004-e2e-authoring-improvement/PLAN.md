# E2E authoring improvement

## Goal

Improve all E2E feature files (and supporting steps/page objects as needed) so they follow `.cursor/rules/e2e-authoring.mdc`. Work is split by domain groups: **audit against the shared checklist** (see `CONTEXT.md`), then **apply fixes**, then **run targeted Cypress** for that group. Product behavior must not change.

## Design decisions

1. **Group by domain folder** (split oversized folders) so each phase stays near the ~5–10 minute slice budget including targeted Cypress.
2. **One Behavior phase per group** = audit + improve + green targeted specs (stop-safe: that group is left better even if later groups never run).
3. **Prefer Gherkin/step/PO wording and structure** over product code; stop for Jidoka if a fix seems to require product behavior change.
4. **`Folder` column** — remove only when parent/child inject order suffices; keep when folder hierarchy is the capability under test.
5. **CLI `@ignore` features** — improve Gherkin/domain language only; do not add a second Cypress PTY harness (see e2e-authoring).
6. **Do not encode phase numbers** in feature/scenario names.
7. **Progress lives in this directory’s `STATE.md`** — not the shared `.planning/STATE.md` (avoids conflicts with other workstreams).

## Phases

### Phase 1 — Bazaar E2E authoring

**Status:** done  
**Type:** Behavior

- Split browsing into focused scenarios; domain wording (`expectCannotEditNotes` / cannot add to learning); dropped unnecessary Folder columns; Background for shared guest setup.
- Refactor: removed dead `navigateWithinOpenNotebook`; extracted `sidebarToolbarButtons.ts` from oversized `noteSidebar.ts`.

**Files:** `bazaar/browsing.feature`, `bazaar/sharing.feature`, `bazaar/add_to_learning.feature`

**Done when:** checklist applied; `pnpm cypress run --spec` for the three bazaar features passes. ✅ 7/7

---

### Phase 2 — Recall E2E authoring (browse / pages / spaced / match)

**Status:** done  
**Type:** Behavior

- Domain Gherkin (resume recalling, remove/revive with Then); When/Then hygiene; shared Backgrounds; PO renames; split `assimilation_settings.ts` from oversized assimilation steps.

**Files:** `recall/browse_answer_and_notes_while_recalling.feature`, `recall/recall_pages.feature`, `recall/spaced_repetition.feature`, `recall/accidental_match_reveal.feature`, `recall/overlap_try_again.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 16/16

---

### Phase 3 — Recall E2E authoring (quiz / tracker / refine)

**Status:** done  
**Type:** Behavior

- Real spelling assertions; domain Gherkin; shared Backgrounds; split property skip/revive; busy-wait; collapsed duplicate assimilation step aliases (callers updated).

**Files:** `recall/recall_quiz_spelling_question.feature`, `recall/recall_quiz_ai_question.feature`, `recall/property_memory_tracker.feature`, `recall/re_assimilate.feature`, `recall/refine_note_after_mcq.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 18/18 (+ caller smoke)

---

### Phase 4 — Assimilation E2E authoring

**Status:** done  
**Type:** Behavior

- Split walkthrough into focused scenarios; domain wording for slow load / assimilate disabled; When/Then hygiene; collapsed duplicate assimilate-on-day Givens; split oversized `note.ts` into cohesive modules.

**Files:** `assimilation/assimilation_walkthrough.feature`, `assimilation/assimilate_with_remembering_spelling.feature`, `assimilation/edit_when_assimilating.feature`, `assimilation/note_refinement.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 26/26 (+ related smokes)

---

### Phase 5 — Notebooks E2E authoring

**Status:** done  
**Type:** Behavior

- Dropped presentation-only catalog open; When/Then + domain wording; split group mega-scenario; inject fixtures for health; dedicated `notebookGroupPage` PO.

**Files:** `notebooks/notebook_catalog_navigation.feature`, `notebooks/notebook_creation.feature`, `notebooks/notebook_export.feature`, `notebooks/notebook_group.feature`, `notebooks/notebook_health.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 14/14

---

### Phase 6 — Note create / edit / delete E2E authoring

**Status:** done  
**Type:** Behavior

- Domain Gherkin; split undo/YAML; soft line-break without Shift-Enter in features; Kind/Text rich-content assertions; collapsed dual rich-content APIs.

**Files:** `note_creation_and_update/note_creation.feature`, `note_creation_and_update/note_edit.feature`, `note_creation_and_update/note_deletion.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 25/25 (+ link smoke)

---

### Phase 7 — Note questions / live audio E2E authoring

**Status:** done  
**Type:** Behavior

- Shared Backgrounds; domain wording; busy waits on audio stop/upload; `@usingRealOpenAiService` marker (mocked specs verified; real OpenAI not run without credentials).

**Files:** `note_creation_and_update/predefined_questions_management.feature`, `note_creation_and_update/record_live_audio.feature`, `note_creation_and_update/record_live_audio_with_real_open_ai_service.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 5/5 mocked

---

### Phase 8 — Note topology E2E authoring

**Status:** done  
**Type:** Behavior

- Domain Gherkin for wiki links / move / tree view; Kind labels `wiki link` / `dead wiki link` / `live wiki link`; open notebook sidebar naming.

**Files:** `note_topology/wiki_link.feature`, `note_topology/note_move.feature`, `note_topology/note_tree_view.feature`

**Done when:** checklist applied; targeted Cypress passes. ✅ 17/17

---

### Phase 9 — Note view / search E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Note view features may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `note_view/search_note.feature`, `note_view/semantical_search.feature`, `note_view/note_recent_update.feature`, `note_view/note_frontmatter_image.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 10 — Messages E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Message features may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `messages/message_center_with_unread_message_count.feature`, `messages/chat_about_a_note.feature`, `messages/message_for_note.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 11 — Circles and relationships E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Circles/relationships features may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `circles/creating_circles.feature`, `circles/notebooks_in_circles.feature`, `relationships/add_relationship.feature`, `relationships/relationship_edit_and_remove.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 12 — Users and admin E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Users/admin features may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `users/account_control.feature`, `users/new_user.feature`, `users/user_access_token.feature`, `users/user_profile.feature`, `user_admin/manage_ai_models.feature`, `user_admin/manage_bazaar.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 13 — Wikidata E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Wikidata features may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `wikidata/associate_wikidata.feature`, `wikidata/associate_wikidata_person_entries.feature`, `wikidata/associate_wikidata_location_entries.feature`, `wikidata/note_create_with_wikidata_id.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 14 — Book reading E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Book reading features may violate checklist (heavy “click” / “press key” wording).
- **Trigger:** Audit + improve toward domain intention where possible without losing Explicit Conditions for key variants.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `book_reading/book_browsing.feature`, `book_reading/epub_book.feature`, `book_reading/reading_record.feature`, `book_reading/reorganize_layout.feature`, `book_reading/ai_reorganize_layout.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 15 — Folder organization E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Folder features may violate checklist (Folder column is often legitimate here).
- **Trigger:** Audit + improve without removing Folder when hierarchy is under test.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `folder_organization/folder_organization.feature`, `folder_organization/folder_page_readme.feature`

**Done when:** checklist applied; targeted Cypress passes.

---

### Phase 16 — CLI E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** CLI features may violate checklist; many are `@ignore` in CI.
- **Trigger:** Improve Gherkin/domain language and non-ignored install/run paths; do not add Cypress PTY for interactive.
- **Post:** Checklist satisfied for editable surface; run Cypress only for non-`@ignore` specs in this group (or confirm tag filter skips ignored).

**Files:** `cli/cli_install_and_run.feature`, `cli/cli_access_token.feature`, `cli/cli_gmail.feature`, `cli/cli_interactive_mode.feature`, `cli/cli_recall.feature`

**Done when:** checklist applied; appropriate targeted Cypress (or documented ignore) green.

---

### Phase 17 — MCP, AI-generated, and testability E2E authoring

**Status:** planned  
**Type:** Behavior

- **Pre:** Remaining feature folders may violate checklist.
- **Trigger:** Audit + improve.
- **Post:** Checklist satisfied; Cypress green.

**Files:** `mcp/mcp_services.feature`, `ai_generated_content/note_content_completion.feature`, `ai_generated_recall_questions/question_contest.feature`, `testability/feature_toggle.feature`, `testability/show_failure_report.feature`

**Done when:** checklist applied; targeted Cypress passes.

## Out of scope

- Full `pnpm cypress run` / entire suite
- Product feature changes (Jidoka if required)
- Second Cypress interactive CLI harness
- Committing `@focus` / `@only`

## Execution notes

- Progress tracker: **`STATE.md` in this directory** (not `.planning/STATE.md`).
- Phases 1–7 done and pushed (2026-08-07).
- Concurrent `.planning/quick/005-ubiquitous-language-link/` rewrote wiki-link Gherkin/steps/modules; caused rebase conflicts on Phase 4/6. Phase 6 merge kept Kind/Text assertions + glossary “wiki link” wording.
- **Jidoka pause** before Phase 8+: prefer finishing or coordinating with 005 to avoid further topology/frontend collisions. Resume Phase 8 (note topology) when ready — update this plan’s `STATE.md` only.
