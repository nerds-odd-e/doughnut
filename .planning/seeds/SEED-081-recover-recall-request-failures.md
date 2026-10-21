---
id: SEED-081
status: dormant
planted: 2026-10-21
planted_during: owner-requested reassessment of project-owned product improvements
trigger_when: making everyday recall recoverable after a question or answer request fails
scope: medium
---

# SEED-081: Recover a recall session when a request fails

## Why This Matters

Learners should be able to finish a recall attempt after a transient request
failure without losing their input, guessing whether an answer was recorded, or
changing their learning history twice. This supports the current direction's
learning and refinement loop and preservation of learning history.

The 2026-10-21 reassessment read current source and existing tests at
`b46b36c9c1abdd6e31802b7ec2dac14330b583cb`. It did not run a browser,
inject a network failure, or measure production frequency:

- Multiple-choice submission allows a retry after an API error, while the
  backend refuses any second answer to an already answered prompt. If acceptance
  succeeds and its response is lost, that retry cannot recover the recorded
  result through the current answer operation.
- Spelling submission disables Answer before emitting its request. Its parent
  reacts only to success, so a failed request leaves the control disabled.
- A failed multiple-choice question fetch becomes the existing self-review
  surface; it has no direct question retry control. A failed spelling question
  fetch leaves an empty stem with the answer form available.

These are source-backed failure paths, not recorded live learner incidents.
The [project findings](../../DonutRetrospectiveFindings.md#dd-317--a-failed-or-lost-answer-response-has-no-complete-recovery-journey)
retain their decisive code and test locators. Establish failure-boundary proof
when refining the selected story; preserve uncertainty about incidence and cost.

## Story Decomposition

These are queued product stories, not executable plans. Both remain unrefined;
implementation choices and precise proof belong to later refinement.

<a id="story-1"></a>

### Recover a recall answer after a failed or lost submission response

**Identity:** SEED-081#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

- **For / why:** A learner submitting a multiple-choice or spelling answer can
  recover the attempt and continue recall after its request fails, including
  when the server recorded the answer but the response never reached the page.
- **Scope:** Keep the current prompt, entered answer and session position
  available through recovery. Make a failed submission understandable. Recover
  the original recorded result when it exists; permit a deliberate retry when
  no answer was recorded. The attempt retains one original answer and one set
  of learning effects, with its original timing evidence. Repeated recovery
  does not create another Grade, recall log or schedule transition. Preserve
  existing ownership and ordinary answer/grading rules. Do not automatically
  resubmit an uncertain outcome or treat a retry as permission to revise an
  already recorded answer.
- **Key examples / evaluation:**
  1. A spelling submission fails before acceptance: the learner sees a failure,
     keeps the typed answer, deliberately retries and reaches the answered
     result without reloading or leaving the attempt.
  2. A multiple-choice answer is committed but its response is lost: recovery
     shows that original selected choice and mark, advances the current session
     once, and leaves exactly the originally recorded learning effects.
  3. The same accepted-response-loss case for a spelling answer recovers its
     original outcome and any existing overlap or accidental-match continuation.
  4. Repeating recovery, or attempting a different answer after acceptance,
     keeps the first recorded result and its schedule rather than submitting
     a second learning event. A genuinely unrecorded failed attempt remains
     available for a deliberate retry.
- **Evidence / response:** [DD-317](../../DonutRetrospectiveFindings.md#dd-317--a-failed-or-lost-answer-response-has-no-complete-recovery-journey).
  This story is the queued response, not a resolution of the finding.
- **Value / learning:** Protect everyday recall continuity and learning history
  at a known UI/API failure seam. No failure rate or quantitative saving is
  claimed.
- **Effort hypothesis:** M–L, medium-low confidence until the recovery read and
  request-outcome cases are refined. Keep the outcome to one existing attempt;
  resplit if it cannot fit a few hours including delivery.
- **Depends on:** No new prerequisite story. Reuse existing attempt, answer,
  recall-history and scheduling responsibilities; a new generic retry service
  or offline queue is not a prerequisite.
- **Safe stopping point:** One failed answer can be recovered safely through the
  existing recall journey even if question-loading improvements remain deferred.

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
- **Depends on:** No hard block on story 1 or the question-context quality story.
  Loading a prompt and recovering a submitted answer are separate user outcomes.
- **Safe stopping point:** A question-load failure has a useful recovery choice
  even when answer-response recovery remains deferred.

## Ordering and Boundaries

The product backlog owns global priority: recover a possibly accepted answer
first, then recover a question load. Existing queued work and Taken membership
retain their relative order. No direction text, grading policy, formal contest
policy, private-help scope, question-generation quality mechanism or unrelated
service infrastructure is changed by this reassessment.

The existing [formal contest candidate](SEED-073-learning-refinement-and-retrieval.md#story-6)
remains unqueued with its owner decisions intact. Shared process findings remain
owned by Open Dough; these two findings concern Donut's user-facing product.
