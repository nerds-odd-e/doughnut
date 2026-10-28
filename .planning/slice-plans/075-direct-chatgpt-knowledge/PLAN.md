# Direct ChatGPT knowledge

**Source:** [Use Donut knowledge directly in ChatGPT](../../seeds/SEED-002-host-mcp-over-https.md#story-3)
**Identity:** SEED-002#story-3

## Goal and scope

An existing Donut user creates or installs a personal/custom ChatGPT plugin,
links their own Donut account through GitHub, and uses current, attributable
notes through Donut's direct HTTPS service while their computer is off.
The source owns the complete scope, genuine constraints and key examples.

Terry selected the recommended direct route and production acceptance on
2026-10-28, with no additional scripts. Use ChatGPT's built-in custom-MCP form,
Donut's existing backend and written setup steps. Add no bridge, connector
script, packaged skill or new test harness. Existing repository commands and
stable-boundary tests are available. Public distribution, custom ChatGPT UI,
body search, editing and signup during linking remain deferred.

This is preparation, not execution or release authority. The story-2 supplier
agreement is satisfied by its compatible-SDK result, not by a ChatGPT result.
No slice is done and no real grant, deployment or installation was performed.

## Current decisions and existing owners

- PFE: reuse `NoteSearchService` (also used by `SearchController`),
  `AttributedNoteContentService` (shared with `AttributedNoteContentController`),
  and `NoteGraphService` behind the three current MCP tools. Their domain
  meaning already matches the requested search, full read and graph traversal.
  Keep the existing tool names and result contracts. The observed gap is client
  metadata, not a second retrieval implementation.
- Keep protocol handling and rotating credentials in the existing Spring
  Authorization Server integration. ChatGPT is the confidential hosted client;
  the Donut user remains the knowledge principal. Use predefined credentials,
  `client_secret_basic`, S256, `notes:read` and the exact callback shown for the
  actual connection. Configure default scopes in ChatGPT; introducing CIMD/DCR
  or an anonymous discovery mode is not required by observed evidence.
- Source attribution follows [ADR 0002](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md):
  the full-read result's revision is the accepted whole-notebook commit, possibly
  null. A graph excerpt is not a full-read citation. Ordinary citations must let
  the user identify the note and inspect the returned source/revision; this story
  does not add a historical-note viewer or Company Knowledge search/fetch API.
- Follow [ADR 0005](../../../docs/adrs/0005-web-routes-accepted.md), including only
  the two previously approved discovery exceptions in
  [assistant-oauth.md](../../../docs/assistant-oauth.md). Follow
  [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md) for
  local disposable verification and operator-owned production observations.
  The current North Star's accepted-content boundary remains intact; no new
  architecture topic, ADR decision or exception is needed.
- Guide the librarian sequence through current descriptors and written prompts:
  candidates → full reads → relevant graph exploration → cited evidence.
  Adjust those existing surfaces only for an observed failure. A model's answer,
  successful SDK call or displayed setup form alone is insufficient proof of
  the actual ChatGPT request/result journey.

## Premises and observations

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/use-donut-knowledge-directly-in-chatgpt`,
at `3d4da7a333d562d2b8ed3c60db2b32c8d2d2d918`. Fetched trunk later advanced to
`58112ba5f50b79a8377518dbb6f9900ebe2accd2` with story-4 preparation changes;
that does not change this observation's code revision.

| Premise / consuming operation | Observation and result | Remaining proof owner |
| --- | --- | --- |
| Existing tools need accurate read-only descriptors for client discovery | A scratch assertion in `McpEndpointTest.ProtocolNegotiation.initializesAndListsRegisteredToolsThroughServlet` expected `$.result.tools[0].annotations.readOnlyHint == true` in the actual authenticated `tools/list` HTTP response. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` ran 2,961 tests with precisely that failure (missing `annotations`) and two skips. The test file was restored byte for byte before record writes. | Slice 1 adds the real contract and a permanent discriminating boundary test; no passing metadata implementation is claimed. |
| Production discovery is ready for ChatGPT | On 2026-10-28 local time, `curl --silent --show-error --max-time 20 --dump-header /tmp/donut-chatgpt-resource-headers.txt --output /tmp/donut-chatgpt-resource.json https://doughnut.odd-e.com/.well-known/oauth-protected-resource/api/mcp` returned HTTP 200 HTML. The same command with `/tmp/donut-chatgpt-issuer-headers.txt`, `/tmp/donut-chatgpt-issuer.json` and `https://doughnut.odd-e.com/.well-known/oauth-authorization-server` also returned HTML. The same request with `/tmp/donut-chatgpt-mcp-headers.txt`, `/tmp/donut-chatgpt-mcp-response.txt` and `https://doughnut.odd-e.com/api/mcp` returned HTTP 401 without `WWW-Authenticate`. | False at observation time. Slice 2 waits for the independently authorized release, then reaches real discovery and linking. Do not infer the operational cause or deploy from this plan. |
| The actual ChatGPT account offers the intended client configuration | Chrome → `https://chatgpt.com/plugins` → Add → Add custom MCP server → public Donut URL → Advanced OAuth settings. The form exposed User-Defined OAuth Client, a connection-specific callback, credentials, scopes and `client_secret_basic`; discovery visibly failed. Cancelled without creating anything or entering credentials. | Slice 2 proves creation, installation, GitHub/Donut consent and an authenticated read. The abandoned callback is not reusable configuration. |
| ChatGPT supports this grant's complete lifecycle and account isolation | Story 2's inherited SDK evidence exercises the backend, but does not observe ChatGPT. Neither public GETs nor the configuration form reaches renewal, withdrawal or the second account. | Early probes 3–5 reach these credentialed operations before the answer-quality work. |
| ChatGPT uses full current source evidence, graph context and direct cloud access | Existing descriptors and source code expose these operations. No live ChatGPT answer or computer-off observation was made. | Slices 6–8 own the real outcomes; the source reading is not accepted journey proof. |

Official guidance fetched on 2026-10-28:
[custom MCP setup](https://developers.openai.com/api/docs/guides/custom-mcp-server),
[OAuth](https://developers.openai.com/plugins/build/auth), and
[MCP descriptors](https://developers.openai.com/plugins/build/mcp-server).
Accurate read-only annotations describe the existing behavior; they do not
replace server authorization. The documented predefined-client path and actual
form justify trying it, not claiming interoperability.

The local observation ran one backend suite on this Mac (16 logical CPUs,
48 GiB memory; initial load averages 10.38/8.74/8.60). It used only assigned
disposable database `doughnut_wt_1326852caae5497c9310b5ace6e44c47_test`.
Reported migration and test phases took 18 s and 79 s respectively. No other
suite failure was reported. This is an intentional failing premise observation,
not a green baseline after restoration. Execution reassesses host capacity
before its required suite; no new profiling script is needed.

## Production proof and human participation

Use `https://doughnut.odd-e.com/api/mcp`. Before slice 2's live action, Terry or
the deployment operator supplies the separately authorized schema-first release
receipt, deployed immutable revision and registered ChatGPT client configuration,
following `docs/assistant-oauth.md`. Current production discovery is known to be
unavailable; repeating creation against it is not useful. The release remains
outside this preparation's authority. Execution stops at that gate until it is
satisfied, without substituting a local tunnel, mock host or another deployment.

At the first request for owner participation, present the complete bounded
observation sequence below, not successive surprise requests. Terry supplies the
ChatGPT account, two existing Donut accounts with deliberately chosen, non-sensitive
acceptance notes, and a second device. Keep credentials out of source, transcripts
and screenshots. The owner performs credential entry, consent and any required
security confirmation. Normal UI edits to the owner's acceptance notes are the
only production fixture setup; no automated reset or testability endpoint is used.

Record in this plan for each actual round: deployed revision, date, setup/account
labels (no secrets), literal UI steps and prompts, selected tools and arguments,
returned evidence, final visible result and limits. Retain fresh source markers
and revisions sufficient to distinguish new reads from chat history. When the
agent can inspect these results, it does so; Terry supplies only observations the
tools cannot reach, particularly the computer-off round. Reuse passing rounds
unless the changed candidate alters their boundary. Authentication compatibility
failures stop downstream work for diagnosis and in-place replanning; a script,
new provider or changed grant model is not an automatic fallback.

## Outside-in proof ownership

| Source promise | Owning slice and observable proof |
| --- | --- |
| Accurate read-only tool discovery | 1: serialized `tools/list` annotations at the HTTP boundary; 2: actual installed ChatGPT tool list. |
| Personal creation/installation, GitHub identity, informed allow/deny and initial read | 2: cancel then retry/allow through real ChatGPT, with a private acceptance-note read. |
| Ordinary renewal without repeat browser consent | 3: actual expiry followed by a new successful MCP read and renewal evidence. |
| Withdrawal invalidates access/renewal; actionable reconnect and fresh consent | 4: new calls fail after withdrawal, reconnect visibly asks again and restores a read. |
| Current permissions and independent user grants, including direct IDs and graph | 5: B's authorized read succeeds while requests for A's private acceptance note are refused by Donut; A retains access. The retained backend current-permission tests, run in 1, prove permission reevaluation on each request. |
| Title/alias candidates, same-title disambiguation, empty-result feedback, attributed full reads and evidence/instruction distinction | 6: real prompts and tool traces, with an answer attributable to full-read evidence and a bounded retry after no match. |
| Bounded graph context used as relevant evidence, full reads for cited related notes | 7: graph call with an explicit budget, related full-read calls and an inspectable answer. |
| Current accepted content/revision while original computer is off | 8: owner edits an acceptance note, powers off the original computer, then retrieves the new marker/revision from another device in a fresh chat. |
| Reproducible onboarding and recovery instructions without another script | 2–8: build the guide incrementally from the actual steps; slice 5's second account follows it, and later rounds use its renewal/reconnect guidance. |

## Ordered slices

Each leaf targets about five minutes of active change, focused checking and
cleanup. The measured backend-suite duration is included in slice 1. The named
live rounds may exceed ten elapsed minutes only for operator release, browser
sign-in or real token-expiry waits; those waits do not justify larger code changes.
At an active-work overrun, record the disproved assumption and finer-decompose
before continuing. No slice can be completed by reporting its unproved promise.

### 1. Advertise the existing read operations accurately
Type: Behavior
Status: planned
Proof: Extend `McpEndpointTest` at authenticated `tools/list` to observe the three
tools' accurate annotations, first failing without the change and then passing.
Run `CURSOR_DEV=true nix develop -c pnpm backend:test_only` (all backend unit
tests, required by the backend skill). Do not test three internal tool classes.

Behavior: A compatible client discovers Donut's tools → their serialized
descriptors identify them as read-only, non-destructive operations over bounded
Donut knowledge. Set `readOnlyHint: true`, `destructiveHint: false` and
`openWorldHint: false` in the existing descriptors. Preserve tool names,
arguments, results and authorization. No new endpoint, result DTO or script.
This remains useful to existing clients independently of the ChatGPT probe.

### 2. Link a personal ChatGPT plugin to the real Donut account
Type: Behavior
Status: planned
Proof: Early credentialed probe: current production discovery, actual custom
plugin creation/installation, one cancelled consent and one retry/allow, followed
by a full read of the owner's private acceptance note. Capture the discovered
three tools, returned account-scoped evidence and the exact usable setup steps.

Behavior: After the authorized production release and client configuration,
the creator follows ChatGPT's built-in form with the service URL, exact current
callback, predefined client and `notes:read` → the user can cancel or approve
through GitHub/Donut and complete a read. Add the observed setup to a ChatGPT
section in `docs/assistant-oauth.md`, linked from the MCP guide. If discovery,
exchange or installation fails, record the failing operation and stop slices
3–8; no speculative auth replacement or extra script is authorized.

### 3. Continue reading after normal access expiry
Type: Behavior
Status: planned
Proof: Early lifecycle probe on slice 2's connection. Record issued-token expiry
from available operator evidence, wait for actual expiry (default five minutes),
then issue a fresh read. Require renewal evidence and returned source, not just
a model answer reusing history. No browser reauthorization during successful renewal.

Behavior: The linked user's access token expires while the grant remains valid →
the next request renews and reads successfully through the normal client. Update
the guide's continuation behavior from the observed result. Do not shorten
production lifetimes or simulate time. A failure stops dependent rounds and
triggers diagnosis at the existing OAuth boundary. Explicit external-wait exception.

### 4. Withdraw permission and reconnect deliberately
Type: Behavior
Status: planned
Proof: Early recovery probe: owner withdraws the Donut client grant in Settings,
requests a fresh read in ChatGPT, observes authentication refusal and failed
renewal, then follows the recovery action to fresh informed consent and a new read.
Use operator-visible auth outcomes when the ChatGPT surface hides renewal attempts;
missing visibility stays an unproved result, not an inferred pass.

Behavior: Withdrawal ends this user's client grant → new reads cannot succeed
through its outstanding credentials, and reconnecting requires new consent.
Document the actual ChatGPT recovery action and that prior conversation content
remains disclosed. Do not promise that uninstalling a plugin substitutes for
Donut withdrawal. Stop subsequent rounds on a failed control boundary.

### 5. Keep two users' connections independent
Type: Behavior
Status: planned
Proof: Early account probe: a second owner-operated ChatGPT connection follows
the written guide and links Donut account B. Observe B reading its own note,
then its search, direct full-read and graph requests against A's private marker
under B's grant. Inspect backend/tool refusals, not only the model's answer.
Confirm A can still read A's note. Use separate chat/connection contexts.

Behavior: The same plugin is linked by another Donut identity → its new calls
use that identity's current notebook permissions. The operator records the
actual account-switching steps, without adding profile tools or another client
implementation. The retained backend current-permission tests prove request-time
reevaluation; this round proves that ChatGPT uses the independently linked identities.

### 6. Answer from identifiable full-note evidence
Type: Behavior
Status: planned
Proof: In ChatGPT, use a direct and an ordinary-language topic request against
two same-title acceptance notes in distinguishable notebooks/folders, including
a repository-bound note and one with a null revision, then an unmatched query
with a corrected title/alias. Record candidates, selected IDs,
full reads and final citations. Include one harmless instruction-like sentence
inside a test note and observe it treated as source text, not a new instruction.

Behavior: The user's topic request → ChatGPT chooses the relevant candidate or
asks for the missing distinction, reads full contents, and identifies each cited
note and its returned notebook revision. Empty search explains the title/alias
limit and offers a useful retry. A null revision remains explicit when the
selected note has none. Improve only existing descriptions or written prompts
if the observation warrants it; preserve the natural search rule. Re-run affected
prompts after any descriptor change and perform the required backend check if
Java changes. Record successful plain instructions in the guide.

### 7. Explain relevant connections with bounded graph evidence
Type: Behavior
Status: planned
Proof: A follow-up asks how a known note relates to another idea. Observe
`get_note_graph` with an explicit token budget and full reads for related notes
actually cited, then compare the answer with those sources. The prompt should
require a relation present in the chosen acceptance notes.

Behavior: The follow-up needs connections beyond one note → bounded graph context
guides further reading and the answer separates source evidence from inference.
Truncated graph text is not presented as a complete note or invented revision.
Retain a successful follow-up example in the same guide. Any metadata correction
stays in existing tools and rechecks the affected earlier prompt observations.

### 8. Retrieve an accepted update with the original computer off
Type: Behavior
Status: planned
Proof: Terry edits a designated acceptance note through the normal Donut UI and
records the accepted content/revision. Power off the original computer. From a
second device, use the installed plugin in a fresh ChatGPT conversation to search
and read the new marker; retain the actual tool result and returned revision.
Terry records the power state because this agent cannot observe its own powered-off
host. Compare with the earlier read rather than inferring freshness from prose.

Behavior: Donut accepts new content and the user's original computer becomes
unavailable → a new request still reads that current content directly from the
hosted service. Finalize the guide from the demonstrated journey, including its
prerequisites and actual source attribution. Stop short of story completion if
the off-device observation is unavailable. Explicit owner/device-wait exception.

## Verification and delivery

New metadata proof belongs at `McpEndpointTest`'s real serialized servlet
boundary. Existing callers are the HTTPS SDK E2E client and any MCP consumer;
the REST/CLI retrieval boundaries keep their payload semantics. The inspected
E2E client consumes tool names and content, not read-only annotations. Existing
MCP and OAuth tests cover retained server behavior; they do not replace the
ChatGPT observations. Source inspection located these consumers; slice 1 runs
the full backend suite and retains its actual results before accepting the change.
If implementation alters payloads, auth handling or other contracts, extend the
consumer trace and proof before proceeding rather than inheriting this exclusion.

When a live probe reveals a defect, first isolate it with the existing high-level
MCP/OAuth tests and refine the affected remaining slice before implementation.
Use E2E only when the changed browser/HTTPS journey requires it, through the
existing `e2e_test/features/mcp/assistant_account_linking.feature` and
`mcp_services.feature` runners. No new scripts or duplicate hosted client.
Generated REST clients need regeneration only if a REST signature/DTO changes.

Execution follows the repository's required delivery sequence: Jidoka, a fresh
dough-post-change-refactor agent, API generation when required, coordinator
`./scripts/run.sh pnpm format:changed` once, plan update, agent commit with the
check-only hook, and publication with the selected execution mode and CI owner.
This preparation performs no execution claim or implementation. Keep probe
failures and unproved story promises in this plan under the execution workflow's
story-obligation rules; publication never makes them complete.

## Plan review

Slice-plan refinement was not needed: the eight leaves each own one externally
observable outcome and proof loop. The early credentialed probes isolate setup,
renewal, withdrawal and account scope before answer-quality work. The last leaf
combines freshness and computer independence in one discriminating new-read round.
The cumulative design extends one existing MCP/OAuth model and one guide, without
an additional runtime, script or knowledge representation.

No unbounded slice-specific concern remains. Actual ChatGPT compatibility and
production availability are explicitly unproved and bounded by the early probes;
their owner-held release and credential requirements remain in force. Readiness
means this bounded plan can be selected for execution, not that those probes
passed or that deployment is authorized. No human refinement decision remains.
