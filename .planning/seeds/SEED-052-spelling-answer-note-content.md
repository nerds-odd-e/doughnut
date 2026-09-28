---
id: SEED-052
status: dormant
planted: 2026-09-28
planted_during: owner request to capture a spelling-answer UI/UX improvement
trigger_when: prioritizing spelling-answer feedback and general UI/UX improvements
scope: small
---

# SEED-052: Consistent note content after spelling answers

## Why This Matters

Learners currently see read-only note content on the answer-the-question page
after an incorrect spelling answer, following a recent change. Correct spelling
answers do not show that content. The owner wants learners to have the same
opportunity to review the note after either outcome.

## Alternatives and Decision

Show the existing read-only note content after both correct and incorrect
spelling answers. Retaining the current incorrect-only display would keep the
reported inconsistency.

## Story Decomposition

<a id="story-1"></a>

### Show read-only note content after every spelling answer

**Identity:** SEED-052#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** learners answering spelling questions can review the note
  regardless of whether their answer was correct.
- **Evaluation:** after submitting either a correct or an incorrect spelling
  answer, the answer-the-question page displays the read-only note content.
- **Value / learning:** provide consistent access to note context during
  spelling-answer review.
- **Effort hypothesis:** S (roughly 30–60 minutes), medium confidence; assumes
  the existing incorrect-answer presentation can be reused.
- **Depends on:** none.
- **Safe stopping point:** consistent note review after both spelling-answer
  outcomes is useful as a standalone UI/UX improvement.

**Scope**

Extend the existing read-only note-content presentation to correct spelling
answers and retain it for incorrect spelling answers. This story captures only
that display change; no additional read-only-page improvement was requested.

**Key examples**

- A learner submits a correct spelling answer → the result page shows the
  read-only note content.
- A learner submits an incorrect spelling answer → the result page continues
  to show the read-only note content.

## Ordering and Scope Reduction

One small story aligned with the backlog's general UI/UX direction. Queue ahead
of the non-urgent semantic-search decommissioning story; preserve Taken work.

## Breadcrumbs

- Owner request, 2026-09-28: show read-only note content on the
  answer-the-question page for both correct and incorrect spelling answers;
  capture just this one story.
