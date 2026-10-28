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
progress and accepted observations are linked below. The completion agreement
waives further live acceptance observations without asserting that they passed.

## Current decisions and existing owners

Execution identity, readiness, observation handles and proof decisions are in
[execution evidence](EXECUTION-EVIDENCE.md#execution-identity-and-readiness).

- PFE: reuse `NoteSearchService` (also used by `SearchController`),
  `AttributedNoteContentService` (shared with `AttributedNoteContentController`),
  and `NoteGraphService` behind the three current MCP tools. Their domain
  meaning already matches the requested search, full read and graph traversal.
  Keep the existing tool names and retrieval owners. Add structured attribution
  to the full-read result for the observed ChatGPT adapter mismatch.
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
  Adjust those existing surfaces only for an observed failure. Record the actual ChatGPT request/result journey alongside its answer.

## Live observation gate

Follow [production proof and human participation](EXECUTION-EVIDENCE.md#production-proof-and-human-participation)
for slice 3. The release and real-client activation need separate operator authority.
Terry authorized landing, release and continued setup on 2026-10-28; the release
and client registration are recorded in [live observations](LIVE-OBSERVATIONS.md).

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

### 2. Expose full-read attribution as structured MCP data
Type: Behavior
Status: done
Proof: Extend the existing authenticated HTTP read test to observe structured
note content and revision equal to the retained text result. Run the full backend
suite red then green. This proves the wire contract, not the browser repair;
slice 3 owns the actual failing-client recheck after release.

Behavior: A successful full read returns the existing attributed note as both
structured data and text, with a matching output schema. Reuse the existing DTO,
mapper and service; retain optional-value semantics and authorization. No new
retrieval, endpoint or client. See [compatibility evidence](READ-COMPATIBILITY.md).

### 3. Release the landed implementation and record the accepted boundary
Type: Behavior
Status: done
Proof: Exact main CI 40625799063 attempt 1 succeeds for 982d32c; all three
unexpired artifacts are available. Release that immutable commit as v1.3.57,
observe the actual publication job and its deployment health evidence.
Reuse the recorded direct-plugin full reads, graph and real-expiry renewal.
Run no additional live acceptance tests, as Terry instructed on 2026-10-28.

Behavior: Publish the reviewed implementation and setup guidance, then close
under the source's human completion agreement. Retain the distinction between
observed direct-plugin success and the unverified normal-browser repair.
Original live rounds 3–9 are retired by that agreement; their original intent,
observations and limitations remain recoverable in Git and linked evidence.
The release workflow's elapsed wait is an explicit external-wait exception.

## Story obligations

### G1. Production release and real connection are not yet observed
Reported: slice 1 — "Acceptance confirms no separately authorized schema-first release receipt, deployed revision or registered ChatGPT configuration has been supplied; real creation/installation, consent and authenticated read remain unobserved."
Story clause: "The plugin creator/operator configures the connection"
Disposition: owner changed "close this execution without further live acceptance tests"

### G2. Real ChatGPT renewal is not yet observed
Reported: slice 1 — "Only the inherited compatible-SDK result exists; ChatGPT expiry, renewal and a fresh read require the connection from slice 2."
Story clause: "After actual access-token expiry, another request succeeds through ordinary renewal without repeating browser consent."
Disposition: owner changed "close this execution without further live acceptance tests"

### G3. Real ChatGPT withdrawal and reconnect are not yet observed
Reported: slice 1 — "Withdrawal, failed new reads/renewal and fresh-consent recovery have not been observed in ChatGPT."
Story clause: "ChatGPT offers an actionable route to connect again; a new grant requires informed consent."
Disposition: owner changed "close this execution without further live acceptance tests"

### G4. Real ChatGPT account isolation is not yet observed
Reported: slice 1 — "No independent second-account connection has exercised private-note search, direct-ID read and graph access alongside the first account."
Story clause: "Its new requests follow its own permissions, including direct note-ID reads and graph access."
Disposition: owner changed "close this execution without further live acceptance tests"

### G5. ChatGPT source selection and attribution are not yet observed
Reported: slice 1 — "Same-title disambiguation, full-read citations including null revision, bounded search retry and instruction-like source handling have no live ChatGPT result."
Story clause: "Use full reads for cited evidence; graph excerpts can be truncated."
Disposition: owner changed "close this execution without further live acceptance tests"

### G6. ChatGPT graph evidence is not yet observed
Reported: slice 1 — "A relevant bounded graph call followed by full reads of cited related notes and evidence/inference distinction has not been observed."
Story clause: "ChatGPT requests graph context with a token budget and reads any related note it relies on for a citation."
Disposition: owner changed "close this execution without further live acceptance tests"

### G7. Computer-off freshness is not yet observed
Reported: slice 1 — "No owner-observed second-device fresh search/read has returned an accepted new marker/revision with the original computer off."
Story clause: "From another device, with the original computer switched off, a fresh conversation still completes a real search and read."
Disposition: owner changed "close this execution without further live acceptance tests"

## Execution complete

Product advice: no further product change is recommended within the owner's
completed delivery scope. [Release and review evidence](RELEASE-CLOSURE.md)
records the successful deployment and waived observations. No result from the
retired live rounds is claimed as passed.
