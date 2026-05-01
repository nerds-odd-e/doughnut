# Note id URL revert plan

We are abandoning note slug paths as the canonical note URL and returning `noteShow` to a direct note id route.

## Discoveries

- In the Apr 24 snapshot (`9c4e8ddd3`), `noteShow` was `"/n:noteId"` and passed only `noteId: Number(route.params.noteId)` to `NoteShowPage`.
- Current `noteShow` is `"/d/notebooks/:notebookId(\\d+)/notes/:noteSlugPath(.*)"`; `NoteShowPage` resolves the slug path through storage before rendering `NoteShow`.
- The slug route work landed mainly through `4d3431684`, `b7472f5ab`, `840d5088e`, `7d064fcf6`, `a01046d44`, and `cee5110a8`.
- Before ambiguous-slug routing, E2E `jumpToNotePage` used `testability().getInjectedNoteIdByTitle(noteTopology)`, built `/n${noteId}`, and pushed named route `noteShow` with `{ noteId }`. Current E2E derives a basename with `wikiBasenameFromTitle` and routes to `noteShowByAmbiguousSlug`.
- The E2E slug switch also changed `e2e_test/start/router.ts` to push a path fallback instead of named route params, and introduced `e2e_test/start/wikiSlug.ts`.
- Current `WikiTitle` exposes `{ linkText, notebookId, slug }`, but `note_wiki_title_cache` already stores `target_note_id`. Exposing `noteId` should not need a data migration.
- The admin wiki migration currently treats `note_slug_path_regeneration` as a required step. If URLs no longer use slugs, the migration should not be blocked by slug regeneration unless another current behavior still depends on it.

## Target decisions

- Canonical note URLs use note id: `noteShow` receives `noteId` and renders that note directly.
- Wiki-title links use target note id, not notebook id plus slug.
- Existing `note.slug` data can remain temporarily if other current code still needs it. The revert should first remove slug from URL behavior, then delete dead URL-specific slug plumbing once callers are gone.
- Do not keep compatibility shims for the in-progress slug URL design unless production has already shipped those URLs and the team explicitly decides to preserve them.

## Phase 1: Restore direct note-id note pages

Type: Behavior

Goal: a user following any app-generated note link lands on a note page whose route carries only the note id.

Observable behavior:

- Given a note with id `123`,
- when the user follows app navigation to that note,
- then the URL is the note-id route and `NoteShowPage` receives `noteId: 123`,
- and no slug lookup is needed before `NoteShow` loads the note.

Work:

- Change `noteShow` route metadata back to the note-id path from the Apr 24 shape.
- Replace `noteShowByNotebookSlugLocation` / `noteShowByNotebookSlugHref` with note-id helpers.
- Simplify `NoteShowPage` so it takes required `noteId` and removes ambiguous-slug and notebook-slug resolution state.
- Update navigation callers such as toolbar, sidebar, search results, assimilation, conversation close, and undo flows to pass note id.
- Remove frontend usage of `loadNoteByNotebookSlug` and `loadNoteByAmbiguousBasename` from normal note navigation.
- Restore E2E `jumpToNotePage` to use the injected-note-id cache (`getInjectedNoteIdByTitle`) instead of recomposing an ambiguous slug from a title.
- Restore `jumpToNotePageById` if any E2E or page-object flow needs to jump directly by id.
- Restore the E2E router helper to push named route params for `noteShow` where that gives the same fast in-app navigation behavior as before.
- Remove `e2e_test/start/wikiSlug.ts` after no E2E helper imports it.

Tests:

- Route tests prove `noteShow` parses a note id and does not require notebook id or slug path.
- `NoteShowPage` tests prove it passes the route note id directly to `NoteShow`.
- Existing navigation/component tests are updated to assert `params.noteId`, not `params.noteSlugPath`.
- Existing E2E scenarios that already use `jumpToNotePage` prove cached injected note ids still route to the expected note without slug recomposition.
- Run targeted frontend tests for route metadata, `NoteShowPage`, and the navigation components touched.

## Phase 2: Make wiki-title links use target note id

Type: Behavior

Goal: rendered wiki links navigate by note id instead of slug.

Observable behavior:

