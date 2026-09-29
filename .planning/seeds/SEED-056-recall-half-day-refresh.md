---
id: SEED-056
status: dormant
planted: 2026-09-29
planted_during: owner request to capture automatic recall refresh at the local half-day boundary
trigger_when: improving recall freshness across local half-day boundaries
scope: small
---

# SEED-056: Keep recall current across local half-day boundaries

## Why This Matters

A learner can keep Donut open while local time crosses a half-day boundary.
Recall questions are recounted for the new window, so the displayed recall
count and queue need to refresh automatically without a manual reload.

## Alternatives and Decision

The owner requested automatic refresh when the half-day changes. Requiring a
manual reload leaves recall stale. Refresh can happen in the background when
the learner is outside recall; during recall, a blocking spinner makes the
refresh visible and prevents interaction with stale recall state.

## Story Decomposition

One story covers keeping recall current and the loading experience when the
boundary is crossed. This seed captures backlog work, not an execution plan.

<a id="story-1"></a>

### Refresh recall automatically at each local half-day boundary

**Identity:** SEED-056#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"refined","approach":"unselected","assessment":"not-ready","reasons":["The question and answer already in progress at the boundary need a product decision.","An execution approach has not been selected; this request captures backlog work only."],"basis":{"document":"1b5d85576d00dae71c7a5c6c16b63b3d2af5277fc5c40b3fd19acd57128cdfbd"}}
```

**Goal**

Learners see the current recall question count and queue after a local half-day
boundary, without manually refreshing Donut.

**Scope**

- Automatically refresh recall when the user's local half-day window changes
  at noon or midnight, using the local-time boundary used by recall scheduling.
- Recount recall questions and refresh the recall queue for the new window.
- If the learner is outside recall, perform the refresh in the background
  without blocking the current activity.
- If the learner is doing recall, block the recall UI with a spinner while the
  refresh is in progress; restore interaction with the refreshed state afterward.
- Resolve what happens to a question already being answered before execution.
  The owner's tentative expectation is that it normally remains in the queue;
  this is an open decision, not a confirmed rule about every question.
- Changes to recall scheduling rules are deferred; this story refreshes the
  state produced by the existing rules.

**Key examples**

- A learner is browsing notes at 11:59 local time → time crosses noon → recall
  is recounted and refreshed in the background; browsing remains usable.
- A learner is doing recall just before local midnight → time crosses midnight
  → the recall UI shows a blocking spinner, then resumes with the refreshed
  count and queue.
- A learner is answering a question when the boundary is crossed → refresh
  occurs with the blocking spinner → the treatment of that question and any
  answer in progress follows the decision below once resolved.

- **For / why:** Learners can trust the recall count and queue after a new
  half-day begins.
- **Evaluation:** Leave Donut open across a local half-day boundary and observe
  automatic recall refresh, background behavior outside recall, and blocking
  loading feedback during recall.
- **Value / learning:** Keep recall current and determine how a boundary refresh
  should preserve the question already in progress.
- **Effort hypothesis:** M (1–2 hours), low confidence until the in-progress
  question behavior and existing refresh mechanism are understood.
- **Depends on:** No prerequisite among currently queued stories is established.
- **Safe stopping point:** Automatic refresh is useful independently of other
  recall improvements; resolve the in-progress question behavior before delivery.

## Ordering and Scope Reduction

Add to the end of the product backlog without changing existing priorities.
Keep this story focused on half-day refresh and its loading experience.

## Open Decisions

- When the learner is already answering a question, should that question remain
  displayed as well as remain in the refreshed queue? The owner expects it
  normally to stay queued but is unsure what happens today. Clarify how to
  preserve the answer in progress and handle an answer submitted as refresh starts.

## When to Surface

When selecting recall freshness and user experience improvements from the backlog.

## Breadcrumbs

- Owner request on 2026-09-29: crossing the local half-day boundary should
  automatically refresh recall because recall questions are recounted; refresh
  in the background outside recall, and block the UI with a spinner during
  recall. A question already being answered normally should still be queued,
  but its exact treatment is uncertain.
