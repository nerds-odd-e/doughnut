---
id: SEED-083
status: dormant
planted: 2026-10-23
planted_during: owner-requested first-priority backlog capture on main
trigger_when: a note contains content the rich editor cannot keep
scope: small
---

# SEED-083: Make incompatible note content editable in Donut

## Why This Matters

A note author encountering "This note has content the rich editor cannot keep."
needs a direct way to make the note editable in Donut. The owner requested a
conversion button and a reasonable warning layout as the first backlog story.

## Story Decomposition

<a id="story-1"></a>

### Make incompatible note content editable with one conversion button

**Identity:** SEED-083#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal:** A note author can explicitly convert incompatible content into a
Donut-compatible form and continue editing it in the rich editor.

**Scope:**

- Keep the warning "This note has content the rich editor cannot keep."
- Delete the accompanying instruction "Switch to Markdown mode to edit it."
  and its exclusively dependent code and fixtures. At implementation acceptance,
  sweep product references to that instruction as a separate reading; prove
  the replacement conversion behavior through its observable results.
- Offer one clearly labeled button in the warning to make the current note
  content Donut compatible. Activating it converts unsupported content into a
  form the rich editor can keep, retaining readable note content and supported
  formatting so the author can continue editing and save the result.
- Make the warning and button fit the existing UI: clear visual hierarchy,
  comfortable spacing, a readable action label, and wrapping that works on
  narrow screens without clipping or overlap.

**Key examples:**

- An editable note contains content the rich editor cannot keep → the author
  sees the warning and its conversion button → activating the button produces
  compatible content that can be edited in the rich editor.
- The author edits and saves the converted content → reopens the note → the
  saved content remains usable in the rich editor.
- The same warning is displayed on a narrow screen → the message and button
  remain readable and the button is easy to reach.

- **Value / learning:** Restore an immediate path from captured content to
  everyday note editing and refinement.
- **Effort hypothesis:** S–M (30 minutes to 2 hours), low confidence until an
  affected note and its unsupported constructs are inspected during refinement.
- **Depends on:** No blocking story identified.
- **Safe stopping point:** One explicit conversion action makes affected notes
  editable; the result is useful independently of other editor improvements.

## Refinement Input

Use a representative affected note to settle the supported conversion results
and any unavoidable formatting tradeoffs before implementation planning.

## When to Surface

First in the product backlog, as explicitly requested by the owner.

## Breadcrumbs

- Owner instruction, 2026-10-23: replace the Markdown-mode instruction beside
  the rich-editor compatibility warning with one conversion button, ensure a
  reasonable UI, and add the story at the top on main, synced with origin.
