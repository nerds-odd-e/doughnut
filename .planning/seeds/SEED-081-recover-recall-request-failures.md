---
id: SEED-081
status: dormant
planted: 2026-10-21
planted_during: owner-requested reassessment of project-owned product improvements
trigger_when: making everyday recall recoverable after a question request fails
scope: medium
---

# SEED-081: Recover a recall session when a request fails

## Why This Matters

Learners should be able to recover a recall question after a transient request
failure, with a deliberate choice about how to continue and learning history
preserved. This supports the current direction's learning and refinement loop.

The 2026-10-21 reassessment read current source and existing tests at
`b46b36c9c1abdd6e31802b7ec2dac14330b583cb`. It did not run a browser,
inject a network failure, or measure production frequency:

- A failed multiple-choice question fetch becomes the existing self-review
  surface; it has no direct question retry control. A failed spelling question
  fetch leaves an empty stem with the answer form available.

These are source-backed failure paths, not recorded live learner incidents.
The [project findings](../../DonutRetrospectiveFindings.md#dd-318--a-failed-question-load-falls-through-to-an-unintended-recall-surface)
retain their decisive code and test locators. Establish failure-boundary proof
when refining the selected story; preserve uncertainty about incidence and cost.

## Story Decomposition

These are queued product stories. Each story carries its own preparation facts;
executable work belongs to its associated slice plan.

<a id="story-2"></a>

### Retry a failed recall-question load or choose self-review explicitly

**Identity:** SEED-081#story-2
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/002-recover-recall-question/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"0a4d1c5e39299bd617b500beddc0c7135502602209e2832d67093f9a513e9ba8","plan":"0f64934989f5e165ed65ec35447657adc1bc9eccab141e0ad28dda224ba5ffb6"}}
```

- **Goal:** A learner using web recall can recover a question-load failure on
  the current card, or deliberately choose existing MCQ self-review, preserving
  session continuity and keeping service failures distinct from learning actions.
- **Scope:** Distinguish loading, failed loading and an available question for
  the current tracker. Offer deliberate Retry on the same tracker and session
  position, including after a repeated failure. During a request, show loading
  and prevent duplicate recovery actions; ordinary navigation stays available.
  For MCQ failures, offer Self-review explicitly. Selecting it reveals the
  existing note context and Good/Again controls; only a subsequent grade changes
  learning history. For spelling, make the stem and answer form usable only
  with a loaded prompt. Retain existing access/error handling.
  A failed future prefetch stays with its tracker: the visible attempt and its
  input remain intact, and reaching the failed tracker offers its own recovery.
  Late completion belongs to the request's tracker and attempt, not whichever
  card is visible when it returns. Loading, retrying and choosing self-review
  alone record no answer, Grade or schedule transition. Preserve the existing
  spelling overlap continuation, which loads a fresh attempt on the same tracker.
- **Deferred promises:** Answer-submission recovery is outside this story. It
  adds no spelling self-grading, automatic retry/backoff, offline queue,
  cross-reload recovery, question-quality changes, generic transport framework,
  or CLI/MCP recovery interface. These are delivery boundaries, not new rejection
  rules. The web change keeps the existing backend API and grading policy.
- **Key examples:**
  1. Current multiple-choice question loading returns the server's generation
     failure: the card explains the failure and offers Retry and an explicit
     self-review choice. Retry succeeds and displays a question for the same
     tracker with recall progress unchanged and no learning event recorded.
  2. The learner chooses self-review after that failure: the existing review
     controls appear, and only their subsequent grade action changes learning
     history.
  3. A spelling question fetch fails: the learner sees the failure and retries;
     the loaded stem and answer entry become usable after success. No answer
     can be submitted for a missing prompt.
  4. A prefetched future question fails while the learner answers another:
     the current attempt remains intact; reaching the failed question offers
     its own recovery, and a second failed retry remains recoverable.
  5. A question retry is pending when the learner moves another tracker to the
     current position. Its eventual success or failure belongs to the original
     tracker; the new visible question and progress remain intact.
  6. After a spelling OVERLAP, the next prompt load fails on the same tracker.
     Retry loads the new attempt, retaining the overlap explanation and ordinary
     focus behavior; the learner can submit a more specific answer.
- **UI:** Inline failure feedback such as “We couldn't load this question.”
  Show Retry, and Self-review for MCQ. Use the existing inline loading indicator
  while fetching. Choosing Self-review opens the existing note and grade surface
  in place. The loading/failure state occupies the spelling question area until
  a real prompt can be presented.
- **Evidence / response:** [DD-318](../../DonutRetrospectiveFindings.md#dd-318--a-failed-question-load-falls-through-to-an-unintended-recall-surface).
  This story is the queued response, not a resolution of the finding.
  Disposable observations on 2026-10-21 at
  `c5e0b9ff1b315317d6ea53cf620a2a0f93ea599e` reproduced MCQ fallback and the
  empty spelling answer surface, and showed that naive fetch consolidation
  breaks overlap continuation. The associated plan retains commands, results,
  limits and the resulting design decision; all scratch edits were reverted.
- **Value / learning:** Keep service availability failures distinct from
  deliberate learning actions and restore a useful question through the same
  recall surface. Production frequency and recovery-time savings are unknown.
- **Effort hypothesis:** Roughly 30–60 minutes including delivery; medium
  confidence. Existing loading and grading owners support three bounded changes;
  spelling overlap invalidation is the main integration concern, observed and
  assigned to the spelling recovery slice.
- **Depends on:** No hard block on the question-context quality story.
  Loading a prompt and recovering a submitted answer are separate user outcomes.
- **Safe stopping point:** A question-load failure has a useful recovery choice
  independently of answer-submission recovery.

## Ordering and Boundaries

The product backlog owns global priority. Existing queued work and Taken
membership retain their relative order. No direction text, grading policy,
formal contest policy, private-help scope, question-generation quality mechanism or unrelated
service infrastructure is changed by this reassessment.

The existing [formal contest candidate](SEED-073-learning-refinement-and-retrieval.md#story-6)
remains unqueued with its owner decisions intact. Shared process findings remain
owned by Open Dough; this finding concerns Donut's user-facing product.
