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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/017-tidy-note-context/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"38a858c62f6c5ed9965d3bb4151a90175dae1a2cba7f29b6690372a2745375bf","plan":"c52a96ed504ce58b5c4f0ebe20caea29cb9b1a93902355dd01ffa6d64c65da7d"}}
```

**Goal:** A learner reading note context after a spelling answer, or in a
note conversation on a narrow screen, sees each piece of note information once
and can read it and close it without one covering the other. As a side effect,
spelling results stop drawing the note's title and location with two separate
components.

**Scope:**

- In a spelling result, whenever the note context is shown (automatically
  after a wrong answer, or on request after a correct one), it takes the place
  of the compact title-and-location line instead of appearing below it: the
  note title and location appear once.
- The note context stays inside the labelled **Note under question** box, so
  an accidental-match result still tells the reviewed note apart from the note
  the answer named. The focused property of a property tracker is marked inside
  the note context, as Just review already does. The memory tracker link stays.
- A correct result with the note context not yet shown keeps its compact
  summary unchanged.
- In a narrow-screen note conversation, the note context drawer's close button
  stays visible and usable, and the note's location stays readable, however
  long the notebook and folder path is.
- Unchanged: Just review, the memory tracker page, multiple-choice answer
  results, and the wide conversation layout. Not in scope: redesigning the
  shared dialog or drawer component beyond what the close-button fix needs.

**Key examples:**

- A learner answers a spelling prompt wrongly → the **Note under question** box
  shows the note title and location once, with its properties, body, and
  references below; **Open full note** still leads to the note page.
- A learner answers correctly, then clicks **Show note context** → the compact
  title line is replaced by the full note context; the title still appears
  once.
- A learner gets an accidental match on a property tracker → the result names
  the other note in its alert, and the **Note under question** box shows the
  reviewed note with its focused property marked.
- A learner opens the note context of a conversation on a 390-pixel-wide phone
  for a note in "Spanish vocabulary › Grammar › Irregular verbs" → the close
  button is visible and closes the drawer, and the whole location can be read.
  (Checked 2026-09-27: today that path runs past the drawer edge, is cut off
  at "Irregu", and hides the close button; a one-folder path just fits.)

**Effort hypothesis:** S.

## When to Surface

Select this story from the product backlog when polishing recall results or
Message Center presentation.

## Breadcrumbs

- Owner decisions, 2026-09-27 refinement: keep the backlog position; also
  stop the duplication for a correct answer's revealed context; keep the
  **Note under question** box around the note context; keep the drawer part
  only because a phone-width check confirmed it.
- Owner request, 2026-09-27: queue the two note-context glitches noticed after
  the read-only note context replaced the embedded note page, at the bottom of
  the backlog. Earlier history of this seed: `9c8aca9bbd:.planning/seeds/SEED-033-simplify-note-presentation.md`.
