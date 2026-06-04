# Reduce a relation note to a property of its source on delete

## Requirement (as given)

When deleting a note, if the note is a **relation note** and has the **necessary properties**, it could be **reduced to a simple property of the source note** instead of being deleted outright.

- If the condition is fulfilled, the delete action shows a **dialog** asking the user whether to **delete the note** or **reduce it to a property of the source**.
- The **name of the property** will be the **same as the relation**.
- If the property **already exists in the source**, the action must **fail with a proper message**.

## Domain analysis (current state of the codebase)

- A **relation note** is a normal `Note` whose YAML frontmatter encodes the relationship. There is **no** separate entity. Shape:

```
---
type: relationship
relation: a-part-of          # kebab-case relation
source: "[[Moon]]"           # wiki link to source note
target: "[[Earth]]"          # wiki link to target note
---
(optional body)
```

  Built/parsed by `frontend/src/utils/relationshipNoteCompose.ts`, `frontend/src/models/relationTypeOptions.ts`, `frontend/src/utils/noteContentFrontmatter.ts`; backend `Frontmatter` (`backend/.../algorithms/Frontmatter.java`) and `NoteContentMarkdown`.

- **Properties** are the same flat YAML frontmatter key/value pairs in `Note.content`. There is already a helper that **removes** wiki links from frontmatter (`NoteContentMarkdown.removeWikiLinksFromLeadingFrontmatterProperties`); we will need an **add-property** counterpart.

- **Deletion today**: `POST /api/notes/{note}/delete` → `NoteController.deleteNote` → `NoteService.destroy(note, referenceHandling, viewer)` (soft delete). The handling enum `NoteDeleteReferenceHandling` is `{ REMOVE_FROM_PROPERTIES, LEAVE_DEAD_LINKS }`. UI flow is `frontend/src/composables/useNoteDeleteFlow.ts` → `popups.confirm` / `popups.options` → `StoredApiCollection.deleteNote`.

- A relation note normally has **no inbound references**, so deleting it currently shows the plain `confirm` dialog. Its **source** and **target** notes list the relation note in their `references`.

- **No existing feature** reduces a relation note to a source property.

### Interpretation / design decisions

1. **Reduce target**: the property is added to the **source** note (the note named by `source:`). Property **key** = the **human-readable relation label** (e.g. `a part of`), derived from the kebab `relation` value (`a-part-of`) via the relation-type mapping; for a custom relation the label is the relation text itself. Property **value** = the `target` wiki link (e.g. `"[[Earth]]"`). After reduction the relation note is **soft-deleted**. Resulting source frontmatter line: `a part of: "[[Earth]]"`.
2. **Kebab → label mapping lives on the frontend**. The kebab→label mapping already exists only in `frontend/src/models/relationTypeOptions.ts` (and correctly handles custom relations). To avoid duplicating/inverting it on the backend, the **frontend computes the property key (label)** and passes it to the delete API; the backend uses the provided key as-is for the conflict check and insertion. (Built-ins happen to be `label.replace(' ', '-')`, but custom relations are not a clean inverse, so we do not derive the label on the backend.)
3. **Qualifying condition** ("necessary properties"): the note is a relationship note with a non-empty `relation`, a `source` that resolves to an existing note, and a `target` value. If any is missing/unresolvable, the reduce option is **not** offered and the normal delete flow is unchanged.
4. **Where the work happens**: the reduction is performed **on the backend** as a new delete mode, so the existence check and atomic "add property + delete note" happen server-side (the conflict failure must be authoritative). Proposed: add a new `NoteDeleteReferenceHandling` value `REDUCE_TO_SOURCE_PROPERTY` plus an optional property-key field on `NoteDeleteDTO` carrying the label from decision 2.
5. **Conflict = no data loss**: if the source already has a property whose key equals the relation label, the operation **fails** (note is NOT deleted, source unchanged) with a clear message. The guard ships in Phase 1 so we never silently overwrite.
6. Refresh the wiki-title cache for the source note after reduction (same as existing frontmatter-editing paths).

### Resolved / open questions

- **Q1 (resolved)**: Property key = the **human-readable relation label** (e.g. `a part of`), not the kebab.
- **Q2 (resolved)**: Property value = the `target` wiki link verbatim (e.g. `"[[Earth]]"`).
- **Q3 (open)**: Cross-notebook source (`source: "[[Notebook: Title]]"`) — reduce only when source resolves in the current notebook scope; otherwise treat as non-qualifying. (assumed)

---

## Phases

