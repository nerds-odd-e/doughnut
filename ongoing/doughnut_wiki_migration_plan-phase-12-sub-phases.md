# Phase 12 Sub-Phases — Title Rename Reference Handling

## Goal

When a note with inbound wiki references is renamed, the user must explicitly choose how existing references should behave. The system should never silently strand stale tokens, and notebook moves should keep references pointing at the moved note.

## Product Language

Use two explicit save choices for a referenced note title change:

- **Update visible reference text** — rewrite references to the new title. Links that already have display text keep that display text.
- **Keep visible reference text** — keep what readers currently see, while retargeting the link to the renamed note. Plain `[[Old title]]` links become `[[New title|Old title]]`; existing `[[Old title|custom text]]` links become `[[New title|custom text]]`.

The backend option names can be domain-focused, for example `UPDATE_VISIBLE_TEXT` and `KEEP_VISIBLE_TEXT`.

## Behavior Contract

- Existing title auto-save behavior remains for notes with no inbound references.
- When the note has inbound references and the user edits the title, debounced title auto-save stops for that dirty title. The UI shows an explicit **Save** control below the title with the two choices above.
- A title update request that changes the title of a note with inbound references must include one of the two reference-handling choices. If no choice is provided, the backend throws a validation/domain exception. The frontend does not need special handling for this exception because the referenced-note UI should not make that request.
- Display text is always preserved when a link already has explicit display text.
- Reference rewrites must refresh derived wiki-title cache rows for every changed referrer so `NoteRealm.references`, graph, and search stay consistent.

## Sub-Phases

### 12.1 — Backend Rejects Ambiguous Referenced Renames

**Type:** Behavior

**Pre-condition:** A target note has at least one inbound wiki-title-cache row from a non-deleted referrer.

**Trigger:** The title update endpoint receives a changed title without a reference-handling choice.

**Post-condition:** The request fails before changing the title or referrer content.

**Tests:**

- Add/extend controller-level backend tests around `TextContentController.updateNoteTitle`.
- Cover: no inbound references still renames without a choice; inbound references plus changed title fails without a choice; unchanged title does not require a choice.

**Implementation notes:**

- Extend `NoteUpdateTitleDTO` with an optional reference-handling enum.
- Keep the exception backend-only; do not add special frontend error handling for this path.
- Use the existing inbound reference lookup from `NoteWikiTitleCacheRepository` / `WikiTitleCacheService` rather than reparsing all notes.

### 12.2 — Rewrite References When Visible Text Should Update

**Type:** Behavior

**Pre-condition:** A target note has inbound references including plain wiki links and links with explicit display text.

**Trigger:** The title update endpoint receives the changed title with `UPDATE_VISIBLE_TEXT`.

**Post-condition:** Plain links point at and display the new title; links with explicit display text point at the new title while keeping their display text. Changed referrers have refreshed wiki-title cache rows.

**Tests:**

- Backend test through the title update endpoint.
- Include both Markdown body and frontmatter/property references if the existing wiki-link parser supports both paths.
- Assert referrer `content`, refreshed cache target, and returned `NoteRealm.references`.

### 12.3 — Rewrite References While Keeping Visible Text

**Type:** Behavior

**Pre-condition:** A target note has inbound references including at least one plain `[[Old title]]` link.

**Trigger:** The title update endpoint receives the changed title with `KEEP_VISIBLE_TEXT`.

**Post-condition:** Plain links become display-text links to the new title, and existing display-text links keep their display text. Changed referrers have refreshed wiki-title cache rows.

**Tests:**

- Backend test through the title update endpoint.
- Assert `[[Old title]]` becomes `[[New title|Old title]]`.
- Assert `[[Old title|custom text]]` becomes `[[New title|custom text]]`.

### 12.4 — Referenced Note Title Edit Uses Explicit Save Choices

**Type:** Behavior

**Pre-condition:** A user is viewing a note that has inbound references shown in the References section.

**Trigger:** The user edits the note title.

**Post-condition:** Debounced title auto-save does not fire for the dirty title. A Save control appears below the title with **Update visible reference text** and **Keep visible reference text** choices. Choosing either option saves the title and applies the matching backend rewrite.

**Tests:**

- Add one successful E2E scenario to the wiki-link capability feature, likely `e2e_test/features/note_topology/link.feature`.
- Prefer the main success scenario for **Keep visible reference text**, because it proves the user-facing distinction: the referenced note title changes, the referrer still displays the old text, and the link still opens the renamed note.
- Add focused frontend component/unit tests only if the E2E cannot clearly prove that auto-save is suppressed before the explicit choice.

**Implementation notes:**

- Keep existing title behavior for unreferenced notes.
- The UI can use `noteRealm.references.length > 0` as the visible precondition; if backend visibility rules differ, the backend exception remains the final guard.
- Update generated frontend API types after the DTO change:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

### 12.5 — Moving A Note To Another Notebook Updates References

**Type:** Behavior

**Pre-condition:** A note has inbound references and is moved to another notebook.

**Trigger:** The existing note move-to-notebook-root flow moves the note across notebook scope.

**Post-condition:** Referrers are rewritten so their links still resolve to the moved note from their own notebook scope, and wiki-title cache rows are refreshed.

**Tests:**

- Backend test through the existing move endpoint/service.
- Include a same-notebook referrer and a cross-notebook referrer if both are supported by current resolution rules.
- Assert the rewritten token uses the correct notebook-qualified target text when needed, while preserving explicit display text.

**Implementation notes:**

- Reuse the Phase 12 reference rewrite operation rather than adding a move-specific parser path.
- This phase should not change the move UI unless the existing behavior already exposes cross-notebook moves.

## Verification

Run targeted checks only:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test <focused test file if added>
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_topology/link.feature
```

## Stop-Safe Boundaries

After **12.1**, silent ambiguous referenced renames are blocked even before rewrite behavior exists. After **12.2** or **12.3**, at least one explicit user choice is shippable from API clients. After **12.4**, the browser flow is usable end-to-end. After **12.5**, containment moves and reference identity are coherent across notebooks.
