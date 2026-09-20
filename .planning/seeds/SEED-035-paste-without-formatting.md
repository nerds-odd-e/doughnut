---
id: SEED-035
status: dormant
planted: 2026-09-20
planted_during: owner request to capture paste without formatting in the product backlog
trigger_when: improving pasting into the rich and Markdown note editors
scope: small
---

# SEED-035: Paste without formatting

## Why This Matters

Note authors sometimes copy Markdown from a source that puts formatted HTML
on the clipboard as well as text. The owner reports that pasting in either
rich mode or Markdown mode converts the formatted content into Markdown,
escaping the original Markdown syntax. Authors need a way to use the copied
text without importing the source's formatting or repairing added escapes.

## Alternatives and Decision

Provide an explicit paste-without-formatting option in both editor modes.
Keep ordinary paste available for importing formatted content. Manually
passing content through a plain-text editor is a workaround, but interrupts
writing and should not be necessary for this operation.

## Story Decomposition

<a id="story-1"></a>

### 1. Paste without formatting in rich and Markdown modes

- **Identity:** SEED-035#story-1
- **Goal / beneficiaries:** Note authors can paste copied text, including
  Markdown, without source formatting changing or escaping its content.
- **Scope:** Offer an explicit paste-without-formatting action in both editor
  modes. Use the clipboard's plain text instead of converting its HTML.
  Preserve Markdown syntax and line breaks without introducing escaping from
  the source formatting. Rich mode may render the resulting Markdown using
  its normal presentation; the underlying Markdown must remain intact.
- **Key examples:**
  - The clipboard contains plain text `**important**` and an HTML representation
    that displays those literal asterisks. Paste without formatting in Markdown
    mode inserts `**important**`, without added backslashes or HTML formatting.
  - Paste the same clipboard content without formatting in rich mode: the
    resulting note retains `**important**` as Markdown, verifiable by switching
    to Markdown mode, rather than escaping the asterisks as literal text.
  - Copy text with multiple lines from a formatted source: paste without
    formatting preserves the text and line breaks while discarding source
    styles. Ordinary paste remains available with its existing behavior.
- **Evaluation:** The examples work in both modes using the explicit option,
  including when the clipboard provides both HTML and plain-text content.
- **Deferred promises:** Automatic detection of incorrectly formatted source
  Markdown and changes to ordinary paste conversion are not part of this story.
- **Value / learning:** Remove manual cleanup when copying Markdown from
  sources that advertise it as formatted content.
- **Effort hypothesis:** S (roughly 30–60 minutes), low confidence until the
  existing paste handling and supported browser interactions are inspected.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Both editor modes support the explicit option while
  ordinary formatted paste remains usable, independently of other editor work.
- **Open decisions for refinement:** Choose how users invoke the option
  (keyboard shortcut, editor menu, or both) within supported browser behavior.

## Ordering and Scope Reduction

Queue after faster note-content saving and before the note-presentation cleanup
that the owner explicitly deferred to last. Preserve the near-future direction.
Keep this as one story covering the same paste choice in both editor modes.

## When to Surface

When improving copying and pasting into the note editor, or selecting this
story from the product backlog.

## Breadcrumbs

- Owner request, 2026-09-20: capture a new backlog story for pasting without
  formatting in rich and Markdown modes, especially when copied Markdown is
  presented by the source as formatted HTML and normal paste escapes it.
- Backlog capture only; implementation and execution planning are not requested.
