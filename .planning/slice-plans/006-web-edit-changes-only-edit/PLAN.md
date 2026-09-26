# A web edit changes only what the user edited

## Source

- Story: [SEED-046#story-5](../../seeds/SEED-046-notebook-files-and-git-findings.md#story-5)
- **Identity:** SEED-046#story-5

## Goal and scope

A web edit changes only what was edited:

- the rich editor writes `#` headings, `-` bullets, `*` emphasis and `---` rules;
- a body edit keeps the frontmatter text byte for byte;
- a property panel edit changes only that property's lines, and the panel
  follows file order;
- the server's picture, image-mask, reduce-to-property and trash link-removal
  edits change only the affected property's lines.

Excluded (seed): other body styles (`+`/`*` bullets, `_` emphasis, setext);
keeping the style of a changed property's own value; content the rich editor
loses (story 4a, plan 005); other whole-file writers (export, new notes,
readmes). Preserved: `type: note` → `type: Note` (ADR 0004); author-owned and
unknown keys (ADR 0004); Markdown mode; a property emptied by link removal is
removed; a new frontmatter block is formatted as today.

## Outside-in proof

| Key example | Proof |
| --- | --- |
| 1 manual-test file, one-word body edit → only the body line changes (plus the server's `type: Note`) | slice 2, new `RichMarkdownEditor.changesOnlyTheEdit.spec.ts`; `type: Note` stays covered by `NoteConceptTypeTest` |
| 2 change `description` in the panel → only its line | slice 4, same spec |
| 3 add `source` → one appended line; remove `tags` → only its line | slices 4 and 5, same spec |
| 4 `## Part`, `- item`, `*em*`, `**strong**`, `---`; one-word edit → only that line | slice 1, same spec |
| 5 picture upload → only `image:` added or replaced | slice 7, `NoteControllerUploadNoteImageTests` |
| 6 reduce a relationship note to a property → one appended line | slice 7, `RelationControllerReduceToSourcePropertyTests` |
| 7 trash with link removal → only the `see also` value changes | slice 8, `NoteControllerTrashTests` |
| 8 `+ item`, `_em_` → become `- item`, `*em*` | slice 1, same spec |

The frontend spec mounts the real editor through
`createRichMarkdownEditorTestHarness`. It edits the body with
`quillInstance().insertText(…, "user")` or edits a property through the
harness's property helpers. It asserts the whole emitted Markdown against the
original with only the expected lines changed.

Commands:

- Frontend:
  `CURSOR_DEV=true nix develop -c pnpm frontend:test RichMarkdownEditor markdownizer quillHtmlToMarkdown noteContent`
- Backend:
  `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests '*NoteContentMarkdown*' --tests '*WikiLinkMarkdownDocumentRewrite*' --tests '*NoteControllerUploadNoteImageTests' --tests '*RelationControllerReduceToSourcePropertyTests' --tests '*NoteControllerTrashTests' --tests '*NoteConceptTypeTest'`

## Slices

### 1. The rich editor writes the common Markdown forms

Type: Behavior
Status: planned
Proof: examples 4 and 8 in the new spec. Update the few converter tests that
assert the old forms (`markdownizer.htmlToMarkdown.spec.ts` has about three)
to the new ones; do not add tests for them.

Behavior: a body using `#` headings, `-` bullets, `*`/`**` emphasis and `---`
rules → after a one-word rich edit only the edited line differs. Other forms
normalize to these.

Change: `TurndownService` options `headingStyle: "atx"`,
`bulletListMarker: "-"`, `emDelimiter: "*"`, `hr: "---"` in
`quillHtmlToMarkdown.ts`. Also set them in `markdownizer.ts`'s own instance,
or remove that instance if nothing uses it.

### 2. A body edit keeps the frontmatter text as written

Type: Behavior
Status: planned
Proof: example 1 in the new spec, and a frontmatter holding a comment, a
quoted value and a flow list, unsorted, surviving a body edit byte for byte.
Existing nested-metadata tests stay green.

Behavior: a rich edit of the body → the emitted Markdown is the authored
frontmatter text, the authored separator, then the new body.

Change: introduce the frontend's one in-place frontmatter edit, beside
`composeNoteContentMarkdown` in `noteContentFrontmatter.ts`. It takes the
authored content and the edited property rows. It returns the authored
frontmatter text with only the changed entries spliced, using each entry's
source range from `YAML.parseDocument`
(`pair.key.range[0]`..`pair.value.range[2]`). With no property change it
returns that text unchanged. `RichMarkdownEditor.composeBodyMarkdown` uses it
with the authored prefix and separator, which subsumes today's
nested-metadata verbatim path. A note without frontmatter still composes as
today.

### 3. The property panel lists properties in file order

Type: Behavior
Status: planned
Proof: in the new spec, an unsorted frontmatter (`name`, `description`,
`type`, `tags`) → the panel's row keys appear in that order.

Behavior: opening a note → its properties appear in the order the file has
them; today they are sorted.

Change: drop the sort in `sortedPropertyRowsFromNoteProperties` and
`sortedPropertyRowsFromRecord` and rename them to match; update their unit
tests' expected order only.

### 4. Changing or removing one property changes only its lines

Type: Behavior
Status: planned
Proof: example 2 and "remove `tags`" from example 3 in the new spec.

Behavior: a panel change of one property's value → only that entry's text is
replaced by a freshly written `key: value`; a removal deletes only that
entry's text; comments and other entries stay.

Change: extend slice 2's edit to replaced and removed keys;
`composeNoteContentFromPropertyRows` callers (`onPropertiesChanged`,
`useRichFrontmatterPropertyEditing`) pass the authored content so the edit can
splice.

### 5. Adding or renaming one property changes only its lines

Type: Behavior
Status: planned
Proof: "add `source`" from example 3, and renaming `name` to `title` keeps
the comment above it and the entry's position.

Behavior: a new key → one `key: value` entry appended just before the closing
fence. A rename → only the key text of that entry changes.

Change: extend the same edit to added and renamed keys. No new owner.

### 6. The server has one in-place frontmatter edit

Type: Structure
Status: planned
Proof: `WikiLinkMarkdownDocumentRewrite` and `NoteContentMarkdown` tests stay
green unchanged.

Change: move the range splicing from
`WikiLinkMarkdownDocumentRewrite.rewriteSupportedFrontmatterValues` into one
owner in `algorithms`. Scalar ranges come from SnakeYAML node marks. The owner
can replace a top-level key's value, append a key before the closing fence,
remove a key, and rewrite supported scalar values.
`WikiLinkMarkdownDocumentRewrite` uses it. This enables slice 7 without a
second splicer.

### 7. Setting a picture, an image mask or a reduced link property changes only that line

Type: Behavior
Status: planned
Proof: `NoteControllerUploadNoteImageTests` (example 5) and
`RelationControllerReduceToSourcePropertyTests` (example 6). Each asserts the
note content equals the original with only the one line added or replaced.
Add one `NoteContentMarkdownTest` case for the image mask.

Behavior: the examples. A note without frontmatter gets a new block as today.

Change: `NoteContentMarkdown.setLeadingFrontmatterProperty` uses slice 6's
owner instead of `Frontmatter.set(...).fenced(...)`. That covers
`withNoteImage`, `withNoteImageMask`, `MovedNotePicture`, and
`addPropertyWithAvailableKeyToLeadingFrontmatter`.

### 8. Removing a trashed note's links from properties changes only those values

Type: Behavior
Status: planned
Proof: `NoteControllerTrashTests` with `removeFromProperties()` (example 7).
The referrer's content equals the original with only the `see also` value
changed. `NoteContentMarkdownWikiLinksTest` keeps its emptied-property and
emptied-frontmatter cases green.

Behavior: the example. A value left empty removes its key, and a frontmatter
left with no keys is dropped, as today.

Change: `removeWikiLinksFromLeadingFrontmatterProperties` uses slice 6's
owner (rewrite values, then remove emptied keys) instead of
`Frontmatter.mapStringValues(...).fenced(...)`.

## Current decisions

- Refinement, 2026-09-26: the panel follows file order; `*` emphasis and
  `---` rules count as common forms; the authored separator is kept.
- Planning, 2026-09-26 (proof, `yaml` 2.9.1 from `frontend/node_modules`):
  `node -e` with `YAML.parseDocument(raw)` on a frontmatter holding a comment,
  a quoted value, `tags: [x, y]` and a block scalar, then `set`, `delete` or a
  key replacement, then `doc.toString({ lineWidth: 0 })`. Result: not byte
  preserving — `[x, y]` became `[ x, y ]` even when untouched, a trailing blank
  line was added, and a comment above a renamed key was dropped. But
  `raw.slice(pair.key.range[0], pair.value.range[2])` returned each entry's
  exact text, including a block scalar. So both stacks splice entry text by
  source range, as `WikiLinkMarkdownDocumentRewrite` already does with
  SnakeYAML marks.
- Planning, 2026-09-26: frontend and backend each keep their own splicer,
  since there is no shared runtime. Each has one owner in its stack.
- Planning, 2026-09-26: plan 005 (story 4a) and this plan both touch
  `RichMarkdownEditor.vue`; whichever lands second rebases.

## Learnings

None yet.