Two user-visible behaviors, each split into commit-sized phases. Every phase ends at a clean, committable, push-able state (CI green: `@wip` E2E scenarios are skipped in CI, so an in-progress scenario does not break the gate). One intentionally failing test at a time — the `@wip` scenario being driven.

### Behavior A — Reduce a qualifying relation note to a source property

End state: deleting "Moon a part of Earth" and choosing "Reduce to a property of the source" adds `a part of: "[[Earth]]"` to "Moon" and removes the relation note.

#### Phase 1 — E2E red: happy-path scenario (`@wip`)
- Add a `@wip` scenario to `e2e_test/features/relationships/relationship_edit_and_remove.feature`: delete "Moon a part of Earth", choose reduce, then assert Moon's content contains `a part of: "[[Earth]]"` and "Moon" no longer relates to "Earth".
- Add the page-object method (reduce choice) in `notePage.ts`/`noteMoreOptionsForm.ts` and the step definition in `relationship.ts`/`note.ts`.
- Run `cypress run --spec …relationship_edit_and_remove.feature`; confirm it fails for the **right reason** (option not offered / not implemented).
- **Commit**: failing scenario is `@wip` (CI-skipped). No production change.

#### Phase 2 — Structure: frontmatter add-property helper
- Add `addPropertyToLeadingFrontmatter(content, key, value)` to `NoteContentMarkdown` (pure, immutable), next to the existing remove helper. Returns updated content or signals an existing-key conflict.
- Black-box unit tests (pure function): adds to existing frontmatter, creates frontmatter when none, reports conflict when key exists.
- No behavior change yet (used by Phase 3). **Commit**.

#### Phase 3 — Behavior green (backend): reduce via the delete API
- Add `REDUCE_TO_SOURCE_PROPERTY` to `NoteDeleteReferenceHandling` and a property-key field to `NoteDeleteDTO`.
- In `NoteService`, add a reduce path: parse the relation note frontmatter, resolve the `source` note, add `<key=label>: <target>` to the source via the Phase 2 helper, refresh the source's wiki cache, then soft-delete the relation note. **Conflict guard**: if the source already has that key, abort with a clear message — note NOT deleted, source unchanged.
- Controller tests (observable HTTP surface): happy-path reduce; conflict aborts with message (non-happy path).
- Regenerate the TS client: `pnpm generateTypeScript`.
- **Commit**. (Happy-path E2E still `@wip` — frontend not wired yet; CI green.)

#### Phase 4 — Behavior green (frontend): offer the choice → happy path live
- In `useNoteDeleteFlow`, detect a qualifying relation note from cached `NoteRealm.content` (relation + resolvable source + target), compute the relation **label** via `relationTypeOptions`. When qualifying, show an `options` dialog ("Reduce to a property of the source" vs "Delete this note") and pass the handling **and the label** through `StoredApiCollection.deleteNote`.
- Frontend unit tests: qualifying → reduce dialog and correct payload; non-qualifying / non-relation / unresolvable source → existing confirm/options unchanged.
- Run the Phase 1 `--spec` until green; **remove `@wip`**.
- **Commit**. Behavior A delivered end-to-end.

### Behavior B — Conflict: source already has the property

End state: choosing reduce when "Moon" already has an `a part of` property shows a clear message and changes nothing.

#### Phase 5 — Conflict UX (E2E red → green)
- Add a `@wip` scenario in `relationship_edit_and_remove.feature`: "Moon" pre-seeded with an `a part of` property; delete the relation note, choose reduce; assert the error message is visible and "Moon" still relates to "Earth". Run `--spec`; confirm right-reason failure.
- Ensure the backend conflict message (from Phase 3) **surfaces to the user** via the existing `apiCallWithLoading` error path (alert popup). Add a frontend unit test for the surfaced message.
- Run `--spec` until green; **remove `@wip`**.
- **Commit**. Behavior B delivered.

---

## Status

- Phase 1 — done
- Phase 2 — done
- Phase 3 — done
- Phase 4 — done
- Phase 5 — done

## Notes for executor

- Run tooling via Nix: `CURSOR_DEV=true nix develop -c …`. Git runs directly.
- After backend controller/DTO signature changes, regenerate the TS client: `CURSOR_DEV=true nix develop -c pnpm generateTypeScript`.
- Targeted checks: backend `pnpm backend:test_only`; frontend single file `pnpm frontend:test tests/...`; E2E single feature `pnpm cypress run --spec e2e_test/features/relationships/relationship_edit_and_remove.feature`.
- Deploy gate between phases: commit, push, let CD deploy.
