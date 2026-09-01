---
title: Notebook-scope wiki refresh on title save and note create
date: 2026-09-01
context: Production slowness and lock-wait; probe plan is quick/038.
---

# Notebook-scope wiki refresh on title save and note create

## Symptom (production)

In a large notebook:

- **Title save is slow.** Bigger notebook → slower.
- **Note create is slow** in the same way.
- **Content save is fine** (body-only; aliases unchanged).
- If a **content save runs while a title save is still in flight**, content fails with MySQL `Lock wait timeout` on `UPDATE note … WHERE id=?`. Stack: `TextContentController.updateNoteContent` → `ResolvedWikiLinkService.refreshForNote` → `NotePropertyIndexService.refreshForNote` (`entityManager.flush()`). Example: `PATCH /api/text_content/9169/content`.

Title and content are independent 1s-debounced autosaves, so both PATCHes can overlap.

First guess (note-level-as-frontmatter making every content save heavy) does **not** match: content-only is fast.

## Cause

`refreshNotebookScope` re-resolves **every live note** in the notebook (`findLiveNotesByNotebookIdOrderByIdAsc`, full `MEDIUMTEXT` content, per-token title/alias lookup) **inside the same `@Transactional` request** that already wrote the `note` row.

| Write | `refreshNotebookScope`? | Observed |
|-------|-------------------------|----------|
| Content (no alias change) | no | fast |
| Title change | yes | slow; holds `note` row lock until the walk commits |
| Note create | yes | slow |
| Content while title request still open | — | lock-wait on `UPDATE note` |

Create paths (`NoteConstructionService.createRootNoteWithWikidataService`, `createNoteFromExtractedSuggestion`) call `refreshForNote` (that note only) **then** `refreshNotebookScope`. Title change calls only `refreshNotebookScope`.

Why the walk exists: a new or renamed title can change whether other notes’ `[[Title]]` shorthands are unique, ambiguous, or missing. That is a real cardinality rule, not a bug in the *need* to update — the cost is doing it **synchronously while holding the write lock**.

Show already re-classifies wiki tokens live (`WikiLinkResolver.classifyToken` in `wikiLinksForViewer`), so **note show can still report unique/ambiguous/missing** without a freshly rebuilt `resolved_wiki_link` cache.

## Probe (interim)

Plan: `.planning/quick/038-skip-notebook-scope-refresh-on-title-and-create/PLAN.md`

**Do:** stop calling `refreshNotebookScope` from title update and note create. Keep `refreshForNote` on create. Do not change the method itself or other callers (alias-changing content, delete, restore, cross-notebook move).

**Accepted gap until a later pass:** `resolved_wiki_link` rows on *other* notes can stay stale (inbound-reference lists, health, focus-context sampling). Show resolution should stay correct via live classify. Existing title-rename and create-namesake tests go through `showNote` and should stay green.

**Deploy check:** in a large notebook, time a title rename and a new note. Edit body while a title save is in flight — content PATCH should not lock-timeout. If both hold, the diagnosis is confirmed.

## If the probe works — deeper follow-up (not in 038)

The durable question is how notebook-scope cache rebuild should happen **without** holding the write transaction:

- After-commit / async rebuild (request returns; cache catches up).
- Incremental: only notes whose authored tokens mention the old/new title (or alias).
- Whether inbound/health/focus-context may keep using a stale cache until that rebuild.
- Other callers (delete, restore, move, alias change) still walk the notebook today; they were not the reported symptom.

Do not treat skipping the walk as the long-term design until that pass.