- Given note details containing a resolved wiki link,
- when the note content is rendered,
- then the live anchor points to the target note-id URL,
- and unresolved wiki links still render as dead links.

Work:

- Change backend `WikiTitle` DTO from `{ linkText, notebookId, slug }` to `{ linkText, noteId }`.
- Change `WikiTitleCacheService.wikiTitlesForViewer` to expose `targetNote.getId()`.
- Update frontend wiki-link rendering to build hrefs from `noteId`.
- Regenerate the TypeScript API client after the backend DTO change.
- Update `RichMarkdownEditor`, markdown conversion, and API-shaped fixtures/tests to use `noteId`.

Tests:

- Backend controller or service test proves `NoteRealm.wikiTitles` contains the target note id.
- Frontend markdown/rendering tests prove `[[Title]]` and upgraded dead-link anchors produce note-id hrefs.
- Run the backend test covering `NoteRealm.wikiTitles`, targeted frontend markdown tests, and API generation checks.

## Phase 3: Unblock admin wiki migration from slug URL work

Type: Behavior

Goal: admin wiki reference migration completes the wiki-reference work without requiring note slug regeneration for URLs.

Observable behavior:

- Given admin migration progress is ready to process wiki references,
- when the admin migration runs,
- then relationship/frontmatter/cache work can complete using target note ids in wiki-title cache output,
- and a failed or unnecessary slug regeneration step does not block wiki-reference completion unless a current non-URL behavior still depends on regenerated slugs.

Work:

- Re-evaluate `note_slug_path_regeneration` in `AdminDataMigrationService`.
- If slug regeneration exists only to support slug URLs, remove it from the active migration step order or mark it as obsolete for new runs.
- If production already has a failed `note_slug_path_regeneration` progress row, define the explicit operator action for ignoring or completing that obsolete step instead of retrying slug batches.
- Keep relationship title/details/cache backfill behavior intact.
- Update `ongoing/admin-wiki-reference-migration-redo-plan.md` and `ongoing/admin-wiki-reference-migration-status.md` after the implementation decision is made.

Tests:

- Admin migration tests prove the migration order no longer blocks cache/reference completion on slug regeneration when slug URLs are disabled.
- Tests still prove relationship backfill and wiki-title cache rows are created with target note ids.
- Run targeted backend admin migration tests.

## Phase 4: Remove dead slug URL surfaces

Type: Structure

Goal: clean up URL-specific slug plumbing after note-id navigation and wiki-title note ids are working.

Work:

- Remove unused frontend storage methods for loading notes by slug.
- Remove unused backend endpoint and generated client code for note retrieval by notebook slug if no current behavior still calls it.
- Remove URL-encoded slash firewall customization if hierarchical slug URLs were its only remaining purpose.
- Delete or rewrite slug-route tests that only protected the abandoned URL design.
- Keep `note.slug`, folder slug, and slug generation code only where another current capability still uses them.

Verification:

- Frontend targeted tests from Phase 1 and Phase 2 still pass.
- Backend controller/admin migration targeted tests still pass.
- Generated OpenAPI/TypeScript client has no stale slug URL client used by the frontend.

## Phase 5: Verify existing note navigation end to end

Type: Behavior

Goal: prove the user-visible app journey is back on note-id URLs.

Observable behavior:

- Given a user opens a notebook, follows note links in the sidebar, search results, note toolbar actions, and rendered wiki links,
- when each navigation occurs,
- then the browser lands on note-id URLs and displays the expected note.
- Given an E2E scenario has created notes through testability,
- when a step calls `jumpToNotePage("Some Note")`,
- then it uses the cached injected note id and opens the note-id URL without depending on ambiguous slug generation.

Tests:

- Do not add new E2E scenarios for this revert.
- Run existing relevant Cypress feature files that already cover note navigation and rendered wiki-link navigation.
- Make sure at least one selected existing scenario exercises the recovered cached-id `jumpToNotePage` helper.
- Run the relevant Cypress feature file with `--spec`, not the full E2E suite.

Deployment and ops:

- Deploy after this phase if all targeted unit/component/backend tests and the relevant E2E pass.
- If production has obsolete failed slug-migration progress, execute the operator action defined in Phase 3 after deploy.
