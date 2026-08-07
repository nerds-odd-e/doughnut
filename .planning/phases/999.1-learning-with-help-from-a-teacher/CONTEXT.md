# Phase 999.1 — Learning with help from a teacher

Captured: 2026-08-07  
Status: BACKLOG (candidate for a future milestone)

## Idea

Learning stays organized by notes. Notes are assimilated into a new type of memory tracker (working name: “with a teacher” — name undecided).

When recall is due, Doughnut:

1. Groups due memory trackers by notebook
2. Builds an instruction for the “teacher” including:
   - Focus notes and the merged graph
   - User learning status
   - Expected learning content for today

That instruction can be sent to a human teacher, copied into ChatGPT, or (later) sent to an agentic teacher inside Doughnut. For now, training is mostly offline to Doughnut.

When training finishes, the teacher completes a feedback form (same instruction or a companion one) with a defined format: quantitative scores and descriptive feedback **per memory tracker**. Feedback returns to Doughnut; Doughnut updates the trackers. Unlike current memory trackers, this type also keeps a log of teacher feedback.

## Instruction generation (staged)

1. **Dumb generator** — list due memory trackers and recent log
2. **Smart generator** — AI-assisted instruction shaping (curriculum coordinator / director of studies role — name TBD)

## Protocol

Doughnut ↔ “teacher” exchange should follow an existing convention or standard if one fits (open research when promoted).

## Related / next

- Same idea could apply to **assimilation**
- Later step: an **agentic teacher** that helps the user assimilate a note

## Open naming

- Memory tracker type name
- Role name for the smart instruction manager (curriculum coordinator / director of studies / …)
