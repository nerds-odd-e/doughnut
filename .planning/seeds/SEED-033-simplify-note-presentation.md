---
id: SEED-033
status: dormant
planted: 2026-09-18
planted_during: investigation of embedded NoteShow and owner backlog request
trigger_when: selected from the product backlog
scope: small
---

# SEED-033: Simplify note presentation outside the note page

## Why This Matters

Spelling results, note conversations, and Just review show a read-only
**note context** instead of embedding the note page. Two rough edges remain:
after a wrong spelling answer the note title shows twice, and on narrow
screens the note context drawer's close button may cover the note's location.

## Story Decomposition

<a id="story-3"></a>

### 3. Tidy the note context in spelling results and narrow conversations

**Identity:** SEED-033#story-3
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
```

**Goal:** A learner reading note context after a wrong spelling answer, or in
a note conversation on a narrow screen, sees each piece of note information
once and can read it without controls covering it.

**Scope:** After a wrong spelling answer, the note title and location appear
once, not both in the compact note summary and at the top of the note
context; the focused-property indicator and tracker link stay available. In a
narrow-screen note conversation, the note context drawer's close button does
not cover the note's location or title. Correct-answer results, the wide
conversation layout, and Just review are unchanged. First check both in the
running app; drop any part that turns out not to happen.

**Key examples:**

- A learner answers a spelling prompt wrongly → the result shows the note
  title once, with its properties, body, and references below.
- A learner opens a note conversation on a phone-width screen and opens the
  note context → the close button sits clear of the location line and title.

**Effort hypothesis:** S.

## When to Surface

Select this story from the product backlog when polishing recall results or
Message Center presentation.

## Breadcrumbs

- Owner request, 2026-09-27: queue the two note-context glitches noticed after
  the read-only note context replaced the embedded note page, at the bottom of
  the backlog. Earlier history of this seed: `9c8aca9bbd:.planning/seeds/SEED-033-simplify-note-presentation.md`.
