---
id: SEED-055
status: dormant
planted: 2026-09-29
planted_during: owner request to review and clean up the current backend public API
trigger_when: prioritizing removal of unused public APIs and reconsideration of redundant services
scope: medium
---

# SEED-055: Keep the public API aligned with supported product features

## Why This Matters

For Donut maintainers, the backend public API should expose capabilities that
serve real frontend or external features. Dead endpoints add maintenance work,
and endpoints that provide duplicated services deserve reconsideration.
The owner wants unused APIs found and removed, and actionable improvements
proposed for redundancies, while preserving supported feature behavior.

## Alternatives and Decision

The owner selected a review of the current API using the generated TypeScript
client or OpenAPI YAML as the starting inventory. Keeping an endpoint because
it has tests does not establish product use. An inventory-only report would
leave confirmed dead APIs in place, so this story includes their cleanup.
Redundant APIs require comparison of their actual services and consumers;
this story proposes improvements without presuming every overlap should merge.

## Story Decomposition

Effort bands: S = 30–60 minutes, M = 1–2 hours, L = 2–4 hours, including delivery.
This seed captures one cleanup outcome; it does not authorize an audit or
implementation now.

<a id="story-1"></a>

### Remove unused public APIs and propose improvements for redundant services

**Identity:** SEED-055#story-1
```json dough-story-state
{"schemaVersion":1,"refinement":"not-refined","approach":"unselected"}
```

**Goal**

Donut maintainers have a public API with confirmed dead endpoints removed and
an evidence-backed set of improvement proposals for endpoints that provide
duplicated services. Existing frontend and external feature journeys continue
to work.

**Scope**

- Inventory the current backend public API from the generated TypeScript API
  client or `open_api_docs.yaml`, checking against backend routes where needed
  to establish coverage and freshness.
- Trace actual feature use across the frontend and external consumers,
  including CLI and MCP features, direct HTTP calls, and supported external
  integrations. Generated declarations, backend implementation references,
  test calls, test fixtures, and mocks do not count as feature use.
- An API is unused when no frontend or external feature uses it. Tests alone
  never justify retaining it. Record the evidence and search coverage for each
  removal candidate; unresolved external usage is an uncertainty to resolve,
  rather than proof that an endpoint is dead.
- Remove confirmed unused endpoints and supporting code that becomes unused
  solely through their removal. Update affected documentation and tests, and
  regenerate the API artifacts through the repository's generation workflow.
- Compare endpoints that deliver the same or overlapping service. Explain
  consumer needs and meaningful differences in behavior, authorization,
  inputs, outputs, and side effects before recommending consolidation,
  simplification, or retention of a justified distinction.
- Deliver concrete proposals for redundancies, identifying affected APIs,
  intended benefit, consumer impact, and any migration or compatibility work.
  Implementing those proposals is deferred to a later product decision.
- Preserve supported feature behavior. Broad service redesign, unrelated dead
  code cleanup, and new API capabilities are outside this story.

**Key examples**

- An endpoint appears in the generated client and has backend tests, but no
  frontend or external feature uses it → confirm the usage evidence, remove
  it and its orphaned support, and regenerate the public API artifacts.
- An endpoint has no frontend caller but supports a CLI command or external
  integration → retain it as used, with that feature recorded as evidence.
- Two APIs provide substantially the same service → compare their consumers
  and behavior, then propose a specific improvement with its migration impact.
- No repository caller is found and external use remains unknown → record the
  unresolved evidence and resolve it before classifying the API for removal.

**Output and evaluation**

The owner can review API coverage, usage classifications and their evidence,
the APIs removed, any unresolved usage questions, and redundancy proposals.
The resulting public API artifacts no longer advertise confirmed dead
endpoints, and relevant automated checks demonstrate that affected supported
feature journeys still work. Finding no dead or redundant APIs is a valid
result when supported by the completed review, rather than assumed in advance.

- **For / why:** Maintainers reduce unnecessary public contracts and can judge
  how to simplify duplicated services without disrupting real consumers.
- **Value / learning:** Establish which exposed APIs actually serve features,
  and which apparent redundancies represent distinct product needs.
- **Effort hypothesis:** L, low confidence until the API inventory and consumer
  coverage are known. Reassess scope and story sizing during refinement if the
  work cannot fit a few hours; this estimate is not an exhaustive-audit budget.
- **Depends on:** Access to the current API definition and evidence of supported
  frontend and external consumers; no dependency on another queued story.
- **Safe stopping point:** Confirmed dead APIs are removed with supported
  behavior preserved; redundancy proposals remain useful independently of any
  later consolidation. Unresolved usage questions remain explicit.

## Ordering and Scope Reduction

The owner requested this story at the top of the product backlog, ahead of
SEED-054#story-1. Establish feature-use evidence before removal. Redundancy
implementation is deferred; the requested recommendations remain in scope.

## Open Decisions

- Which APIs are unused, and where does external usage need confirmation?
  Determine from the review, without preselecting endpoints for deletion.
- Which redundancy proposals should be implemented? Decide from the evidence
  and consumer impact after this story's recommendations are available.

## When to Surface

Next, as the highest-priority queued story requested by the owner.

## Breadcrumbs

- Owner request on 2026-09-29: check the current backend public API through the
  generated TypeScript code or YAML; unused means no frontend or external
  feature use, and tests do not justify keeping an API. Find and clean up dead
  APIs and propose improvements for duplicated services.
- `.agents/agent-map.md` identifies the generated API summary, TypeScript
  client, OpenAPI YAML, backend routes, frontend, CLI, and MCP entry points.
