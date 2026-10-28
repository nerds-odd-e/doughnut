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

Preparation granted no release authority. The story-2 supplier agreement is
satisfied by its compatible-SDK result, not by a ChatGPT result. Execution
progress and acceptance evidence are linked below; live results remain unproved.

## Current decisions and existing owners

Execution identity, readiness, observation handles and proof decisions are in
[execution evidence](EXECUTION-EVIDENCE.md#execution-identity-and-readiness).

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

## Preparation evidence

See the dated [premises and observations](EXECUTION-EVIDENCE.md#premises-and-observations).

## Live observation gate

Follow [production proof and human participation](EXECUTION-EVIDENCE.md#production-proof-and-human-participation)
before slice 2. The release and real-client activation need separate operator authority.

## Ordered slices

Each leaf targets about five minutes of active change, focused checking and
cleanup. The measured backend-suite duration is included in slice 1. The named
live rounds may exceed ten elapsed minutes only for operator release, browser
sign-in or real token-expiry waits; those waits do not justify larger code changes.
At an active-work overrun, record the disproved assumption and finer-decompose
before continuing. No slice can be completed by reporting its unproved promise.

### 1. Advertise the existing read operations accurately
Type: Behavior
Status: done
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

## Story obligations

### G1. Production release and real connection are not yet observed
Reported: slice 1 — "Acceptance confirms no separately authorized schema-first release receipt, deployed revision or registered ChatGPT configuration has been supplied; real creation/installation, consent and authenticated read remain unobserved."
Story clause: "The plugin creator/operator configures the connection"
Disposition: receiving slice 2

### G2. Real ChatGPT renewal is not yet observed
Reported: slice 1 — "Only the inherited compatible-SDK result exists; ChatGPT expiry, renewal and a fresh read require the connection from slice 2."
Story clause: "After actual access-token expiry, another request succeeds through ordinary renewal without repeating browser consent."
Disposition: receiving slice 3

### G3. Real ChatGPT withdrawal and reconnect are not yet observed
Reported: slice 1 — "Withdrawal, failed new reads/renewal and fresh-consent recovery have not been observed in ChatGPT."
Story clause: "ChatGPT offers an actionable route to connect again; a new grant requires informed consent."
Disposition: receiving slice 4

### G4. Real ChatGPT account isolation is not yet observed
Reported: slice 1 — "No independent second-account connection has exercised private-note search, direct-ID read and graph access alongside the first account."
Story clause: "Its new requests follow its own permissions, including direct note-ID reads and graph access."
Disposition: receiving slice 5

### G5. ChatGPT source selection and attribution are not yet observed
Reported: slice 1 — "Same-title disambiguation, full-read citations including null revision, bounded search retry and instruction-like source handling have no live ChatGPT result."
Story clause: "Use full reads for cited evidence; graph excerpts can be truncated."
Disposition: receiving slice 6

### G6. ChatGPT graph evidence is not yet observed
Reported: slice 1 — "A relevant bounded graph call followed by full reads of cited related notes and evidence/inference distinction has not been observed."
Story clause: "ChatGPT requests graph context with a token budget and reads any related note it relies on for a citation."
Disposition: receiving slice 7

### G7. Computer-off freshness is not yet observed
Reported: slice 1 — "No owner-observed second-device fresh search/read has returned an accepted new marker/revision with the original computer off."
Story clause: "From another device, with the original computer switched off, a fresh conversation still completes a real search and read."
Disposition: receiving slice 8


## Delivery evidence

[Execution evidence](EXECUTION-EVIDENCE.md) retains the source-only review, original
proof ownership, verification contract, plan review and accepted observations.
