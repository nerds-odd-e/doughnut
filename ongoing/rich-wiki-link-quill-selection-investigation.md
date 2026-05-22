# Rich wiki link Quill selection investigation

## Why Quill throws `Cannot read properties of null (reading 'offset')`

The reproduced failure is caused by replacing `quill.root.innerHTML` while the
browser native selection still points at a DOM text node that was just removed.
After that, a later Quill API call such as:

```ts
quill.deleteText(index, oldLen, Quill.sources.SILENT)
```

can crash inside Quill selection normalization:

```text
TypeError: Cannot read properties of null (reading 'offset')
  at quill.js ... normalizedToRange
  at quill.js ... getRange
  at quill.js ... update
  at quill.js ... getSelection
  at quill.js ... modify
  at quill.js ... deleteText
```

The important point is that `SILENT` does not mean "do not touch selection".
Some Quill mutation APIs still go through code paths that inspect the native
selection. If that native selection refers to a detached node from the old
`innerHTML`, Quill tries to convert an invalid browser range into a Quill range
and crashes.

### Full frontend unit reproduction

The smallest useful reproduction uses the real `QuillEditor.vue` component and
a real Quill instance in the browser test environment:

1. Mount `QuillEditor.vue` attached to `document.body`.
2. Get the real Quill instance from the component.
3. Insert raw wiki text, for example `Hello [[MyNote]]`.
4. Put the browser native selection at the end of that text node.
5. Replace `quill.root.innerHTML` with HTML containing an anchor:

   ```html
   <p>Hello <a href="/n42" class="doughnut-link" data-wiki-title="MyNote">MyNote</a></p>
   ```

6. Call a Quill mutation such as:

   ```ts
   quill.deleteText(6, "[[MyNote]]".length, Quill.sources.SILENT)
   ```

7. The test should assert that the call throws `/offset/`.

This reproduces the actual class of production crash better than a helper-level
unit test because the failure depends on a real Quill editor, real DOM nodes,
and the browser selection object.

## Requirement for wiki link editing

In Rich mode, typing a completed wiki token such as `[[MyNote]]` should turn it
into a Doughnut wiki link once enough information is available.

The editor needs to support at least these cases:

- A completed raw token becomes a live link when the title is present in
  `wikiTitles`.
- A completed raw token becomes a dead link when the title is not present.
- A dead link later becomes a live link when the backend/API returns an updated
  wiki title list.
- The cursor must remain near the completed wiki link, usually after the display
  text, rather than jumping to the start or disappearing.
- Markdown serialization must stay canonical, including `data-wiki-title` and
  optional `data-wiki-display`.
- Other Rich editor behavior should continue using the existing full HTML
  normalization path where appropriate: initial load, unrelated prop changes,
  paste handling, frontmatter changes, and broad markdown-to-HTML refreshes.

The narrow target is only the wiki-link display transition. Broadly replacing
the editor root for that transition is what makes selection stability fragile.

## Why the problem is hard

The hard part is that wiki-link display state is not purely local editor state.
Whether a link is live or dead depends on `wikiTitles`, which comes from outside
the editor and can change after the user types.

That means the editor has to handle two different transformations:

1. Raw text token to anchor:

   ```html
   [[MyNote]]
   ```

   to:

   ```html
   <a href="/n42" class="doughnut-link" data-wiki-title="MyNote">MyNote</a>
   ```

2. Dead anchor to live anchor when backend data arrives:

   ```html
   <a href="" class="dead-link" data-wiki-title="MyNote">MyNote</a>
   ```

   to:

   ```html
   <a href="/n42" class="doughnut-link" data-wiki-title="MyNote">MyNote</a>
   ```

The first case changes editor text length and formatting. The second case keeps
visible text stable but changes anchor attributes. They probably need different
implementation paths and different tests.

## Emit, props, watch loop complication

The Rich editor has a loop like this:

1. User types in Quill.
2. `QuillEditor.vue` emits `update:modelValue` with raw Quill HTML.
3. `RichMarkdownEditor.vue` converts that HTML to markdown.
4. The parent receives emitted markdown and sends it back through
   `props.modelValue`.
5. `RichMarkdownEditor.vue` recomputes HTML from markdown and `wikiTitles`.
6. `QuillEditor.vue` receives the computed HTML through its `modelValue` prop
   watcher.

The timing is important. When the user completes `[[MyNote]]`, the raw emitted
value can come back through props before the linkified replacement is applied.
If the child watcher sees a difference at that moment and writes
`quill.root.innerHTML`, it can replace the live editor DOM while the browser
selection is still inside the old raw text node. That creates the stale
selection that later causes Quill API calls to crash.

A second complication is that `wikiTitles` can change independently after the
raw edit loop has settled. The editor therefore needs to react to backend/API
wiki-title updates without treating them as a generic full-content prop
replacement during active editing.

## Learnings from the attempted Delta approach

Using Quill Delta operations is still directionally promising, but there are
sharp edges:

- A single combined `retain(index).delete(rawLen).insert(display, { link })`
  operation did not behave reliably in the local experiment; in one observed
  path it produced duplicated text such as `Hello MyNote[[MyNote]]`.
- Splitting the operation into a delete Delta followed by an insert Delta
  avoided that duplication in the experiment, but this needs more confidence
  before keeping it.
- Even `updateContents` can interact with Quill selection internals. Clearing or
  preserving native selection needs to be deliberate, not hidden behind
  try/catch.
- Swallowing Quill exceptions is the wrong direction for this bug. The first
  priority should be tests that reproduce the exact crash and fail loudly.
- Restoring only an internal saved Quill range is not enough. The browser native
  selection can still be detached, and that is what Quill later reads.
- Watcher-level no-ops are important. If the live editor HTML already equals the
  incoming raw prop echo, the watcher should not replace `innerHTML`.
- Dead-link to live-link updates may be better represented as an attribute-only
  DOM update on the existing anchor, because visible text and selection position
  do not need to change.

## Suggested next investigation path

Before attempting another production fix, keep these tests in mind:

1. Reproduce the stale-native-selection crash with a real Quill editor.
2. Assert that completing `[[MyNote]]` does not assign to
   `quill.root.innerHTML`.
3. Assert that the raw prop echo after typing does not assign to
   `quill.root.innerHTML`.
4. Assert that a later `wikiTitles` update changes a dead link to a live link
   without assigning to `quill.root.innerHTML`.
5. Assert both Quill's saved range and the browser native selection remain near
   the completed link.
6. Keep a separate test proving unrelated prop HTML changes still use the
   existing full `innerHTML` replacement path.

These tests should be browser component tests rather than pure helper tests
because the bug depends on Quill, DOM replacement, and the browser selection
object interacting together.

## Notes on reverted attempt

The local commit being removed was:

```text
c4d7a65b97 feat: enhance QuillEditor with wiki link delta replacement functionality
```

That commit and the later uncommitted follow-up attempted to add helper-level
DOM diffing, Delta replacement, raw-echo watcher no-ops, and active-editor
wiki-title updates. The tests were useful, but the implementation felt too
risky to keep because the selection and watcher interactions are subtle and the
behavior needs a cleaner design pass.
