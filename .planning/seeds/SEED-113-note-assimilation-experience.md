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
{"schemaVersion":1,"refinement":"refined","approach":"unselected"}
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
  Offer **Next note** as the primary action. Leave any available optional modes
  accessible so the learner can add them before deliberately moving on. The
  sequence waits at the result until the learner chooses to advance.
- **Skip from sequence…** opens a confirmation that explains the note will stay
  out of the sequence until restored and that optional modes are unaffected.
  Confirming a skip advances the sequence. Revisiting a skipped note still
  allows direct Understanding assimilation or **Return to sequence**.

The story covers the note-level panel. Property-row assimilation is a separate
concern. Preserve the existing sequence order, including property entries. Settle
the advance button label for property entries and the result when there is no
next item during slice planning.

**Key examples.**

- A learner opens **かた と ほう** on a wide screen. The note title, default
  Understanding action, and two optional modes are all visible. At iPad portrait
  width, the note comes first and the same optional modes are one tap away from
  the bottom bar.
- The learner selects **Assimilate understanding**. The result names what was
  added and the note title remains visible. The learner can verify Spelling or add
  Commissioned on that same note. Only **Next note** advances the sequence.
- The learner confirms **Skip from sequence…**. The sequence advances with the
  note excluded from future offers. Reopening the note allows **Return to
  sequence**, which makes it eligible again.

**Design reference.** The [interactive mockups](../notes/assimilation-options.html)
preserve all three explored layouts. **Visible options** and **Read first** supply
the responsive layouts. The result and advance behavior above supersedes the
mockups’ simulated next screen. Remove this temporary reference and the mockups
when the story is wrapped up.
