---
id: SEED-058
status: dormant
planted: 2026-09-29
planted_during: owner request to suppress the empty-content rich Markdown warning
trigger_when: improving the editing experience for empty notes
scope: small
---

# SEED-058: Handle the empty-content Markdown discrepancy quietly

## Why This Matters

A note body can look empty while still holding invisible markup, such as
`&nbsp;`, `<p></p>`, or a lone `<br/>`. The rich editor cannot reproduce that
markup, so its "cannot keep" check marks the body as something it would lose:
it shows "This note has content the rich editor cannot keep." and opens the
note read-only. The learner sees an empty box with a warning and cannot type.
A truly empty body (no text, blank lines, or frontmatter only) already opens
editable with no warning.

## Alternatives and Decision

- Hide only the warning and keep the read-only lock: rejected on 2026-09-29.
  The learner would see an empty box that ignores typing, with no explanation.
- Treat a body that shows nothing as empty: chosen. There is nothing visible to
  lose, so the rich editor opens it editable and without the warning. Opening
  the note still changes nothing; the invisible markup is replaced only when
  the learner actually edits in the rich editor.

## Story Decomposition

One story makes blank-looking note bodies open like empty ones in the rich
editor.

<a id="story-1"></a>

### Hide the empty-content rich Markdown mismatch warning

**Identity:** SEED-058#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/013-blank-looking-body-stays-editable/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"1df5bb6587cb05adf9fe81764bf4851460ed9f159709cee3e309101a8558bb4c","plan":"9cd22c6466b31dcdd41e32719af826d04a11fcc38eaf6e9851f686cf44e37023"}}
```

**Goal**

Learners opening a note whose body shows nothing in the rich editor can type
into it straight away, with no warning, and opening it alone does not change
the note.

**Scope**

- A note body that renders to no visible content counts as empty for the rich
  editor's "cannot keep" check: no warning and no read-only lock.
- Opening such a note emits and saves no content change. The first real edit
  in the rich editor saves the rich editor's Markdown, which drops the
  invisible markup; that loss is accepted because nothing visible is lost.
- Bodies with visible content the rich editor cannot keep still get the
  warning and open read-only. Invalid frontmatter still gets its warning.
- Markdown mode is unchanged and still shows the invisible markup as written.
- Deferred: repairing how the rich editor converts such markup, and any
  cleanup of existing blank-looking bodies in stored notes.

**Key examples**

- Body is `&nbsp;` (or `<p></p>`, or `<br/>` and a newline) → open in the rich
  editor → no warning, the editor is editable, and no content change is emitted.
- Same body → learner types "Hello" in the rich editor → the saved body is the
  typed text as rich-editor Markdown, without the earlier invisible markup.
- Body is `<details><summary>S</summary>Inner</details>` → open in the rich
  editor → the existing warning appears and the editor is read-only.
- Body is empty, blank lines only, or frontmatter only → unchanged: editable,
  no warning, no change on open.

- **For / why:** Learners are not blocked by a warning and lock on a note that
  looks empty, while notes with real content keep their protection.
- **Evaluation:** Open a blank-looking note in the rich editor, see no warning,
  type, and see the typed text saved; open a note with content the editor
  cannot keep and still see the warning.
- **Effort hypothesis:** S (30–60 minutes), medium confidence.
- **Depends on:** No queued prerequisite is established; independent of SEED-057.
- **Safe stopping point:** Blank-looking bodies open editable without the
  warning while other "cannot keep" bodies are still protected.

## Ordering and Scope Reduction

Append after the relationship sentence reduction story, preserving earlier
priorities. Limited to bodies that show nothing in the rich editor.

## Approved Product Decision

Terry Yin asked on 2026-09-29 to stop showing the warning for empty notes.
During refinement the same day, the owner chose to let blank-looking bodies be
edited in the rich editor rather than keep them read-only without a warning.

## When to Surface

When selecting empty-note editing and warning presentation improvements.

## Breadcrumbs

- Owner request on 2026-09-29 to hide the empty-content warning; refinement
  found the warning and the read-only lock come from the same check, and the
  owner chose editable over a silent lock.
- Probed on 2026-09-29 with the component test harness: `""`, blank lines,
  whitespace, and frontmatter-only bodies show no warning; `&nbsp;`, `<p></p>`,
  and `<br/>\n` show the warning and open read-only.
- Warning and read-only lock (one shared check): `frontend/src/components/form/RichMarkdownEditor.vue`.
- "Cannot keep" comparison: `frontend/src/components/form/richEditorKeepsBody.ts`.
- Existing "cannot keep" examples: `frontend/tests/components/form/RichMarkdownEditor.bodyItCannotKeep.spec.ts`.
