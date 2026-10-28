# Direct ChatGPT knowledge: execution evidence

[Active plan](PLAN.md) · SEED-002#story-3

These records preserve preparation and slice 1 observations before the subsequent
release and client registration. See [live observations](LIVE-OBSERVATIONS.md)
for the original slice 2 handoff. Current evidence is in
[connection evidence](CONNECTION-EVIDENCE.md); remaining slices were renumbered
under [compatibility refinement](READ-COMPATIBILITY.md).

## Slice 1 accepted observations

On 2026-10-28, the owned execution checkout's authenticated `tools/list`
response advertised all three existing tools with `readOnlyHint: true`,
`destructiveHint: false` and `openWorldHint: false`. The package-private
`McpServerConfiguration.READ_ONLY_TOOL_ANNOTATIONS` owns that policy; existing
descriptors reference it. Names, schemas, handlers, results and authorization
are unchanged. No REST signature/DTO changed, so no API generation is required.

Proof uses `McpEndpointTest.ProtocolNegotiation.initializesAndListsRegisteredToolsThroughServlet`:
real POST `/api/mcp`, three serialized annotation-array assertions. Setup is
`McpEndpointTestBase.issuePersonalToken` with persisted `makeMe` user/token data.
The implementation agent owned the red and initial green terminal results:

- `CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-chatgpt-slice1-red.log 2>&1`
  exited 1: 2,961 tests, exactly the new missing-`readOnlyHint` assertion failed,
  two skips. No production metadata implementation existed at that observation.
- `CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-chatgpt-slice1-green.log 2>&1`
  exited 0: 2,961 tests, zero failures/errors, two skips, 65-second test phase.
- Fresh refactor agent `/root/metadata_refactor` consolidated repeated metadata
  into the constant. The coordinator then owned
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only > /tmp/donut-chatgpt-slice1-refactored.log 2>&1`
  to exit 0: 2,961 tests, zero failures/errors, two skips, 66-second test phase.
  All 598 XML suite reports were counted. Launch load was 5.10/6.35/7.60,
  with no competing owned suite. This replaces the initial green implementation
  observation; the original red remains applicable. Whitespace checking passed.

Refactor returned `## REFACTOR COMPLETE`; no test/setup boundary changed.
The coordinator split retained context/evidence from the executable plan to
satisfy the 250-line limit, preserving dates, revisions and observations.
Consumer traces are `/tmp/donut-chatgpt-slice1-consumer-trace.md` and
`/tmp/donut-chatgpt-slice1-refactor-consumer-trace.md`, generated with the installed
`consumer-trace.mjs --checkout /Users/terryyin/git/doughnut/.worktrees/direct-chatgpt-knowledge --base 926b0d57041a46396c46a5439bc537ed92df0034 --term READ_ONLY_TOOL_ANNOTATIONS --term ToolAnnotations --term readOnlyHint --term destructiveHint --term openWorldHint --term 'tools/list'`
(the first trace predates the constant and omits its term). Every consumer has
a disposition: servlet registration/discovery and retained search/read/graph/
OAuth tests ran; API bearer, Swagger and OAuth endpoint references consume only
the unchanged endpoint constant. The E2E client at `e2e_test/support/mcp_client.ts`
maps discovery to names at lines 50–60 and does not consume annotations.
Planning/guidance matches do not consume descriptors. Actual ChatGPT discovery
is expressly unproved and remains with slice 2, alongside obligations G1–G7.

Next dependent boundary is slice 2's operator gate. No release receipt,
deployed revision or real-client configuration has been supplied. The story
remains Taken; no automatic retrospective, closure or production action is due.

## Execution identity and readiness

Execution started on 2026-10-28 in Story Branch Mode. Originating and
integration checkout: `/Users/terryyin/git/doughnut`. Created owned workspace:
`/Users/terryyin/git/doughnut/.worktrees/direct-chatgpt-knowledge`, branch
`codex/direct-chatgpt-knowledge`, starting revision
`2eec8a2b78c48619aec32b4a7525cf89186a8dd0`. Publisher
`codex-direct-chatgpt-20261028`, agent Tsubomi-chan. Take accepted on
`origin/refs/heads/main` at `926b0d57041a46396c46a5439bc537ed92df0034`;
the execution branch was published at that revision. The default checkout
advanced independently. Execution increments target
`origin/refs/heads/codex/direct-chatgpt-knowledge`. The trunk claim has
`pendingCi: unobserved`; the branch observer owns subsequent increments.
Existing planning authority is retained; no new scope is authorized.

Checkout preparation `./scripts/run.sh bash scripts/worktree_setup.sh`
succeeded for this checkout and its current locked dependencies. The applicable
command `./scripts/run.sh node .agents/skills/dough-execute-plan/scripts/story-obligations.mjs list --plan .planning/slice-plans/075-direct-chatgpt-knowledge/PLAN.md --slice 1`
succeeded with no obligations. Check-only pre-commit lint is installed;
coordinator selective formatting is `./scripts/run.sh pnpm format:changed`.

