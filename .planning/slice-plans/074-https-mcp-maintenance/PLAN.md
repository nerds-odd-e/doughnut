# Maintain one coherent HTTPS MCP connection

**Source:** [Complete the retirement of the local MCP server](../../seeds/SEED-002-host-mcp-over-https.md#story-4)
**Identity:** SEED-002#story-4

The owner-managed private client migration was verified on 2026-10-28 as
recorded below. Execution still requires a separate instruction.

## Goal and scope

Users configure the backend HTTPS MCP endpoint with a personal token;
maintainers support that connection through accurate guidance and distinct
behavior tests. Complete the source story's transitive deletion and
whole-product reading without rebuilding the delivered service.

The tracked package, workspace membership, build commands, CI wiring, and
process fixtures were deleted in `b8c5d9ae302d4be49c63edeaea06e5b7ffe7b158`.
Current work includes the remaining generated installation, redundant E2E
search examples, misleading feature/guidance text, and obsolete current
transport assumptions in planning consumers. Preserve shared dependencies,
backend tool semantics, attributed reads, notebook authorization, HTTPS
verification, and disposable E2E ownership. No new runtime guard or absence
test accompanies the deletion.

OAuth linking and ChatGPT onboarding belong to sibling stories 2 and 3.
No retrieval redesign, new public API, deployment, client certification matrix,
global test optimization, or arbitrary cleanup of other checkouts is included.

## Owner-managed client migration

On 2026-10-28 Terry Yin selected a separate Cursor-agent handoff to migrate
his private clients and requested landing this preparation. That migration
does not expand this story's implementation scope. Before taking this story,
obtain its result: the private `doughnut` entry connects directly to the
verified HTTPS `/api/mcp` endpoint with a personal bearer token, discovers
`list_note_candidates`, `read_note`, and `get_note_graph`, and affected local
clients have reconnected with their bundle processes released. An unavailable
endpoint or still-active consumer remains explicitly unresolved with that
owner. No deployment or forced termination of unrelated work is authorized.

After Terry reported the Cursor update, the fresh default-checkout observation
in item 6 below verified the configured HTTPS endpoint, authenticated tool
discovery, and release of the bundle processes. This settles the migration
prerequisite. The generated directory remains for this story's deletion;
recheck its ownership and active consumers at execution because clients and
filesystem contents can change. No private configuration change, deployment,
or process termination is part of this preparation.

## Existing owners and decisions

PFE reading followed the tool boundary into existing backend search,
attribution, and graph services, and followed E2E steps through
`mcpAgentActions` → `mcpClientTasks` → `McpClient` → verified TLS forwarding →
`/api/mcp`. Reuse these owners. The root `@modelcontextprotocol/sdk` remains
an active E2E client dependency. `donut-api` and fixture packages have surviving
callers. The TLS proxy and marker/barrier support serve HTTPS and isolation;
they are not exclusive dependents of the deleted package.

Relevant Accepted decisions are [ADR 0002 — Git-native Portable notebook tree
synchronization](../../../docs/adrs/0002-git-native-portable-notebook-synchronization-accepted.md)
for accepted revision attribution, [ADR 0005 — Web routes](../../../docs/adrs/0005-web-routes-accepted.md)
for `/api/mcp`, and [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md)
for disposable proof. The index and in-file statuses agree on Accepted
0000–0007, with no relevant supersession or conflict. ADR 0001's filename
omits the accepted suffix; its status is unambiguous. No ADR change is needed.
The existing North Star's accepted-change boundary is preserved; no new topic
or consequential architecture choice is introduced.

## Outside-in proof ownership

All rows belong to slice 1. These prove surviving behavior or active refusal;
the deletion search is a separate reading below.

| Promise | Retained observable proof |
| --- | --- |
| URL and personal token discover the three tools over HTTPS | MCP feature Background → `connectMcpHttps` and `expectDiscoveredNoteContextTools`; supported setup in the MCP skill and README |
| Title search returns all candidates, distinguishes equal titles, and signals no match | Retain all-candidates, notebook/folder disambiguation, and a dedicated unmatched-query scenario; the generic positive-search rows add no distinct outcome |
| Full reads carry notebook revision attribution and later edits change it | Retain the read/edit/read E2E scenario and `ReadNoteMcpEndpointTest` |
| Graph context follows the requested budget | Retain both small/large-budget outline rows and graph boundary tests |
| Personal tokens resolve their user's readable notes | Retain the two-user E2E scenario and existing MCP access/withdrawal/foreign-note boundary tests; refusals are active permission behavior |
| E2E uses the owning checkout's disposable stack with verified TLS | Focused runner's selected DB/origin plus actual client journey; retain the separate paired `@mcpIsolationProof` scenario, TLS tests, and marker/barrier support unchanged |
| Alias/subscription search and deliberate protocol refusals remain covered | Retain `ListNoteCandidatesMcpEndpointTest`, `McpEndpointTest`, `GetNoteGraphMcpEndpointTest`, and the existing failure-reporting tests unchanged |

Do not confuse shared setup or separate test layers with redundant outcomes.
The two generic positive title-search examples are subsumed by the retained
candidate scenarios and their real search calls. The unmatched query, both
graph-budget cases, attribution transition, two-user permissions, and paired
isolation have different outcomes and stay. Backend tests cover edge/refusal
cases at a different boundary and are not deletion candidates.

## Ordered slices

### 1. Support the direct HTTPS connection with coherent guidance and proof
Type: Behavior
Status: done
Proof: Accepted 2026-10-28. Focused command
`CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`
passed: seven ordinary scenarios, zero failed (~12s scenarios); Cypress 16.2.0 /
Electron 146 / Node 26.12.1; DB
`doughnut_e2e_wt_dc2511cf5a214a79a2e84e834232b217`; origin
`http://127.0.0.1:64570`; exit 0. Boundary: feature → `mcpAgentActions` →
`mcpClientTasks` → `McpClient` → TLS proxy → `/api/mcp`. Ordinary selection
excludes `@mcpIsolationProof` (ownership reused). Consumer-trace after edits
classified feature/helpers as run; isolated-runner admission docs and unchanged
backend MCP/TLS/isolation ownership as reused; skill-name and planning-identity
links excluded. Supported setup in `.agents/skills/mcp-server/SKILL.md` agrees
with the exercised HTTPS personal-token connection. Removal acceptance reading
recorded below. Integration filesystem deletion of
`/Users/terryyin/git/doughnut/mcp-server` completed with zero live consumers;
not a Git-delivered artifact.

Behavior: Given a user with a personal token and a maintainer using the
supported setup, configuring and exercising the HTTPS connection discovers
and uses the backend's read tools with correct attribution, permissions, and
graph limits; the maintained examples and guidance describe that same service.

- Replace the generic three-row search outline with its unmatched-query
  scenario; the all-candidates and disambiguation scenarios own positive
  candidate proof. Delete only support made exclusive by that edit. The
  inspected step and response helpers still have callers, so keep them.
- Correct the feature's claim about automatically updating notes to describe
  the read-only knowledge journey. Express the current MCP guidance directly;
  remove the separate-process absence sentence rather than retaining a
  historical removal note. Keep the existing skill name and its valid links.
- Delete the inventoried generated directory at
  `/Users/terryyin/git/doughnut/mcp-server` during authorized execution. Recheck
  its contents and ownership first: `build/`, `dist/`, and package-local
  `node_modules` links/cache were observed. Remove links without following
  their shared targets. Do not modify the integration checkout's index or
  other work. Live foreign owners were observed in observation 5 and their
  release was verified in observation 6. Recheck at deletion; preserve new
  authored content or unresolved active consumers and stop that deletion for
  their owner.
  This local filesystem effect is not delivered by a Git commit alone.
- Sweep source, generated artifacts, root/package configuration, scripts,
  CI, docs, comments, and agent guidance transitively. Delete discovered
  exclusive dependents; shared owners stay. Inspect the actual execution
  checkout and the named integration checkout separately.
- Refresh any obsolete current transport guidance found in surviving planning
  consumers, using a dated observation of the delivered HTTPS client and
  preserving their promises and actual dated probe evidence. Remove
  obsolete dependent local-package observations only when their consumers
  have been repaired. Retain still-needed dated observations verbatim in
  substance; never rename an observed action. If a retention decision cannot
  be settled within this disposition, leave it with its owner and report it.
  Story wrap-up owns deletion of this story's spent seed/plan material.
- Record the one-time acceptance reading separately from the proof above.

Sizing hypothesis: 5–10 minutes of cohesive edits, inventory, focused proof,
and cleanup, plus the runner's startup wait. The observed ordinary scenarios
took 10 seconds; fresh startup dominated the preparation run. Do not split
test editing from the behavior it preserves. More than 10 minutes of reducible
work or discovery of a live implementation dependency requires finer planning;
the bounded startup wait is the explicit timing exception. Story estimate
remains S (30–60 minutes) including independent refactoring and delivery.

## Consumer trace and verification

- Run the whole focused MCP feature after the final edits:
  `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
  It is already admitted for isolated execution. Its single-run proof does
  not execute the paired isolation scenario.
- Reuse the current ownership of unmodified backend, TLS, runner, and paired
  isolation tests; no fresh broad verdict is claimed by this plan. The
  planned edits do not reach those implementations. If the actual diff changes
  their boundary, rerun the affected proof: all backend tests via
  `CURSOR_DEV=true nix develop -c pnpm backend:test_only` for backend changes
  (backend skill requirement), TLS tests for proxy changes, and the existing
  isolated-runner/paired proof for changes to isolation ownership. Replan
  before changing that infrastructure; a single-run pass cannot replace
  cross-worktree isolation proof.
- Exclude extra frontend/CLI suites: no shared runtime or API effect is
  planned. Keep their shared packages and dependencies. Revise this
  disposition if the consumer trace shows an actual effect on them.
- At the execution diff, run the installed `consumer-trace.mjs --checkout
  <execution-checkout> --base HEAD` with explicit terms
  `AI agent finds relevant notes via MCP`, `listNoteCandidates`,
  `expectResponseContains`, `connectMcpHttps`, and any newly removed name.
  Follow returned steps/helpers to journeys; classify every consumer as run,
  reused, or excluded with a boundary reason. Inspection found the generic
  response and search steps shared by graph, access, and isolation scenarios.
- Follow the required execution delivery order in `AGENTS.md`: Jidoka →
  fresh post-change-refactor agent → API generation if a contract changes →
  coordinator `./scripts/run.sh pnpm format:changed` once → plan update →
  agent commit with independent hook → push, then normal CI ownership and
  story wrap-up. This preparation authorizes none of that execution.

## Retained preparation observations

Observed on 2026-10-28 in
`/Users/terryyin/git/doughnut/.worktrees/complete-the-retirement-of-the-local-mcp-server`
at `d65e2550331fea6682c0405813085516a4164c98`, with only the scratch edits below.

1. **Inventory and dependency premise.** Commands included `git ls-files
   '*mcp*' '*Mcp*' '*MCP*'`, whole-checkout `rg --hidden` plus explicit
   `--no-ignore` readings, and `git grep -n -i -E
   'mcp-server|stdio.*mcp|mcp.*stdio|local mcp|standalone.{0,30}mcp|build:mcp|mcp:build|mcp:test|StdioClientTransport|StdioServerTransport'`.
   `git show --stat b8c5d9ae302d4be49c63edeaea06e5b7ffe7b158` confirmed the
   tracked removal. Reading the complete client/task/page-object chain found
   actual Streamable HTTP requests, a private CA, and `rejectUnauthorized: true`.
   The consuming operation is exercised in observation 2, not inferred solely
   from these files' presence.
2. **Reduced HTTPS journey.** Setup: current MCP feature, its real client,
   personal tokens created by the application, normal disposable fixtures.
   Replaced only the generic search outline with its `Fiona` unmatched-query
   case. Literal command:
   `CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`.
   Result: seven passed, zero failed, 10 seconds of scenarios; all-candidate,
   disambiguation, both graph budgets, accepted revision transition, and
   token-user isolation reached their assertions. Cypress 16.2.0 / Electron
   146 / Node 26.12.1; selected DB
   `doughnut_e2e_wt_fef680329ccc4bd1be657ee8191d4988`, application origin
   `http://127.0.0.1:65436`. The SDK reached the backend through the trusted
   ephemeral HTTPS proxy. Log: `/tmp/story4-mcp-reduced.log` (local, not durable).
3. **Candidate proof discrimination.** With the same reduced feature, added
   temporary `@focus` to its all-candidates scenario. After awaiting the real
   tool call, replaced only `list_note_candidates` response content with `[]`
   in `e2e_test/support/mcp_client.ts`. Repeated the literal command in 2.
   Result: one failed, zero passed, at `expected [] to deeply equal
   [ 'Functional', 'Functional analysis' ]`; exit 1 was expected.
   Log: `/tmp/story4-mcp-discrimination.log`. This demonstrates retained
   positive-search proof discrimination, not mutation testing of every tool.
   Both scratch-edited files were restored from their original bytes;
   `git diff --exit-code -- e2e_test/features/mcp/mcp_services.feature
   e2e_test/support/mcp_client.ts` passed before the seed/state write.
4. **Local artifact location.** `git -C /Users/terryyin/git/doughnut status
   --short --ignored mcp-server`, `ls -la`, and a recursive file/symlink
   inventory found compiled JS under `build/`, the 797201-byte bundle under
   `dist/`, and package-local installation links/cache. Symlinks point to
   shared root packages/dependencies and must not be followed during deletion.
   The same directory is absent in this fresh preparation checkout. No files
   were deleted from the integration checkout during preparation.
5. **Active artifact consumers.** A read-only process inventory using
   `ps -axo pid,command` found seven `node` processes running the exact bundle;
   `ps -p <pid> -o pid=,ppid=,lstart=,comm=` traced them to Cursor extension
   hosts, Cursor's MCP process, and cursor-agent sessions. A JSON reading of
   `/Users/terryyin/.cursor/mcp.json`, reporting only the matching entry's
   name, command, bundle argument and environment-key names, found the
   `doughnut` entry configured with `node` and that bundle. The project-local
   `.cursor/mcp.json` is absent. No token value was exposed, no private
   configuration was edited, and no consumer was stopped. This falsifies
   treating the generated directory as unused. The later owner decision is
   recorded separately above; this observation establishes no migration.

Limits of observations 1–5: ordinary selection excludes `@mcpIsolationProof`; no concurrent
worktree run, production observation, OAuth observation, or new full backend
suite result is claimed. All probes were unpaid and used the owned disposable
E2E allocation. They do not complete the planned slice.

Host decision before launching: local macOS host, 48 GiB RAM, 62% memory free,
load around 8.8; a VM and unrelated Nix work were active. Chose one focused
E2E owner at a time, no parallel heavy suite and no paid service. Both owned
runner invocations exited and shut down their stacks. Recheck host conditions
before execution; do not stop unrelated workloads.

6. **Client migration and readiness review, 2026-10-28.** Checkout:
   `/Users/terryyin/git/doughnut`, revision
   `3d4da7a333d562d2b8ed3c60db2b32c8d2d2d918`, before preparation edits.
   Terry reported that Cursor MCP was updated. A private JSON read of
   `/Users/terryyin/.cursor/mcp.json` found `doughnut` configured with URL
   `https://doughnut.odd-e.com/api/mcp`, an Authorization header, and no command
   or bundle arguments. It reported only transport/endpoint and header names;
   credentials were neither printed nor copied into the repository. Filtering
   `ps -axo pid=,ppid=,comm=,args=` privately for Node processes using the exact
   bundle found zero consumers. `rg --files --hidden --no-ignore mcp-server`
   and a filesystem read found compiled `build/` files, package-local
   installation links/cache, and an empty `dist/`; no artifacts were deleted.
   These results supersede the current prerequisite concern, while preserving
   observations 4–5 as dated evidence.

   Consuming operation:
   `CURSOR_DEV=true nix develop -c node /tmp/story4-verify-cursor-mcp.mjs`.
   This temporary script loaded the installed SDK from this checkout and the
   configured URL/header privately, then awaited `Client.connect` through
   `StreamableHTTPClientTransport` and `Client.listTools` with normal TLS
   certificate verification. Result: passed, exit 0; tools were exactly
   `get_note_graph`, `list_note_candidates`, and `read_note`. Initialize and
   tools/list returned HTTP 200; the observed status sequence was
   `[200, 202, 405, 200]`, including the SDK's auxiliary request. The script
   closed its client. No note tool was invoked and no private note was read.
   This verifies the real deployed discovery path using the migrated entry;
   it is not a new Cursor UI, OAuth, E2E, or paired-worktree verdict.

   Re-read the story, proof mapping, slice, and ADRs 0002, 0005, and 0007.
   `git diff d65e2550331fea6682c0405813085516a4164c98 HEAD --
   e2e_test/features/mcp/mcp_services.feature e2e_test/support/mcp_client.ts`
   was empty. The TLS proxy changes since that probe add browser certificate
   pin metadata and URLSearchParams forwarding; the ordinary MCP JSON path
   used by the retained proof is unchanged. Reuse observations 2–3 within
   their original boundary, with final focused proof still owned by execution.
   Only plan 074 remains under `.planning/slice-plans/`; the conditional plan
   073 consumer repair is no longer pending. No new architecture choice,
   probe slice, scope decision, or unmapped proof remains.

## Removal acceptance reading

Recorded 2026-10-28 during slice 1 execution in
`/Users/terryyin/git/doughnut/.worktrees/complete-the-retirement-of-the-local-mcp-server`
at revision `9a28f258985ac7e594499ef07c6177251a7757c1` with uncommitted slice edits
to `e2e_test/features/mcp/mcp_services.feature` and
`.agents/skills/mcp-server/SKILL.md`, after deleting the integration checkout
filesystem directory `/Users/terryyin/git/doughnut/mcp-server` (ignored/untracked
generated tree; integration Git index not modified). This is a one-time reading,
not an automated test.

Commands (execution checkout unless noted):

```bash
rg --hidden -n \
  -e 'mcp-server/' -e 'donut-mcp-server' -e 'bundleMcpServer' \
  -e 'spawnAndConnectMcpServer' -e 'StdioClientTransport' -e 'StdioServerTransport' \
  -e 'mcp-server\.bundle' -e 'mcp-server:(bundle|test|format|lint)' \
  -e 'the spawned MCP server process has exited' \
  --glob '!node_modules/**' --glob '!.git/**' --glob '!**/pnpm-lock.yaml' .
rg --hidden --no-ignore -n \
  -e 'mcp-server/' -e 'donut-mcp-server' -e 'bundleMcpServer' \
  -e 'spawnAndConnectMcpServer' -e 'StdioClientTransport' -e 'StdioServerTransport' \
  -e 'mcp-server\.bundle' -e 'the spawned MCP server process has exited' \
  --glob '!node_modules/**' --glob '!.git/**' --glob '!**/pnpm-lock.yaml' .
# Per-term exact scans outside .planning for each retired name above
# Integration path: ls /Users/terryyin/git/doughnut/mcp-server (absent after deletion)
```

Classified matches:

| Match | Classification |
| --- | --- |
| This plan's prep observations and this reading naming the search terms | Active preparation / acceptance provenance; retained until story wrap-up |
| `SEED-002` dated evidence mentioning `mcp-server/dist/mcp-server.bundle.mjs` and the integration generated directory | Dated probe evidence; retained verbatim in substance |
| `README.md`, `docs/assistant-oauth.md`, `.agents/agent-map.md` paths to `.agents/skills/mcp-server/SKILL.md` | Retained skill name / HTTPS setup links, not removed implementation |
| Skill name `mcp-server` in `AGENTS.md` / `CLAUDE.md` / unit-testing skill package list | Retained skill name |
| `StdioClientTransport` / `StdioServerTransport` under root `node_modules/@modelcontextprotocol/sdk` and unrelated packages | Third-party SDK / dependency symbols; not Donut local-server wiring |
| Ordinary `stdio` options in scripts/process helpers | Ordinary process I/O; not removed MCP transport |
| `donut-mcp-server`, `bundleMcpServer`, `spawnAndConnectMcpServer`, `mcp-server.bundle`, `mcp-server:(bundle\|test\|format\|lint)`, `the spawned MCP server process has exited` outside `.planning` | No product matches |
| Integration `/Users/terryyin/git/doughnut/mcp-server` | Deleted: `build/` compiled JS, empty `dist/`, package-local `node_modules` links/cache (8 symlinks unlinked without following shared targets). Zero live bundle consumers at deletion. Shared `donut-api` / `donut-test-fixtures` / pnpm store targets intact. No authored `package.json`/`src` present. |

Limits: did not treat ignored dependency trees as product text beyond classifying SDK symbols; did not search other worktrees; ordinary E2E selection still excludes `@mcpIsolationProof`; no production redeploy observation. No permanent absence checker, negative removal assertion, runtime guard, or tombstone was added.

## Concern and sizing assessment

Slice-plan refinement was not needed: the cleanup remains one bounded outcome
and proof loop, with the proposed search simplification exercised and its
retained candidate proof shown to discriminate. The owner settled scope by
selecting private client migration through a separate Cursor-agent handoff.
Observation 6 settles that actual migration result in the named default
checkout with authenticated discovery and a fresh consumer reading. The
existing bounded slice and mapped proof remain suitable. Record ready against
the reviewed story and plan, and auto-land this one-shot preparation under the
owner's instruction. Execution rechecks mutable artifact ownership before
deletion and runs its final focused proof. No remaining scope question,
runtime responsibility move, or speculative structure is proposed.

## Execution complete

Product advice: No backlog change. Story 4 finishes local MCP retirement against the delivered HTTPS service; near-future direction and queue already put SEED-002#story-3 (ChatGPT) next. Wrap-up should close story 4 / spend plan 074; do not invent follow-ups from this cleanup.
