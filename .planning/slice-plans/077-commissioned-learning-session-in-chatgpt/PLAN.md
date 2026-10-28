# Conduct a commissioned learning session in ChatGPT through MCP

**Source:** [Conduct a commissioned learning session in ChatGPT through MCP](../../seeds/SEED-090-commissioned-learning-via-mcp.md#story-1)
**Source review:** [Independent source review](source-review.md)
**Execution evidence:** [Accepted proof and delivery](execution-evidence.md)
**Identity:** SEED-090#story-1

## Goal and scope

A Donut learner connected through the ChatGPT plugin can ask ChatGPT to
conduct one commissioned Learning Session and have Feedback for completed
Session Items saved in Donut, including an early end. The source story owns
the complete scope, exclusions, key examples, and the owner decision
(2026-10-28) to expand hosted-assistant OAuth with a narrow
`learning_session:record` grant alongside `notes:read` — not `notes:write`,
and not write under `notes:read` alone.

## Exclusions

- Choosing among programs or notebooks when several are available
- Automatically starting a further session
- Replacing the markdown Request/Report protocol with MCP-only schemas
- General note editing or a broad `notes:write` assistant grant
- A second learning-session domain implementation beside
  `LearningSessionService`

## Assumptions

- First available Potential learning session means the first notebook in
  Donut's due-commissioned tracker order after the same first-seen notebook
  grouping the recall UI uses (`useRecallData.potentialLearningSessions`).
- ChatGPT tutors from Request markdown in the conversation; Donut only
  supplies Request and records Report through MCP.
- Request/due tools need an IANA `timezone` argument (no stored user
  timezone). Record may omit timezone; the HTTP path already ignores it for
  scheduling.
- Existing personal-token MCP clients may use the new tools with full-user
  authority; assistant tokens need the matching scopes.

## Existing owners and decisions (PFE)

Reuse, do not duplicate:

- Due commissioned order:
  `UserService.getCommissionedMemoryTrackersNeedToRepeat` /
  `RecallService.getDueMemoryTrackers` → group by notebook first-seen.
- Request and record:
  `LearningSessionService.request` / `record` (partial reports already
  match ADR 0003).
- MCP tool shape: existing `@Component` tools +
  `McpServerConfiguration` registration;
  user from transport context (`TRANSPORT_USER_KEY`).
- OAuth: extend `AssistantAuthorizationConfiguration`,
  `AssistantClientBootstrap`, `AssistantAuthorizationServerConfiguration`
  (`scopes_supported`), `AssistantResourcePolicy` (today exact
  `{notes:read}`), consent page/docs, and place authorized scopes on the
  MCP transport so the record tool can gate writes.

Follow [ADR 0001](../../../docs/adrs/0001-ubiquitous-language.md#commissioned-learning-session),
[ADR 0003](../../../docs/adrs/0003-spaced-repetition-scheduling-policy-accepted.md#commissioned-learning-session),
[ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md) (`/api/mcp`),
[ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md). No new
North Star topic; no ADR conflict when report recording uses a distinct
scope rather than widening `notes:read`.

## Settled decisive premises

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/conduct-a-commissioned-learning-session-in-chatg`.

| Premise | Consuming operation | Observation | Result |
| --- | --- | --- | --- |
| HTTP Learning Session request/record and partial reports work | Slices 2–3 wrap `LearningSessionService` | `CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests 'com.odde.donut.controllers.LearningSessionRequestTests' --tests 'com.odde.donut.controllers.LearningSessionRecordTests' -Dspring.profiles.active=test` | PASS |
| Hosted assistant discovery/consent today advertise and grant only `notes:read`; `notes:write` is rejected | Slice 1 changes policy, bootstrap, discovery, consent | Same suite with `--tests 'com.odde.donut.oauth.AssistantConsentTest' --tests 'com.odde.donut.oauth.AssistantDiscoveryTest'` | PASS — current single-scope model |
| MCP servlet lists registered tools for an authenticated personal token | Slices 2–3 register new tools | `--tests 'com.odde.donut.mcp.McpEndpointTest'` | PASS |
| `AssistantResourcePolicy` requires exact `{notes:read}` | Slice 1 must permit `{notes:read}` and `{notes:read, learning_session:record}` | Read `AssistantResourcePolicy.permits` + tests rejecting `notes:write` / multi-scope with write | Confirmed — multi-scope needs an explicit permitted-set change |
| Consent Allow's joined scope value can grant both permissions | Slice 1 consent form / scopes list | Full backend slice 1 run: SAS rejects joined dual scopes with `invalid_scope` | Invalidated — use repeated native form scope fields on Allow; Deny submits no scopes |
| Tools do not see OAuth scopes today | Slice 3 write gate | Read `ApiBearerAuthenticationFilter` + MCP transport context (user only) | Confirmed — scopes must be added to transport (or equivalent) before the record tool can refuse `notes:read`-only assistants |
| ChatGPT Advanced OAuth accepts requesting `notes:read` and `learning_session:record` together | Slice 4 early probe | Not observed (credentialed ChatGPT) | Open — bounded by slice 4 |

## Outside-in proof ownership

| Source promise | Owning slice and observable proof |
| --- | --- |
| Hosted assistant can be granted `notes:read` + `learning_session:record`; `notes:read` alone still authenticates; write scopes other than the new grant stay refused | 1: discovery, consent, token exchange / MCP auth boundary tests |
| First Potential learning session Request (or none) through MCP | 2: MCP endpoint tests — sole notebook, several (first wins), empty |
| Record Feedback for completed items only; early end / empty report; assistant without record scope refused | 3: MCP endpoint tests with personal token and assistant tokens with/without `learning_session:record` |
| ChatGPT plugin path can authorize both scopes and complete one request→record journey | 4: early owner-operated probe on a real or staging ChatGPT connection after 1–3 |

## Ordered slices

Each leaf targets about five minutes of active change, focused checking, and
cleanup. Slice 4 may wait on owner ChatGPT steps; that wait does not enlarge
code in slices 1–3.

### 1. Grant learning_session:record beside notes:read for hosted assistants
Type: Structure
Status: done
Proof: Extend `AssistantDiscoveryTest`, `AssistantConsentTest`, and related
OAuth/MCP access tests so discovery lists `learning_session:record`, a
consent request for both scopes can be allowed, tokens with
`{notes:read}` or `{notes:read, learning_session:record}` authenticate MCP,
and unauthorized scope sets (including `notes:write`) still fail. Update
consent copy and `docs/assistant-oauth.md` / MCP skill for the dual-scope
ChatGPT setup. Prefer
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests 'com.odde.donut.oauth.AssistantDiscoveryTest' --tests 'com.odde.donut.oauth.AssistantConsentTest' --tests 'com.odde.donut.oauth.AssistantMcpAccessTest' -Dspring.profiles.active=test`
plus focused frontend consent page tests when copy changes.

Internal change: Add `learning_session:record` as a registered hosted-assistant
scope; change `AssistantResourcePolicy` to permit the read-only set and the
read+record set; bootstrap clients and `scopes_supported` / resource metadata;
carry authorized scopes on the MCP transport context; describe report recording
in consent when that scope is requested. Do not grant general note editing.
Enables slice 3's write gate and slice 4's ChatGPT dual-scope link.

### 2. Obtain the first Potential learning session Request through MCP
Type: Behavior
Status: done
Proof: New MCP endpoint tests (personal token) covering: one available
notebook → Request markdown for that notebook; several → first in due order;
none → clear no-session outcome. Prefer focused
`CURSOR_DEV=true nix develop -c ./backend/gradlew -p backend test --tests '<new MCP learning-session request test class>' -Dspring.profiles.active=test`.

Behavior: Given an authenticated MCP client and a timezone → the client
obtains either the Learning Session Request for the first Potential learning
session in Donut's returned order, or an explicit indication that none is
available. Reuse due-commissioned ordering + `LearningSessionService.request`.
Do not add a program picker. Tutoring stays in the chat client.

### 3. Record a Learning Session Report through MCP under the report grant
Type: Behavior
Status: done
Proof: MCP endpoint tests: personal token records Feedback for completed
items only (early end leaves others unchanged; empty report records nothing);
assistant token with `{notes:read, learning_session:record}` can record;
assistant with `{notes:read}` alone cannot. Prefer focused backend MCP tests
named above plus any needed assistant-token fixture helpers.

Behavior: Given an MCP client authorized to record and a Learning Session
Report for a notebook → Donut records Feedback via existing
`LearningSessionService.record` (Grade 1–4, descriptive text, title matching).
Incomplete Session Items stay unscored. Refuse recording for hosted assistants
that lack `learning_session:record` without widening `notes:read`.

### 4. Prove ChatGPT can authorize both scopes and complete one session
Type: Behavior
Status: planned
Proof: Early credentialed probe after slices 1–3: in ChatGPT's custom MCP /
Advanced OAuth settings, request `notes:read` and `learning_session:record`,
complete Donut consent that names report recording, then in one conversation
obtain the first Request and submit a Report for at least one completed item
(or for none if ending immediately). Record deployed/revision or local origin,
scope string used, consent wording observed, tool calls, and Feedback visible
in Donut learning history. If ChatGPT cannot request or retain the second
scope, stop and replan; do not fall back to personal-token-only as meeting
this story's ChatGPT promise.

Behavior: A learner with the ChatGPT plugin linked under both scopes asks to
start learning from Donut → ChatGPT retrieves the first available Request,
tutors, and saves Feedback for completed items through MCP. Update
`docs/assistant-oauth.md` only with steps actually observed.

## Current decisions

- Scope name: `learning_session:record` (colon style, capability-named).
- Recommended direction from refinement: protocol-preserving MCP tools +
  narrow hosted-assistant report grant.
- Slice-plan refinement: not needed — four cohesive slices, one proof loop
  each; ChatGPT interoperability is an authorized early probe (slice 4), not
  an unresolved boundary inside 1–3.

## Execution context

- Established claim: `SEED-090#story-1`, publisher
  `dashboard-territory.local-doughnut`, agent Maria-chan; Story Branch Mode.
- Execution checkout (also originating checkout):
  `/Users/terryyin/git/doughnut/.worktrees/conduct-a-commissioned-learning-session-in-chatg`;
  branch `codex/conduct-a-commissioned-learning-session-in-chatg`.
  Existing owned linked worktree; no integration checkout selected.
- Authorized publication: `origin` (`nerds-odd-e/doughnut`), trunk `main`;
  increments publish to the execution branch. Starting revision
  `ed515bc49f23186828a7c439ba762b2ec4424b72`; accepted claim and initial
  candidate `9acdb9fceb886a6b766f76d7a8aac378c4e77e20`, confirmed on both
  remote branches on 2026-10-28. Claim CI: unobserved.
- Checkout setup: `./scripts/run.sh bash scripts/worktree_setup.sh` passed
  (locked workspace installation current), then
  `./scripts/run.sh pnpm -C frontend exec vue-tsc --version` passed.
- Replanning: existing planned-work authority retained; no `--no-replan`.
- CI: GitHub Actions `ci.yml`, verified active display name `donut CI`;
  target execution branch, coordinator `maria-chan-090`, observer
  `/tmp/dough-ci-501/watch-XohUdl`, PID `54859`, stream session `8558`,
  yielded cell `8`. Runtime is the same checkout's
  `.agents/skills/dough-execute-plan`. Reuse this observer for increments.

## Remaining concerns

None identified for readiness beyond slice 4's early probe, which the plan
already bounds. Existing assistant grants remain read-only until reconnect
with the new scope set; that reconnection is part of slice 4 / operator docs,
not a separate readiness block.

## Story obligations

### G1. Native consent must grant the requested dual scopes
Reported: slice 1 — "SAS consent expects repeated scope form fields, whereas the existing Allow button submits notes:read learning_session:record as one value and dual consent fails invalid_scope."
Story clause: "Expand hosted-assistant OAuth and consent with a narrow grant for recording Learning Session Reports"
Disposition: proved by slice 1: AssistantConsentTest.contextComesFromThePendingGrantAndAllowPersistsTheSamePermission (repeated scope fields, persisted exact scopes) and AssistantConsentPage.spec.ts (native FormData.getAll on Allow and no scopes on Deny); full backend and browser tests pass.
Commitment: C20

### G2. SDK dual-scope journey observation
Reported: slice 1 — "CI expected the read-only consent heading, but discovery makes the real SDK request both supported scopes."
Story clause: "Expand hosted-assistant OAuth and consent with a narrow grant for recording Learning Session Reports"
Disposition: proved by slice 1: focused assistant_account_linking.feature reproduces the old expectation failure, then observes dual-scope consent, renewal, withdrawal and reconnect after repairing expectations; product behavior unchanged.


### G3. Deterministic scheduling regression proof
Reported: slice 2 — "The full backend suite failed shouldWeakenUniqueMatchedSpellingTrackerWithoutRecallCredit: expected stability 115 but got 116; its clock used an ID-fuzzed due time."
Story clause: "existing Feedback recording and scheduling"
Disposition: proved by slice 2: shouldWeakenUniqueMatchedSpellingTrackerWithoutRecallCredit now fixes elapsed time at lastRecalledAt + 200h, preserving exact stability 115 and every scheduling/no-credit assertion; full 2,975-test backend pass, no production scheduling change.

### G4. Credentialed ChatGPT journey remains unobserved
Reported: slice 4 — "No candidate staging/test HTTPS endpoint and owner-operated ChatGPT connection were supplied; the SDK fixture pass does not observe the real ChatGPT path."
Story clause: "A Donut learner connected through the ChatGPT plugin can ask ChatGPT to conduct one commissioned Learning Session and have Feedback for the Session Items they completed saved in Donut, including when they end early."
Disposition: return
Commitment: C01, C06, C07, C23, C25, E01, E02, E03, E04, E05, E06, E07, E09, E22

## Manual probe waiting

Slices 1–3 are delivered; slice 4 remains planned. Code candidate:
`b261a1eceb8912b9d04eb32c5c4d3ab23c289bdd` on the execution branch.
Resume at the credentialed probe; do not repeat accepted implementation/proof.
The owner must identify a candidate HTTPS environment and ChatGPT connection;
then request both scopes, observe informed consent, Request/tutoring/Report,
and Feedback in learning history (including early-end/no-session observations
owned by the source review). Record candidate origin/revision, actual scopes,
tool calls, result and limitations. No deployment or real client activation
performed. Keep the Taken entry, plan, source and owned worktree. No execution
completion, retrospective, wrap-up or trunk integration is claimed.