CI observer: GitHub Actions `ci.yml` (verified active workflow 5208694,
`donut CI`), repo `nerds-odd-e/doughnut`, target
`codex/direct-chatgpt-knowledge`, coordinator
`codex-direct-chatgpt-20261028`, directory `/tmp/dough-ci-501/watch-ro4Sg2`,
PID 76241, PTY session 89097, yielded cell 19. Runtime is this execution
checkout's `.agents/skills/dough-execute-plan`.

Pre-launch proof decision on this Mac: 16 logical CPUs, 48 GiB RAM,
load 7.01/7.63/8.47, about 10.7 GiB inactive memory plus free/speculative pages.
No other owned heavy verification is running; unrelated processes remain with
their owners. Preparation measured the full backend suite at 79 seconds plus
18 seconds migration. Run the required full backend command sequentially for
the discriminating failure and passing candidate; the implementation agent owns
both terminal results. Reconsider on contention, changed workload or failure.


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


## Source review and acceptance mapping

Fresh source-only reviewer `/root/source_promises` was launched with
`fork_turns: none`, only the original story's Goal, Scope, Genuine constraints,
Deferred promises and all six Key examples, and instructions to read no files,
use no tools and derive commitments without slice ownership. The actual launch
text and returned clause-by-clause tables are retained in this execution's
conversation. Comparison with the source preserves the create-or-install
alternative and distinguishes delivery exclusions from authorization refusals.

| Original trigger/setup and expected result | Actual observation and remaining proof owner |
| --- | --- |
| Existing user's ordinary ChatGPT conversation → create or install a personal/custom plugin, link their own Donut identity through GitHub, retrieve current attributable knowledge through direct HTTPS with their computer off. | No live ChatGPT connection has been made by this execution. Slices 2–8 retain this combined journey; slice 1 supplies discovery metadata only. |
| Operator configures the connection; user selects it, signs in and sees read access → separate informed consent, readable notebooks/subscriptions; cancel leaves an incomplete connection with retry; sign-in alone grants no read. Setup/reconnection failures give usable feedback. | Production release receipt and registered client configuration are missing. Slice 2 owns real creation/installation, deny/retry/allow and private full read plus observed guide; slice 4 owns reconnect feedback. |
| First-release request → existing backend/GitHub identity, existing authorization and notebook permissions, `notes:read`, read-only tools; every new request checks permissions. | Slice 1 retains these contracts and runs their existing backend proof; actual ChatGPT identity and current-permission outcomes remain with slices 2 and 5. |
| Same-title notes in different notebooks/folders and a topic request → compare candidate context, clarify ambiguity, read selected notes in full; title, notebook/folder, note ID and full-read notebook revision identify cited evidence. Explain whole-notebook revision, preserving null when none. | Slice 6 owns actual prompts, tool traces and final inspectable citations, including repository-bound and null-revision notes. |
| Empty title/alias result → explain no matching candidates, permit corrected query; do not infer that all Donut content lacks relevant material. | Slice 6 owns no-match/retry observation. Title/alias candidate search, full read and graph capabilities stay with existing services. |
| Retrieved instruction-like note text → treat it as evidence, not application instructions. | Slice 6 owns this live observation; metadata alone establishes no model behavior. |
| Connection follow-up → graph request with explicit token budget; full read of any related note cited; distinguish evidence from interpretation and truncated excerpts from complete notes. | Slice 7 owns graph/tool traces and compared answer. |
| Accepted edit, then fresh request from another device while original computer is off → real search/read returns new accepted content and notebook revision. | Slice 8 owns markers, full tool results, revision comparison and owner-observed power state. Old chat text or a tunnel supplies no proof. |
| Second account links independently and requests the first account's private note → search, direct-ID read and graph follow B's permissions; knowledge of ID grants no access; A retains its own access. | Slice 5 owns the live two-account result and a second guide-following attempt. Backend refusal/current-permission tests remain useful independent proof. |
| Actual token expiry → ordinary renewal and fresh successful read without repeated browser consent. | Slice 3 owns actual expiry and renewal evidence; existing SDK proof cannot establish ChatGPT renewal. |
| Settings withdrawal → new reads and renewal fail; actionable reconnect requires fresh informed consent and restores reading; prior disclosures remain in conversation. | Slice 4 owns live withdrawal, credential outcomes, reconnect and guide updates. |
| Selected direct route → existing backend, ChatGPT setup UI and observed written instructions; incompatibility goes to a decision. | No connector script, bridge or packaged workflow is selected. Deployment and activation remain operator-owned; public directory/UI, body/semantic search, editing, signup during linking, offline copies and universal account-type support remain explicitly deferred. |


## Verification contract

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

## Resumed CI observation

The prior stream ended with a confirmed stopped receipt and process exit.
Resumed same coordinator/checkout/branch on 2026-10-28; GitHub selector `ci.yml`,
workflow 5208694 (`donut CI`) verified active. Cell 146, session 58407, PID 41368,
directory `/tmp/dough-ci-501/watch-ggakYT`, coordinator
`codex-direct-chatgpt-20261028`. This stream owns subsequent publications.
