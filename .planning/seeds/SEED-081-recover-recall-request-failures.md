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
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner whose recall question cannot load can recover the
  current question without mistaking the failure for an intended self-review
  exercise or submitting an answer to an empty spelling question.
- **Scope:** Distinguish question loading, failure and an available question on
  the current recall card. Offer a deliberate retry that keeps the current
  tracker and session position. For multiple-choice failures, preserve the
  existing self-review capability as an explicit learner choice. A spelling
  failure offers retry without exposing a blank question for answering; this
  story does not introduce self-grading for spelling. Keep ordinary navigation
  available and retain existing access/error handling. A failed background
  prefetch must not change a different visible card. Loading or retrying alone
  records no answer, Grade or schedule transition.
- **Key examples / evaluation:**
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
- **Evidence / response:** [DD-318](../../DonutRetrospectiveFindings.md#dd-318--a-failed-question-load-falls-through-to-an-unintended-recall-surface).
  This story is the queued response, not a resolution of the finding.
- **Value / learning:** Keep service availability failures distinct from
  deliberate learning actions and restore a useful question through the same
  recall surface. Production frequency and recovery-time savings are unknown.
- **Effort hypothesis:** M, medium confidence; refine the shared loading state
  and existing self-review entry before execution.
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
owned by Open Dough; these two findings concern Donut's user-facing product.
