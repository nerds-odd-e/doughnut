---
id: SEED-049
status: dormant
planted: 2026-09-27
planted_during: owner-requested backlog capture after SEED-047#story-1
trigger_when: selected from the product backlog
scope: medium
---

# SEED-049: Give the frontend note store a cohesive architecture

## Why This Matters

`frontend/src/store/StoredApiCollection.ts` holds most note-changing behavior
in one class: loading and caching note realms, note creation, text and content
edits, image upload, wiki-link cache refresh, undo, trash, permanent deletion,
removal landing, relationship reduction, and moves. It was about 380 lines
before SEED-047#story-1 and about 403 after it, well over the project's
250-line file guide. Each new note behavior lands there by default, so
unrelated concerns keep accumulating in one place.

## Story Decomposition

<a id="story-1"></a>

### Give the frontend note store a cohesive architecture

**Identity:** SEED-049#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/016-note-store-architecture/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"e944ed66a1ff450fb66671388e17b994e7a1f3cd1f7aecd856c4a9967e4e6d30","plan":"94ae3283d84520f9ea2436d384ab32fc047cd4ab97bc3c1a8378e5d3a1032878"}}
```

**Goal:** Developers and AI agents changing note behavior in the web frontend
find one small, domain-named note store: note commands in one place, undo
(its records and how each is reversed) in another, reached through one entry
point. A change to one concern does not grow or touch an unrelated one; each
store file stays within the 250-line guide (today `StoredApiCollection.ts` is
about 403 lines). Simplification comes before decomposition: most of the size
is removable, and only one split is made. Product behavior is unchanged except
that creating a note or relationship sends fewer requests; responsiveness stays
the same or better.

**Assumes:** SEED-047#story-2 is completed first (owner decision 2026-09-27),
so removal landing already loads its listing through the shared user-action
load and builds rows through the one sidebar-rows function.

**Scope:**

- **Delete the wiki-link cache re-save outright.** The backend dropped its
  resolved wiki-link cache (`V300000316__drop_resolved_wiki_link.sql`); links
  resolve live. Remove `refreshWikiLinkCacheForNote`, the
  `refreshWikiLinkCacheForNoteIds` option through `createRootNoteAtNotebook`,
  `noteNewFormSubmit.ts`, `NoteNewForm.vue` (`wikiLinkCacheRefreshSourceNoteId`)
  and `AddRelationshipFinalize.vue`, the `sourceNoteId` prop of
  `NoteUnresolvedWikiLinkModal` passed from `NoteShow`, and its store spec.
  Nothing replaces it: no reload of the source note, no test or note of its
  absence (owner decision 2026-09-27).
- **Simplify commands in place:** one move command with a folder-or-root target;
  one way to read a note (a ref that loads when needed) and one load; drop
  `getNoteRealmRef`, the duplicate load, `noteReferenceHandlingBody` and the `NoteTrashOptions` alias, the
  `placementUndoForNote` null guards, and return values no caller uses.
- **One failure style in note requests:** every request throws on failure
  (ADR 0006); the `return undefined` / `return boolean` paths and the
  commands' `if (!result) return` branches go away. The error toast
  `apiCallWithLoading` already shows stays as it is. Field-error enrichment for
  title and creation stays, because forms show it.
- **Removal landing leaves the store:** the "where to land after removing"
  rule sits with the sidebar order it depends on, and the removal flow reads
  the destination before removal and navigates after it; trash and permanent
  delete commands take no router. The store no longer imports sidebar,
  peer-sort or sidebar-tree code.
- **Undo is one module:** the history, its record types, and how each record is
  reversed live together; reversing returns the route to open. Its state is
  explicitly reactive. `NoteEditingHistory.ts` and `undoInner`'s ad-hoc
  `{noteRealm, notebookFallbackId}` shape go away.
- **One entry point:** a module singleton reached by `useNoteStore()` replaces
  `useStorageAccessor()`, `createNoteStorage`, `AccessorImplementation`, and
  the per-call `storedApi()` object. Callers write `noteStore.command(...)`.
  Tests reset it through one helper instead of assigning a new accessor.
- **Tests observe through high-level entry points:** component specs own the
  behavior; store-level specs that repeat a component spec are removed rather
  than moved.

**Deferred:** in-flight load de-duplication, optimistic updates, and Pinia or
any other new state library.

**Key examples:**

1. Typing in a note's content → debounced autosave saves it → only that note's
   views re-render and the sidebar listings are not refreshed (as today).
2. Renaming a note's title → the sidebar listing shows the new title (as today).
3. Creating a note from an unresolved wiki link, or creating a relationship
   note → one create request; no follow-up content save of the source or
   target note.
4. Trashing a note, then undo → the note is back in its folder under its old
   title and the app opens it (as today); the undo button's label follows the
   latest undoable action without relying on a deep-reactive wrapper.
5. Undo after creating a note → the note is trashed and the app opens its
   notebook (as today).
6. A trash, permanent delete, reduce-to-property or image upload request
   fails → the error toast shows (as today) and the command stops by throwing,
   not by a silent early return.
7. Moving a note to a folder or to a notebook root, then undo → it returns to
   its previous folder or notebook root (as today).

**Architecture (owner-approved 2026-09-27):**

- Shared state stays in two owners: the note realm cache (one reactive ref per
  note; this is what keeps re-rendering fine-grained) and the undo history.
  Router and sidebar refresh stay parameters and collaborators, not state.
- Components and composables reach both through `useNoteStore()`, following the
  project's existing module-singleton idiom (`useFeatureToggle`,
  `useRecallData`); no Pinia exists and none is added.
- Rejected: a mechanical per-concern split into about five files (owner prefers
  simplification, and most of the size is removable); Pinia (a new dependency
  for what module singletons already do); command classes for undo (more
  structure than one small switch needs); optimistic updates (a behavior
  change, not a simplification); replacing the wiki-link re-save with a
  reload (owner: remove without replacement).

## When to Surface

Owner priority: first in the product backlog as of 2026-09-27.

## Breadcrumbs

- Owner request, 2026-09-27, after executing SEED-047#story-1: add the
  `StoredApiCollection.ts` improvement to the product backlog as its own
  story with the highest priority; it needs a more careful architectural
  design to refine and plan it.
- Owner review, 2026-09-27: refine assuming SEED-047#story-2 is done;
  include the single entry point; delete the wiki-link re-save cleanly with
  nothing left behind.
- The refactor passes for SEED-047#story-1 flagged the file size and
  recorded the split as out of that story's scope (`edfd7ba92a:.planning/slice-plans/014-continue-to-neighboring-note-after-deletion/PLAN.md`).
