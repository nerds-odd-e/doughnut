# Prove isolated CLI peer safety, then admit MCP E2E workflows

Sources: [SEED-015 Story 4](../../seeds/SEED-015-concurrent-worktree-environments.md#story-4)
and [Story 5](../../seeds/SEED-015-concurrent-worktree-environments.md#story-5),
plus completed quick/088 (recover its plan at `419135973b`;
merged by `c3c2a5d3a9`).
Status: planned.

## Goal and scope

By explicit developer request, this plan combines the quick/090 corrections
with quick/089's MCP delivery, in that order. The story boundaries remain
distinct: slice 1 closes the delivered CLI story's proof and documentation
gaps; slices 2–3 deliver MCP isolation. Completing slice 1 is a safe stopping
point even if MCP delivery is deferred.

First prove that the already-admitted installed CLI clone/pull/publish
workflow retains its own notebook data while a peer worktree resets and
uses its own data. Publish the supported CLI command in the usage guidance.
This corrective work admits no additional CLI spec.

In an isolated linked worktree with an existing application allocation
(1a–1c, 2, 2a), run `pnpm sut` then
`pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature`. The MCP
client's search and note-graph tool calls must reach that worktree's own
backend and data, and disconnecting the MCP client must leave no spawned MCP
server process still running, so this spec run alongside another worktree's
MCP, CLI, or browser run does not read, reset, or outlive into either side's
resources.

Add exactly this one MCP spec to the isolated-run allowlist (there is
currently only this one MCP feature file). Fix the existing MCP client
disconnect path so it actually terminates the process it spawned.

Excluded: any future MCP tool or feature file, additional CLI workflows or
interactive/OAuth behavior,
every other browser spec, changes to `mcp-server`'s tools or bundling beyond
what isolated selection needs, a new MCP-specific lease or process registry,
general process supervision, Cloud VM/CI changes, and any change to the
runner-lease or retirement mechanisms themselves.

## CLI correction provenance

- `09fdd29e1f`: original execution-ready quick/088 plan.
- `af60083602`: threads the resolved origin into both CLI subprocess paths.
- `419135973b`: admits the CLI spec, adds guard tests, and marks quick/088 done.
  Aggregate review boundary: `af60083602^..419135973b`; merged by
  `c3c2a5d3a9`. Unrelated quick/087 and dependency changes were excluded.
- The guard test checks two Cypress URLs but does not spawn the CLI or
  observe notebook data. Recorded live evidence covers one isolated 4/4 run
  and the retirement veto, not the planned paired run. This is a proof gap,
  not a confirmed runtime data leak; unrecorded checks may have run.
- The browser-test guide and Cypress-origin rule still omit CLI support.
  Both findings remained at `33b2b34ebc`. The focused guard test passed 2/2:
  `CURSOR_DEV=true nix develop -c node --test scripts/isolated-cypress-cli-spec.test.mjs`.
- Existing single-worktree and retirement-veto evidence remains valid.
  Process retrospective was excluded by the developer.

## Current decisions

- `e2e_test/start/pageObjects/mcpAgentActions.ts`'s `connect()` already passes
  `baseUrl: e2eAppBaseUrl()` (`e2e_test/support/e2eAppUrl.ts`), which resolves
  `Cypress.config('baseUrl')` — the same isolated origin
  `scripts/isolated-cypress.mjs`'s `guardCypressNodeSetup` already assigns
  before `e2e_test/config/common.ts` registers the MCP tasks. Unlike CLI story
  4, there is no hardcoded-origin bug here; this plan changes no origin
  resolution code, only proves the existing wiring against an isolated
  allocation.
- `e2e_test/support/mcp_client.ts`'s `disconnectMcpServer()` is dead code
  today: it checks `(this.transport).child` and
  `(this.client).disconnect`, but the installed SDK's
  `StdioClientTransport` exposes no `child` property (only a private
  `_process` and a `pid` getter) and its `Client` (via the shared `Protocol`
  base class) exposes `close()`, not `disconnect()`. Both `if` conditions are
  therefore always false, so today calling `disconnectMcpServer()` never
  terminates the spawned MCP server child process — it only drops the
  in-memory references. The SDK's own `StdioClientTransport.close()` already
  does the correct bounded cleanup (await close, escalate to SIGTERM after
  2s, then SIGKILL after another 2s if still alive), and `Client.close()`
  cascades straight into it. Calling `await this.client.close()` is therefore
  sufficient; no new process-tracking code is needed.
- `mcp-server/dist/mcp-server.bundle.mjs` is built per-worktree checkout
  (`bundleMcpServer` task builds it on demand under that checkout's own
  `mcp-server/dist`), so two worktrees never share a bundle path or process;
  no isolation change is needed there.
- `scripts/isolated-cypress-spec-selection.mjs`'s
  `SUPPORTED_ISOLATED_CYPRESS_SPEC(S)` gates every isolated run before the
  generic Cypress runner lease (`scripts/sut-owner.mjs`) is acquired, exactly
  as for CLI story 4. Adding the one MCP spec to that list admits it into the
  existing lease, health-verified-owner (2a), and retirement-veto (6/6a)
  machinery unchanged.
- `mcp_services.feature` calls only Donut's own backend tools (note search,
  note graph); it uses no OpenAI, Google, or other mocked external service, so
  no mock work belongs to this plan.
- `e2e_test/config/*.test.ts` colocated unit tests (e.g. the existing
  `cliE2eManagedPty.test.ts`) are not wired to a root `package.json` script
  today. They run via `node:test` through a workspace-provided `tsx`, e.g.
  `cli/node_modules/.bin/tsx --test e2e_test/support/mcp_client.test.ts`. This
  plan uses that same command for its new test; adding a package script is
  outside this plan's scope.

## Outside-in proof map

| Example | Observable result | Proof owner |
| --- | --- | --- |
| Installed CLI workflow overlaps a peer browser reset | Both workflows complete with their own expected notebook contents after the ordered reset | Slice 1 |
| Developer follows the isolated-run guide | CLI command and paired proof are documented; MCP command is added only when admitted | Slices 1 and 3, respectively |
| Disconnect while an MCP server child is running | The spawned process actually exits before disconnect resolves | Slice 2 |
| Existing (non-isolated) MCP spec run, unchanged | `mcp_services.feature` still passes across all three scenarios (each Background reconnects) | Slice 2 (regression) |
| Isolated worktree runs the allowlisted MCP spec | MCP tool calls reach that worktree's own backend and notebook data | Slice 3 |
| Two isolated worktrees run the MCP spec concurrently | Each MCP server searches/reads its own worktree's data; neither run's process or data affects the other | Slice 3 |
| Mixed selection or a non-allowlisted spec | Refused before fixture/client setup; existing allowlisted single specs remain supported | Slice 3 (regression) |
| `pnpm worktree:retire --check` during an active allowlisted MCP run | Refuses reclamation via the existing busy Cypress runner lease veto | Slice 3 |

## Ordered slices

### 1. CLI notebook state survives a peer worktree reset
Type: Behavior
Status: planned

Behavior: Given two healthy isolated worktrees with separate allocations,
when the installed CLI workflow runs in one while the browser workflow
resets and edits the other's data, both complete with their own expected
notebook contents.

- Reuse `scripts/worktree-reset-isolation-barrier.mjs` and
  `scripts/worktree-reset-isolation-harness.mjs` for an explicit `cli` mode:
  CLI peer and browser resetter. Preserve existing modes; introduce no new
  scheduler or ownership mechanism.
- Signal the peer-seeded barrier after the selected CLI scenario's
  notebook/token setup and before clone/pull/publish. Without the paired
  environment, the task remains a no-op. Use distinct browser-peer data and
  require its reset after CLI fixtures exist. Synchronize one representative
  scenario so stale barrier files cannot substitute for reset-order proof.
- Observe both real workflows completing, reset ordering, and each side's
  contents. Real installed CLI calls must exercise `config.baseUrl` through
  the CLI task factory and subprocess; URL-only assertions are insufficient.
- Update `docs/worktree-browser-tests.md` and the Cypress-origin section of
  `.cursor/rules/e2e-authoring.mdc` with the supported CLI command, scope,
  paired proof command, and separate healthy allocation prerequisites.

Proof: After implementing the mode, run
`CURSOR_DEV=true nix develop -c node scripts/worktree-reset-isolation-harness.mjs --mode cli --peer <cli-checkout> --resetter <browser-checkout>`.
Record the resolved command, both exit results, reset ordering, and uncrossed
notebook assertions. Run
`CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation`, extending
the existing harness test only for the new CLI-mode contract.

Sizing: approximately 5–10 minutes active work, medium confidence, one paired
proof loop. Dual-SUT startup and Cypress runtime are external-wait exceptions.
If coordination requires additional independent implementation beats or
exceeds the active-work hard limit, refine this slice in place. A failed
paired check keeps the slice unfinished.

Stop-safe result: the existing CLI contract has repeatable concurrent proof
and accurate guidance; MCP support is still excluded at this boundary.

### 2. Await bounded MCP server child cleanup on disconnect
Type: Behavior
Status: planned

Behavior: Given a connected MCP client with a live spawned server, when the
client disconnects, that server exits before disconnect resolves.

In `e2e_test/support/mcp_client.ts`, replace
`disconnectMcpServer()`'s dead `transport.child`/`client.disconnect` checks
with `await this.client?.close()` (falling through when no client is
connected), then null out `this.client` and `this.transport`. No MCP spec is
admitted to isolated runs until slice 3.

Proof: New focused test in `e2e_test/support/mcp_client.test.ts` (`node:test`,
run via `cli/node_modules/.bin/tsx --test
e2e_test/support/mcp_client.test.ts`, through Nix) that connects through the
MCP client's public entry point using the real SDK transport and a minimal
local MCP server fixture, then disconnects and observes that the spawned
process has exited before the call resolves. A `close()` spy and nulled
fields alone do not prove child cleanup. Keep the fixture's process cleanup
bounded even when the assertion fails. Regression: the existing
(non-isolated) `mcp_services.feature`
spec (`pnpm cy:run-on-sut --spec e2e_test/features/mcp/mcp_services.feature`
against a running `pnpm sut`, through Nix) still passes across all three
scenarios. Run this pre-admission regression in an unconfigured primary
checkout; an isolated worktree cannot run this spec until slice 3.

Sizing: target approximately 5 minutes active work plus focused subprocess
and Cypress waits. Inspect the public connection seam before implementation;
if a real-child fixture needs substantial preparation, refine this slice in
place rather than substituting spy-only proof.

### 3. Admit the MCP services workflow into isolated concurrent runs
Type: Behavior
Status: planned

Behavior: Given two linked worktrees each with an existing isolated
application allocation, when each runs `pnpm sut` then
`pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature` concurrently,
each spawned MCP server searches and reads the note graph from its own
worktree's backend and notebook data, and neither run's process, result, or
teardown affects the other. The unconfigured primary checkout keeps running
the same spec against its current default origin, unchanged. Mixed selections
and non-allowlisted specs still refuse before fixture/client setup; existing
allowlisted browser and CLI single-spec runs remain supported.
An active MCP run still causes `pnpm worktree:retire --check` to refuse
reclamation via the existing busy-runner-lease veto.

Add `e2e_test/features/mcp/mcp_services.feature` to
`SUPPORTED_ISOLATED_CYPRESS_SPECS` in
`scripts/isolated-cypress-spec-selection.mjs`.
Update the same guide and Cypress-origin section touched in slice 1 to add
MCP support while preserving the already-documented CLI and browser commands.

Proof: Run the MCP spec in two isolated linked worktrees concurrently with
distinct notebook fixtures, and confirm both complete with correct, uncrossed
notebook data (targeted end-to-end check — the stable proof boundary for this
main user behavior), also confirming the already-correct `baseUrl` wiring
described in Current decisions holds under real isolation. Observe both
spawned MCP processes exit on disconnect, using slice 2's lifecycle boundary.
Record the literal Nix-prefixed commands, overlap evidence, and outcomes. Extend
`scripts/isolated-cypress.test.mjs` with the MCP spec as an accepted single
spec and an unchanged-refusal case when combined with another spec, run via
`pnpm test:browser-worktree-isolation`. Manually exercise
`pnpm worktree:retire --check` against the worktree while the run is active
to confirm the existing veto fires; this manual step is an accepted
external-wait exception — decomposing it further would only re-run the same
existing retirement mechanism this slice adds no code to.

Sizing: target approximately 5 minutes active work plus paired Cypress and
retirement-check waits. Reuse the existing concurrent proof mechanisms where
applicable; refine in place if the data/overlap proof needs independent beats.
Do not replace live data isolation proof with guard-only assertions.

## Execution constraints

All slices remain planned; this merge executes no work. The developer
explicitly authorized the combined plan and corrections-first ordering.
Do not recreate quick/090 or retain competing slice ownership there.
Preserve unrelated working-tree changes and the source seed's story anchors.

Use `CURSOR_DEV=true nix develop -c` for repository tooling; Git runs directly.
Each slice includes required Jidoka, fresh post-change-refactor agent,
coordinator selective formatting once, plan update, commit, and push when
execution is separately authorized. Target roughly 5 minutes per leaf;
scrutinize above 5 and finer-decompose above 10 active minutes. External
service/test waits do not waive that active-work limit.

Readiness: the proof owners and ordering are consistent. Refine in place if
the real-child fixture or paired-run coordination exceeds these sizing gates.

## SLICE PLAN WRITTEN
