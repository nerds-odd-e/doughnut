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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/006-spelling-result-note-content/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"1f4e6b3624e63a2af384747f19768aac27e183a65c2a9a26657feb70187ceb99","plan":"f307992e6e054c9281b97d8ee2ea38ad6b7dd909c525edb5596568127270da94"}}
```

**Goal**

Learners reviewing a spelling answer see the read-only note content right away,
whether the answer was correct or wrong. Today a wrong answer shows it and a
correct one hides it behind a **Show note context** button. One consistent
result page removes that extra click and a special case from the recall
answer review.

- **Effort hypothesis:** S (roughly 30 minutes), high confidence; the
  existing wrong-answer display is reused as is.
- **Depends on:** none.
- **Safe stopping point:** the change is complete and useful on its own.
- **Plan:** [006-spelling-result-note-content](../slice-plans/006-spelling-result-note-content/PLAN.md)

**Scope**

- Required: the spelling result page shows the read-only note content inside
  the note-under-question area for every outcome: correct, wrong, and an
  accidental match with another note.
- Required: the **Show note context** button on a correct spelling result is
  removed, together with the reveal-on-demand state behind it. This reverses
  the earlier "keep a correct spelling result brief" choice on the owner's
  request.
- Boundary: the recall flow is unchanged (the owner proceeded to planning
  with this default). A correct answer still moves on to the next question
  without stopping. Its result page is seen when the learner goes back with
  **view last answered question**, and that page now shows the note content.
  Only a wrong answer stops on its result page, as today.
- Deferred: the recall history view ("This is a spelling question. Details
  are not needed.") and non-spelling answered-question pages are not changed.

**Key examples**

- A learner answers "Sedition" correctly, and recall moves on to the next
  question. They go back with **view last answered question** → the result
  shows "Correct!" and the note content containing "Sedition means incite
  violence", with no **Show note context** button.
- A learner answers "sedation" (wrong) → recall stops on the result, which
  says the answer is incorrect and shows the same note content, as today.
- An answer names another note (accidental match) → the result keeps its
  accidental-match message and resolve action and also shows the note
  content, as today.

## Ordering and Scope Reduction

One small story aligned with the backlog's general UI/UX direction. Queue ahead
of the non-urgent semantic-search decommissioning story; preserve Taken work.

## Breadcrumbs

- Owner request, 2026-09-28: show read-only note content on the
  answer-the-question page for both correct and incorrect spelling answers;
  capture just this one story.
