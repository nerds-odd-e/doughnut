# Phase 9 Sub-Phases — Wiki Link Display Text

## Scope

This breakdown covers only display text for wiki links:

```markdown
[[note title]]
[[note title|display text]]
```

Folder-qualified paths are out of scope for this slice. Do not add `folder/name/note title` parsing, matching, UI, or database behavior while executing these sub-phases.

## Goal

Users can read and author wiki links whose stored target text differs from the visible label. Rich mode shows the display text, while Markdown remains the portable source of truth.

## Design Decisions

- The part before `|` is the target token used for resolution.
- The part after `|` is the display text shown in rich mode.
- If no `|` is present, target token and display text are the same.
- Unresolved links keep their original Markdown token and still show display text in rich mode.
- When a user links a dead-link token to an existing note, rewrite the original token to target the selected note and keep the original wiki text as display text: `[[Selected Note Title|original wiki text]]`.
- Derived cache/index rows may store parsed target and display text, but note content remains the source of truth.

## 9.1 — Rich Mode Shows Display Text for Unresolved Links

**Type:** Behavior  
**Status:** Planned

Pre-condition: A note body contains an unresolved wiki link such as `[[Unknown Topic|friendly label]]`.

Trigger: The user opens the note in rich mode.

Post-condition: The body shows a dead-link anchor with visible text `friendly label`, not `Unknown Topic|friendly label`. Saving without edits preserves the Markdown token.

Implementation notes:

- Start with the smallest frontend test around rich Markdown display and HTML-to-Markdown round trip.
- Parse wiki link inner text into target token plus optional display text before rendering dead-link anchors.
- Keep dead-link click payload rich enough to know both the target token and visible display text; do not rely on `anchor.textContent` alone once display text exists.

Verify:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/utils/wikiPropertyValueField.spec.ts
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.spec.ts
```

## 9.2 — Rich Mode Shows Display Text for Resolved Links

**Type:** Behavior  
**Status:** Planned

Pre-condition: A source note contains `[[Target Note|friendly label]]`, and `Target Note` resolves to an existing note.

Trigger: The user opens the source note in rich mode.

Post-condition: The body shows a live Doughnut link with visible text `friendly label` and navigation target `/d/n/{targetId}`.

Implementation notes:

- Extend the backend wiki-link parser/resolver so resolution uses the target token before `|`.
- Extend the note-show wiki-title data shape only as far as needed for rich rendering: target token, display text, and target note id.
- Update frontend link replacement so `[[Target Note|friendly label]]` becomes a live anchor whose text is `friendly label`.
- Preserve `[[Target Note|friendly label]]` when converting rich editor HTML back to Markdown.

Verify:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/notes/NoteTextContent.spec.ts
```

If generated API types change:

```bash
CURSOR_DEV=true nix develop -c pnpm generateTypeScript
```

## 9.3 — Insert Wiki Link Supports Explicit Display Text Internally

**Type:** Structure  
**Status:** Planned

Pre-condition: Existing search insertion can build only `[[Target Note]]` or notebook-qualified equivalents.

Trigger: Production code needs to rewrite a dead-link token to an existing selected note while preserving a different visible label.

Post-condition: The wiki-link text builder can produce `[[Target Note|display text]]` for existing-note targets without changing current toolbar insertion behavior.

Implementation notes:

- Keep the existing toolbar "Insert as a wiki link" output unchanged when no display text is requested.
- Add a focused unit test for the builder/helper that proves display text is included only when supplied and meaningful.
- Continue to avoid folder paths.

Verify:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/InsertWikiLink.spec.ts
```

## 9.4 — Dead Link Can Be Linked to an Existing Note

**Type:** Behavior  
**Status:** Planned

Pre-condition: A note contains an unresolved wiki link such as `[[original wiki text]]`, and a suitable existing note can be found by search.

Trigger: The user clicks the dead link, searches in the dialog, selects an existing note, and chooses to link to it.

Post-condition: The source note content is rewritten to `[[Selected Note Title|original wiki text]]`; rich mode shows `original wiki text` as a live link to the selected note.

Implementation notes:

- Reuse the existing note/folder search UI where practical, but expose a dead-link-specific action so the user can choose an existing note instead of creating a new note.
- The rewrite should target the clicked occurrence, not every matching token in the document.
- Refresh the source note's wiki-title cache after rewriting so references and graph behavior stay coherent.
- Preserve the current "create a new note from dead link" path as an option.

Verify:

```bash
CURSOR_DEV=true nix develop -c pnpm frontend:test tests/links/SearchDialog.spec.ts
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/note_creation_and_update/note_creation.feature
```

## 9.5 — Display Text Drives Link Index Semantics

**Type:** Behavior  
**Status:** Planned

Pre-condition: A source note has display-text links and normal wiki links.

Trigger: The source note is saved, reloaded, and graph/reference surfaces read derived link data.

Post-condition: Outgoing/backlink behavior is based on the resolved target token, while UI surfaces that render link text can use display text when appropriate.

Implementation notes:

- Keep index/cache data derived from note content.
- Store or expose display text only where an observed consumer needs it.
- Do not broaden this into folder-qualified resolution.

Verify:

```bash
CURSOR_DEV=true nix develop -c pnpm backend:test_only
```

## Completion Criteria

- Rich mode displays the label side of `[[target|label]]` for both resolved and unresolved links.
- Markdown save/round-trip preserves display text.
- Existing simple wiki links keep current behavior.
- Dead-link flow supports selecting an existing note and rewrites the clicked token to keep the original text as display text.
- No folder-path resolution or folder-path authoring syntax is implemented in this phase.
