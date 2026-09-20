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

The owner selected a temporary floating action after ordinary paste: **Use as
Markdown** replaces that insertion using the original clipboard text. It
works in both editor modes and supports touch without a keyboard shortcut.
Ordinary paste still imports formatted content immediately. Manually passing
content through a plain-text editor interrupts writing; a shortcut alone
does not help users who discover the problem after pasting.

## Story Decomposition

<a id="story-1"></a>

### 1. Paste without formatting in rich and Markdown modes

- **Identity:** SEED-035#story-1
- **Plan:** [Paste without formatting](../quick/260922-paste-without-formatting/PLAN.md)
- **Goal / beneficiaries:** Note authors can paste copied text, including
  Markdown, without source formatting changing or escaping its content.
- **Scope:** After a paste carrying HTML and non-empty plain text, offer one
  floating **Use as Markdown** action near the insertion. Activating it replaces
  only that paste using the captured plain text, preserving surrounding content
  and any existing frontmatter. Keep Markdown syntax and line breaks without
  source-format-induced escaping; rich mode renders the Markdown normally.
  Plain-text-only or HTML-only pastes retain existing behavior without an
  unavailable alternative. Do not read the clipboard again when clicked.
- **Key examples:**
  - The clipboard contains plain text `**important**` and an HTML representation
    that displays those literal asterisks. Paste normally into a selected span
    in Markdown mode, then choose **Use as Markdown**: replace that insertion
    with `**important**`, without added backslashes, duplication, or changes to
    the surrounding text.
  - Paste the same clipboard content in rich mode and choose the action:
    the inserted word renders bold; switching to Markdown mode shows
    `**important**`. Existing properties and surrounding body remain intact.
  - Copy text with multiple lines from a formatted source: paste without
    formatting preserves the text and line breaks while discarding source
    styles. Dismissing or ignoring the action leaves ordinary paste intact.
  - Continue typing or paste again before choosing: the old action cannot
    overwrite subsequent edits; a new paste can offer its own action.
- **Evaluation:** The examples work in both modes using the explicit option,
  including when the clipboard provides both HTML and plain-text content.
- **UI:** One compact, non-modal action and an accessible dismiss control,
  reachable by touch or keyboard, without covering the insertion or stealing
  editor focus. Choose once, then close; this is not a format toggle.
  Dismiss on typing, outside interaction, Escape, mode/note change, or editor
  teardown. Default timeout: 10 seconds of inactivity, paused while hovered
  or keyboard-focused. This duration is a reviewable design default.
  Preserve the editing position after pointer activation. Keep the control
  within the visible viewport, including when a mobile keyboard reduces it.
- **Existing paste prompt:** Preserve link/image removal. When its modal is
  open, suspend the floating choice and its timer; after cancellation, offer
  the choice. Choosing removal consumes that paste choice. Do not reopen the
  removal prompt when applying **Use as Markdown**.
- **Design acceptance:** Simple frontend design, minimal handwritten code,
  direct names for paste and its temporary choice, one owner for shared rules,
  and no duplicated conversion, dismissal, or replacement policy per mode.
  Reuse existing editor/conversion owners and keep necessary selection mechanics
  local to each editor. Review the aggregate diff and line counts for unnecessary
  code; do not compress code or create abstractions merely to lower a count.
- **Verification:** Rely on high-level frontend unit tests with real editor
  components and clipboard events. Plan zero new E2E scenarios; the owner's
  absolute ceiling is one if later evidence establishes a missing boundary.
- **Deferred promises:** Preview, Markdown detection, remembered preferences,
  extra shortcuts or menus, arbitrary later reformatting, global paste UI
  redesign, other input fields, and new persistence behavior are not selected.
- **Value / learning:** Remove manual cleanup when copying Markdown from
  sources that advertise it as formatted content.
- **Effort hypothesis:** S–M (roughly 1–2 hours), medium confidence after
  inspection; rich selection replacement and mobile focus need direct proof.
- **Depends on:** No known product prerequisite.
- **Safe stopping point:** Both editor modes support the explicit option while
  ordinary formatted paste remains usable, independently of other editor work.
- **Open product decisions:** None blocking planning. The timeout and compact
  placement are reviewable defaults within the selected interaction.

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
- Owner follow-up: refine the floating post-paste direction and write a slice
  plan, with simple domain-cohesive frontend design and predominantly frontend
  unit proof (at most one new E2E scenario). Commit and push planning changes
  for review; implementation is not authorized by this request.
