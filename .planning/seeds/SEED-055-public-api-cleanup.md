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
{"schemaVersion":1,"refinement":"refined","approach":"planned","plan":"../slice-plans/012-public-api-cleanup/PLAN.md","assessment":"ready","reasons":[],"basis":{"document":"11ac7526dbf86e0d8a04314f733e2cae230570ee05b533ddd7a607d20a3bd40d","plan":"35244fb9fd161058946a269429558c9676478656f20256a86cc203077b7482bd"}}
```

**Goal**

Donut maintainers and their coding agents work from a public API that
advertises only endpoints serving real features. The generated API summary is
the agents' default endpoint lookup, so every dead or duplicated endpoint is a
wrong option an agent can pick and build on. This story removes the confirmed
dead endpoints with everything only they kept alive, and gives the owner one
evidence-backed improvement proposal for the overlapping file-serving endpoints
left behind by the attachment, Book, and LFS migration. Supported frontend and
external feature journeys keep working.

**Scope**

- An endpoint is used when a frontend, CLI, MCP, or supported external feature
  calls it, through the generated client or direct HTTP (for example Git and
  Git LFS transfer, page image and file URLs, event streams, install).
  Endpoints used by E2E tests, such as the testability controllers, are exempt
  from removal as test infrastructure. Other test calls, fixtures, and mocks do
  not count as use.
- Remove the endpoints with no such use. A usage review on 2026-09-29 found
  these five; the implementer confirms each before removal:
  - `DELETE /api/user/token-info` (revoke token)
  - `GET /api/user/recall-ez-diffusion`
  - `GET /api/user/daily-probe-convergent-validity`
  - `GET /api/memory-trackers/{memoryTracker}/recall-logs`
  - `GET /api/notebooks/{notebook}/book/file`, found during slice planning: the
    frontend reads Book bytes only through `GET /api/books/{book}/file`
- Removal is transitive: also remove every implementation piece whose only
  dependent was a removed endpoint (services, queries, DTOs, entities or
  columns, helpers, tests, fixtures), repeating until nothing orphaned
  remains. Then clean up across the whole product scope (backend, frontend,
  CLI, MCP, documentation, agent guidance) so the result reads as though the
  removed things never existed. Regenerate the API artifacts through the
  repository's generation workflow.
- Removal leaves no trace: no negative tests, absence checks, or historical
  notes replace what was removed.
- Compare the remaining file-serving endpoints — `GET /api/books/{book}/file`,
  `GET /api/notebooks/{notebook}/attachments/{attachment}/image`,
  `GET /api/notebooks/{notebook}/attachments/{attachment}/content`, and
  `GET /api/notes/{note}/image` — by consumer, authorization, inputs, outputs,
  and side effects, then deliver one concrete proposal naming affected
  endpoints, intended benefit, consumer impact, and migration work.
  Implementing it is a later product decision.
- Deferred: redundancy review of the rest of the API, implementing any
  consolidation, the code-generation workaround endpoint
  (`AiController.dummyEntryToGenerateDataTypesThatAreRequiredInEventStream`),
  unused DTO fields or parameters, dead code unrelated to removed endpoints,
  and new API capabilities.

**Key examples**

- The recall logs endpoint has backend tests but no frontend or external
  caller → remove it, the implementation only it used, and its tests;
  regenerate the API artifacts; nothing asserts it is gone.
- A testability endpoint is called only by E2E setup → keep it.
- `POST /api/notebooks/{notebook}/attach-book` has no generated-client caller
  but the CLI calls it over HTTP → keep it as used by the CLI.
- A service method was used only by a removed diagnostic endpoint → remove it
  too, and any query or DTO only that method used.
- The Book file and attachment content endpoints return the same bytes, but
  only the Book endpoint sets caching headers → the proposal explains who calls each and what differs, then
  recommends consolidating or keeping the distinction, with its migration impact.

**Output and evaluation**

The owner can review the confirmed usage evidence for each removed endpoint,
the removal diff, and the file-serving proposal. The public API artifacts no
longer advertise the removed endpoints, and the relevant automated checks
show that supported feature journeys still work.

- **For / why:** Maintainers and agents stop reading, maintaining, and
  building on endpoints no feature needs.
- **Value / learning:** Whether the migration's overlapping file-serving
  endpoints are a real distinction or duplication worth consolidating.
- **Effort hypothesis:** S–M. The main usage review is done; the rest is
  confirming it, transitive removal, and one focused comparison.
- **Depends on:** Nothing queued.
- **Safe stopping point:** After the removals land with behavior preserved;
  the proposal is useful independently.

## Ordering and Scope Reduction

The owner requested this story at the top of the product backlog, ahead of
SEED-054#story-1. Establish feature-use evidence before removal. Redundancy
implementation is deferred; the requested recommendations remain in scope.

## Open Decisions

- Whether to implement the file-serving proposal is decided after the owner
  reviews it.

## When to Surface

Next, as the highest-priority queued story requested by the owner.

## Breadcrumbs

- Owner request on 2026-09-29: check the current backend public API through the
  generated TypeScript code or YAML; unused means no frontend or external
  feature use, and tests do not justify keeping an API. Find and clean up dead
  APIs and propose improvements for duplicated services.
- Owner refinement on 2026-09-29: accepted the four removals, the E2E
  exemption, and limiting redundancy review to the file-serving endpoints;
  removal is transitive and followed by whole-product cleanup, with no
  negative tests.
- `.agents/agent-map.md` identifies the generated API summary, TypeScript
  client, OpenAPI YAML, backend routes, frontend, CLI, and MCP entry points.
