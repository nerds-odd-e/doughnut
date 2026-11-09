---
id: SEED-113
status: active
planted: 2026-11-09
scope: story
---

# Choose how to assimilate a note and when to move on

<a id="assimilate-notes-with-a-clear-default-and-result"></a>

### Choose how to assimilate a note and when to move on

**Identity:** SEED-113#assimilate-notes-with-a-clear-default-and-result
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-note-assimilation-experience/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"4654c205d1f81848c58c1b5e11c2499f3f2637c364a4f25e24813b020ccee981","plan":"673a8a0cfd07150658b89d642d32132615359a4d81db7dc36cfdf157f84173b6"}}
```

**Goal.** A learner can see which note they are assimilating, distinguish the
default Understanding action from optional modes, and decide when to advance
after seeing what was added to recall.

**Why this matters.** The current panel sits above the note title and gives
Commissioned, Spelling, and Understanding the same filled **Assimilate** button.
Understanding is listed last, and **Skip** does not explain what it affects.
The design discussion used the relationship note **かた と ほう** as the example.

**Scope.** Combine the two selected mockups into one responsive note-level
assimilation experience.

- At wider widths, use **Visible options**. Put the note title first and show
  default **Understanding** alongside **Optional modes**. Use one primary
  **Assimilate understanding** action, with **Skip from sequence…** as a
  secondary action. Show both optional modes with a short description and a
  distinct action.
- Around iPad portrait width and below, use **Read first**. Show the note title,
  properties, and content before the assimilation actions. Keep **Assimilate
  understanding**, **Skip from sequence…**, and **Optional modes** accessible in
  a bottom action bar. Open Spelling and Commissioned in a popover from that bar.
  Choose the breakpoint by the space available to the panel and use iPad portrait
  as a layout check.
- Keep the spelling and tutor icons next to the **Spelling** and **Commissioned**
  labels and the checkmark for the successful result. **Verify spelling** asks
  for the title or an alias from memory with the note hidden during verification.
  **Add to sessions** adds the note for future learning sessions with a human or
  AI tutor. Each optional mode can be added independently and keeps the learner
  on the note. Already active modes show their tracker and next recall date.
- After Understanding succeeds, keep the current note and title visible in a
  temporary result state. Show a checkmark and **Understanding added to recall**.
  Offer **Next item** as the primary action. Leave any available optional modes
  accessible so the learner can add them before deliberately moving on. The
  sequence waits at the result until the learner chooses to advance. Adding an
  optional mode preserves the Understanding result on that same note.
- **Skip from sequence…** opens a confirmation explaining that only this note's
  Understanding entry stays out of the sequence until restored. Its property
  entries and optional modes are unaffected; a property on this same note may
  appear next. Confirming a skip advances the sequence. Revisiting a skipped note
  still allows direct Understanding assimilation or **Return to sequence**.

The story covers the note-level panel. Property-row assimilation is a separate
concern. Preserve the existing sequence order, including property entries, and
property-row advancement. **Next item** accommodates a property destination,
including one on the current note. When an explicit advance finds no next item,
keep the current note page, show the existing **No more notes to assimilate**
feedback, and close the assimilation presentation. A daily target being reached
does not end the sequence while another item exists.

**Scope limits.** This increment does not redesign property-row learning, launch
or change commissioned learning sessions, change scheduling or daily quotas, or
restore the temporary success result after a browser reload. These are delivery
boundaries, not new product refusals. Existing authorization and tracker rules
continue to apply; Spelling still requires successful verification before adding
its tracker.

**Key examples.**

- A learner opens **かた と ほう** on a wide screen. The note title, default
  Understanding action, and two optional modes are all visible. At iPad portrait
  width, the note comes first and the same optional modes are one tap away from
  the bottom bar.
- The learner selects **Assimilate understanding**. The result names what was
  added and the note title remains visible. The learner can verify Spelling or add
  Commissioned on that same note without losing the Understanding result. Only
  **Next item** advances the sequence after Understanding succeeds.
- A note has pending Understanding and a property entry → the learner confirms
  **Skip from sequence…** → only note-level Understanding is excluded and the
  property remains eligible. Reopening the note allows **Return to sequence**,
  which makes its Understanding entry eligible again. Direct Understanding
  assimilation also remains available; optional additions preserve the skip.
- Understanding succeeds and the next eligible unit is a property on the same
  note → the learner chooses **Next item** → that property opens in the existing
  sequence order. Assimilating a property retains its existing advance behavior.
- Understanding succeeds on the last eligible unit → the learner can still add
  optional modes → **Next item** reports **No more notes to assimilate**, closes
  the assimilation presentation, and keeps that note page visible.

**Challenge and decision.** A confirmation saying the entire note is excluded
could mislead a learner when a property on that same note appears next. On
2026-11-09 Terry accepted the recommended scope: preserve note-level skip and
explain that property learning and optional modes are unaffected. The wording
and examples above follow that decision. The selected responsive interaction
and deliberate advance still deliver the stated purpose; no further goal or
scope decision remains open.

**Investigation (2026-11-09, revision `96edea6b5919f11b874cb3a8780cd8c99534bc6d`).**

- [Tracker creation](../../backend/src/main/java/com/odde/donut/services/MemoryTrackerAssimilation.java)
  selects Understanding, Spelling, or Commissioned independently. Understanding
  clears its matching skip; optional modes preserve it.
- [Skip predicates](../../backend/src/main/java/com/odde/donut/entities/AssimilationSequenceSkip.java)
  distinguish the empty note-level key from each property key; the
  [property selection query](../../backend/src/main/java/com/odde/donut/entities/repositories/NotePropertyIndexRepository.java)
  uses only its matching property skip. This code observation informed the
  accepted scope decision; no backend test was executed in this investigation.
- [Assimilation completion](../../frontend/src/composables/useAssimilateUnit.ts)
  advances ordinary Understanding automatically, including property requests;
  optional modes stay on the note. The
  [next-unit consumer](../../frontend/src/composables/useGoToNextAssimilation.ts)
  handles both note and property destinations and the exhaustion feedback above.
- The literal command
  `CURSOR_DEV=true nix develop -c pnpm frontend:test tests/components/recall/AssimilationPanel.spec.ts tests/components/recall/AssimilationPanel.trackers.spec.ts tests/composables/useGoToNextAssimilation.spec.ts`
  ran here in headless Chromium: all 20 tests passed. A disposable edit that
  suppressed auto-advance only for note-level Understanding made the two panel
  tests asserting the next note fail; the other 18 passed. The edit was reverted
  and the product diff checked empty before this record was written. This
  establishes that those tests discriminate existing auto-advance, not that the
  future result state or property journey is proved. Future proof must observe
  the success result, optional additions, deliberate advance, and enclosing note
  page rather than merely updating their route assertions.
- The selected mockups are design references. No product usability observation
  or iPad layout run was performed; comprehension and fit remain hypotheses to
  evaluate against the implemented interaction.

**Architecture.** Keep tracker creation and mixed-unit selection in their
existing domain owners. The new result is temporary note-level presentation
state; it does not add a persisted learning state, schedule, or next-item cache.
The shared action path also serves property rows, so deliberate advance must
preserve that caller's existing behavior. The
[panel](../../frontend/src/components/recall/AssimilationPanel.vue)
emits a reload after optional additions, and
[NoteToolbar](../../frontend/src/components/notes/core/NoteToolbar.vue)
reloads the realm and changes the panel key. Result ownership must survive that
same-note refresh and reset when moving to a different note; storing it only in
the remounted panel would defeat the promised interaction.

Relevant Accepted decisions are
[ADR 0001 — Ubiquitous language](../../docs/adrs/0001-ubiquitous-language.md#assimilation)
(independent tracker types and sequence units),
[ADR 0003 — Spaced-repetition scheduling policy](../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md)
(assimilation creates a New tracker, immediately due),
[ADR 0005 — Web routes](../../docs/adrs/0005-web-routes-accepted.md#spa-locations)
(named note/property destinations), and
[ADR 0006 — Failure handling](../../docs/adrs/0006-failure-handling-accepted.md#usage)
(retain established action-error handling). The ADR index and record statuses
agree; no conflict or exception was identified. ADR 0001's filename omits the
conventional Accepted suffix, which does not change its recorded authority.

**Design reference.** The [interactive mockups](../notes/assimilation-options.html)
preserve all three explored layouts. **Visible options** and **Read first** supply
the responsive layouts. The result and advance behavior above supersedes the
mockups’ simulated next screen. Remove this temporary reference and the mockups
when the story is wrapped up.
