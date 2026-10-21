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

These are queued product stories. Each story carries its own preparation facts;
executable work belongs to its associated slice plan.

<a id="story-1"></a>

### Recover a recall answer after a failed or lost submission response

**Identity:** SEED-081#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/001-recover-recall-answer/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"434d63e42a1df69d8d8f39d5320d478698d3bbf67e4259596bba13acf7d27453","plan":"82e02f232c227a4f7003acb508e9f3eb8f2fbdabcbc6c5b458a93d7ee1f019ba"}}
```

- **Goal:** A learner can finish the current multiple-choice or spelling recall
  attempt after a failed submission, including a lost response after acceptance,
  without losing the answer or applying learning effects twice.
- **Scope:** Retain the current prompt, selected choice or typed answer, original
  submission timing evidence, and session position until a result is known.
  Explain that submission could not be confirmed and offer a deliberate recovery
  action. That action returns the first accepted answer if present, or submits
  the retained answer if the earlier request never recorded one. A further failure
  leaves the same recovery action available. Recovery is available within the
  current mounted recall session; reloading the page or restarting the app is
  not required to recover.
  The original answer, mark/outcome, creation time and timing metrics are
  authoritative. Repeating recovery, submitting different answer content after
  acceptance, or overlapping a slow original request with a deliberate retry
  preserves one accepted answer and its original Grade, Confusion, recall-log
  and scheduling effects. Existing ownership checks and ordinary grading rules
  continue to apply. An uncertain request is never automatically resubmitted;
  recovery is not permission to revise an accepted answer.
  Feed a recovered result through the existing answer continuation exactly once:
  ordinary results advance once and remain available in answered history;
  OVERLAP keeps the tracker current and offers its existing more-specific-answer
  continuation; ACCIDENTAL_MATCH retains its reveal and resolution continuation.
  Note context and available resolution actions remain subject to current note
  visibility and authorization, as on the ordinary reveal.
- **Deferred promises:** Question-load recovery belongs to story 2. This story
  adds no offline queue, automatic retry policy, cross-reload draft persistence,
  generic transport framework, historical notebook-content snapshot, grading
  policy change, or recovery for unrelated writes such as exclusion/contest.
- **Key examples:**
  1. A spelling answer `sedition` fails before acceptance. The same stem and
     input remain, feedback explains the uncertain submission, and deliberate
     recovery submits it successfully without reloading. Time spent waiting or
     recovering does not replace the captured answer-timing evidence.
  2. A multiple-choice answer is committed but its response is lost. Deliberate
     recovery shows the first selected choice and mark, advances the session once,
     and leaves exactly the original learning effects and timing. Sending a
     different choice after acceptance still recovers the first result.
  3. A spelling answer is committed but its response is lost. Recovery restores
     the original outcome. An accidental match still offers its existing reveal
     and resolution actions for readable matching notes; an overlap still asks
     for a more specific answer on the same tracker without a Grade or schedule
     transition for the overlap. This new overlap attempt is distinct from
     retrying transport for the accepted overlap answer.
  4. Recovery fails again, or the initial request completes while recovery is
     pending. The learner keeps the current attempt until one result is known;
     repeated recovery produces one accepted answer and one normal continuation.
  5. Another learner attempts recovery for this prompt: the existing ownership
     rule refuses access and mutation.
- **UI:** Keep the unanswered card and its retained input visible during failure.
  Show inline feedback such as “We couldn't confirm your answer. Retry to recover
  this attempt.” Provide one deliberate Retry action using the retained
  submission; disable duplicate actions while a request is pending. Do not
  describe a generic request failure as a deleted or skipped tracker. Ordinary
  navigation remains available; a late response must not advance a different
  visible attempt.
- **Evidence / response:** [DD-317](../../DonutRetrospectiveFindings.md#dd-317--a-failed-or-lost-answer-response-has-no-complete-recovery-journey).
  This remains the queued response, not a resolution of the finding. Temporary
  failure-boundary observations on 2026-10-21 at `c242d19afddb88b0f2a0246cc1c69290de7f23eb`
  reproduced the disabled spelling control, rejected accepted MCQ resubmission,
  and incomplete reloaded accidental-match projection. Commands, setup, results
  and limitations are retained in the associated plan; scratch tests were removed.
- **Value / learning:** Protect recall continuity and learning history at the
  observed UI/API failure seam. Production incidence and quantitative savings
  remain unknown.
- **Effort hypothesis:** A few hours including delivery; medium confidence.
  The early database probe bounds the remaining same-prompt serialization risk.
- **Depends on:** No prerequisite story. Reuse prompt, answer, history,
  scheduling and note-matching owners.
- **Safe stopping point:** Answer recovery works independently of question-load
  improvements.

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
