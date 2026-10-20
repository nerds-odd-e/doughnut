# Exercise current MCP source in local E2E runs

**Source:** [Exercise current MCP source in local E2E runs](../../seeds/SEED-077-current-mcp-e2e-bundle.md#story-1)

**Identity:** SEED-077#story-1

## Goal and scope

A developer runs the normal MCP E2E feature and gets evidence about the current
checkout, including when an earlier bundle exists. Prepare current source before
connecting, surface a build failure before tool execution, and preserve existing
connection, tool behavior and teardown in local and CI runs. The seed owns the
examples. Tool semantics, CLI build policy, watch mode, shared caches and general
build orchestration are outside this delivery.

Preparation checkout:
`/Users/terryyin/git/doughnut/.worktrees/exercise-current-mcp-source-in-local-e2e-runs`.
Baseline: `18d34c42064e5a1a704ae2da958d6ac689fbf5c0`, observed 2026-10-20.
Execution consumes this landed plan in its selected checkout; repeat an
observation if that checkout changes its relevant boundary. Preparation
authorizes no implementation or Take.

## Existing solutions and decisions

- PFE: change the existing `mcpClientTasks(repoRoot).bundleMcpServer` owner.
  `common.ts` registers it and the tagged Before hook in
  `e2e_test/step_definitions/hook.ts` awaits it before connection. The MCP client
  consumes that checkout's `mcp-server/dist/mcp-server.bundle.mjs` through stdio.
  Keep this path and its existing `pnpm bundle`, working directory and subprocess
  environment. Rebuild each time the hook asks for preparation.
- The CLI's `ensureCliBundleFresh` combines source mtimes with CLI-specific Gmail
  configuration, installation and version-cache responsibilities. MCP needs none
  of that policy. Its measured build cost supports direct rebuilding; no shared
  freshness service or new cache representation is justified.
- CI's `scripts/ci/e2e-bundle-if-needed.sh` restores bundles or calls
  `pnpm bundle:all`; its matrix then uses the same feature and task. Keep those
  inputs valid, including an already-current bundle. No CI-only preparation
  branch is needed.
- Follow [ADR 0006 — Failure handling](../../../docs/adrs/0006-failure-handling-accepted.md#usage):
  propagate the build failure, retaining useful existing context. A loud failure
  needs a bounded observation here, not an additional unit test of throwing.
  Follow [ADR 0007 — Environments and isolation](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  the E2E wrapper owns disposable data and processes; no Development stack is used.
  Existing North Star topics need no change for this local adapter correction.
- The common rule is current preparation before each tagged scenario. Missing,
  older and already-current bundles all follow it. Keep implementation, regression
  proof and guidance cleanup in one Behavior slice; splitting by bundle state
  would fragment the same outcome.

## Investigation and decisive premises

All scratch source/task edits and generated observation entry points were removed,
and the original bundle was rebuilt, before the preparation records were written.
These observations establish planning premises, not implementation acceptance.

| Premise and consuming operation | Literal observation and setup in the baseline checkout | Result and consequence |
| --- | --- | --- |
| Rebuilding at the hook is cheap enough | `CURSOR_DEV=true nix develop -c python3 -` measured three `subprocess.run(['pnpm','-C','mcp-server','bundle'])` calls with `time.monotonic()` inside one Nix shell | All exit 0: 0.652, 0.072, 0.072 seconds. Includes pnpm/esbuild process cost, excludes Nix entry and dependency install. Existing dependencies were current via `./scripts/run.sh bash scripts/worktree_setup.sh`. |
| An earlier bundle can supply the previous tool behavior | `CURSOR_DEV=true nix develop -c python3 /tmp/donut-mcp-preparation-observe.py`; disposable driver compiled `e2e_test/config/.mcp-preparation-observation.ts` with esbuild, then ran it with Node. A real baseline bundle existed; the scratch source changed only `list_note_candidates`' empty-result text to `Current source observation.`. The driver called the real preparation, spawn, tool-call and disconnect tasks, with an ephemeral HTTP server returning `[]` for backend search | Existing adapter took 0.0 ms and returned `No matching notes.` despite changed source. The stdio server exited at disconnect. Reproduces the selected symptom at the adapter's consuming MCP boundary without application data mutation. |
| Unconditional preparation exercises current source, including missing output | Same command and driver, with only the task's existing-file early return temporarily deleted; then remove the generated bundle and repeat | Current response observed both times. Task cost 73.2 and 74.4 ms, exit 0; real spawned servers exited at disconnect. This discriminates freshness at the external tool result. |
| A build error surfaces before a usable earlier artifact can provide a result | Same scratch task/driver, append invalid TypeScript after a successful build | Exit 1 with esbuild syntax error and task context. No tool result; disconnect reported `pid: null`. Retain this as loud-failure evidence, not an absence regression test. |
| Existing disconnect proof is runnable | `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --test ../e2e_test/support/mcp_client.test.ts` | Exit 0, one test, 1.32 seconds overall. It uses the stdio fixture and observes child exit before disconnect resolves; it does not prove bundling or backend behavior. |
| The selected full E2E route is admitted in this worktree | `CURSOR_DEV=true nix develop -c node --input-type=module -e 'import { assertSupportedIsolatedCypressSpecs } from "./scripts/isolated-cypress-spec-selection.mjs"; console.log(JSON.stringify(assertSupportedIsolatedCypressSpecs(["e2e_test/features/mcp/mcp_services.feature"])));'` | Exit 0; neither private OpenAI nor Wikidata mock is required. The next operation starts services and mutates disposable test data; the full feature is slice 1's early probe. Preparation claims no Cypress or hosted CI pass. |

The disposable driver mocks only backend HTTP; actual source, bundler, task,
stdio server, tool dispatch and client teardown ran. Its `/tmp` script is local
observation material, not a future execution dependency. Recreate the described
fixture if its evidence is invalidated.

## Outside-in proof ownership

| Final promise / affected consumer | Owner and observable proof |
| --- | --- |
| Current source is prepared when output is missing or older | Slice 1: add a focused task-boundary regression under `e2e_test/config/`, using a temporary repository-shaped build fixture and real build execution. Call `mcpClientTasks(root).bundleMcpServer()`, execute its artifact and assert the current fixture value; change an imported source value and repeat with the earlier artifact present. The fixture supplies source, not the resulting artifact. Show the freshness assertion fails on the existing adapter and passes after the change. |
| Normal local MCP journey consumes the prepared source | Slice 1: retain `mcp_services.feature`'s search, graph and `read_note` outcomes. Before the first run, build a scratch empty-search response marker into the old bundle, then restore source. The ordinary wrapper must expose that stale marker in the existing Fiona scenario on baseline; after the fix it returns the expected current response. Revert every scratch edit before acceptance. |
| Build failure terminates preparation visibly | Slice 1: bounded observation with invalid scratch source and an earlier usable bundle through the tagged feature; expect the build error at `bundleMcpServer`. Restore source and rebuild afterward. Do not add a unit test for permitted loud failure or assertions about an obsolete skip path. |
| Already-current/CI-prepared bundle and connection/teardown remain usable | Slice 1: run the normal feature with the artifact produced by `pnpm -C mcp-server bundle`, plus the existing client disconnect test. Run the feature from a missing artifact too. Hosted CI's MCP matrix remains the CI-environment proof after execution publication. Do not treat a fixture-only client test as full feature proof. |
| Developer guidance matches automatic preparation | Slice 1: update the MCP skill's manual pre-E2E rebuild instruction to describe the owning task, while retaining the standalone build command for IDE/client use. Read any other current guidance found by the sweep below. |

The new task regression should use the existing Node test style in this directory
and real fixture data/build inputs, with no export of private freshness helpers
or internal module mocks. Use the existing `tsx` runner. Include it in an existing
appropriate package verification command used by CI so it remains executable
regression coverage; do not introduce another test framework.

## Ordered slices

### 1. Prepare current MCP source before the E2E client connects

Type: Behavior
Status: done
Proof: Task-boundary current-artifact regression, normal MCP feature with old,
missing and current bundle preconditions, bounded build-failure observation,
and retained client teardown proof as mapped above.

Execution evidence (2026-10-20, execution checkout at published claim
`32381066ad`): baseline early probe built a stale-marker bundle with source
restored — the Fiona empty-search scenario failed expecting
`No matching notes.` and observed the stale marker (6 passing, 1 failing),
proving the stale route, not infrastructure failure. After the change:
`pnpm -C cli exec tsx --test ../e2e_test/config/mcpClientTasks.test.ts ../e2e_test/support/mcp_client.test.ts`
passed 2/2 (new regression also shown failing against the baseline adapter in
a separate temporary checkout: `first-value` vs expected `second-value`);
`pnpm test:browser-worktree-isolation` passed (112 node tests + the wired tsx
test), so CI reaches the regression via `lint:all`; the normal
`pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature` passed 7/7
with a stale bundle present and again with the bundle missing. Bounded
build-failure observation with invalid scratch TypeScript and a usable bundle
failed at `bundleMcpServer` before any tool result; source was restored and
the bundle rebuilt. All scratch edits were reverted. Learning: the CI wiring
extends the pre-existing `test:browser-worktree-isolation` E2E-infra test
bucket (already host to an `e2e_test/config` test and reached via `lint:all`);
renaming that bucket is not part of this slice.

Behavior: Given current MCP source and any existing bundle state, run the normal
MCP E2E feature; preparation builds that source before connection and the client
observes its tool behavior. If building fails, preparation surfaces the error.
Keep startup/teardown and CI's existing entry point coherent with the same rule.

Early probe, before dependent implementation: execute the isolated feature
command below against the deliberately earlier response bundle, with source
already restored. This reaches the real wrapper, token setup and backend-backed
tool result. An infrastructure failure stops dependent implementation until the
proof route is repaired or the plan is reassessed; distinguish it from the
expected stale-response failure. The selection prefix and real adapter/stdio
journey passed in preparation; test-data mutation belongs to execution.

Delete the existing-file shortcut and exclusive imports/locals, extend the
stable-boundary proof and reconcile current guidance. Preserve shared CLI
utilities and standalone MCP build support. A single preparation rule handles
all examples; no intermediate behavior or preparatory Structure slice is needed.

Sizing: 7–9 minutes of implementation, focused task/client proof and cleanup;
scrutinized above the five-minute target because the fixture and verification
command wiring belong to the same observable outcome. Isolated backend startup
and the bounded repeated feature runs are the focused-test waiting exception to
the ten-minute cap. If implementation itself exceeds ten minutes, stop and refine
from the actual learning before continuing; do not split by files or test layers.

## Verification and delivery

Use the runner-owned stack in accordance with AGENTS.md and the agent map:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/mcp/mcp_services.feature
CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --test ../e2e_test/config/mcpClientTasks.test.ts ../e2e_test/support/mcp_client.test.ts
```

The task test path is the planned new regression. Verify its selected persistent
package/CI command too. The full MCP feature owns backend-backed tool semantics;
no broader backend/frontend suite is required for this adapter-only change.
If implementation changes other consumers, trace their effect and add only their
required proof. Hosted CI runs its normal matrix after authorized execution
publication; preparation starts no CI observer.

Follow AGENTS.md execution delivery: Jidoka, fresh
`dough-post-change-refactor` agent, API generation only if signatures change,
coordinator `./scripts/run.sh pnpm format:changed` once, plan evidence update,
agent commit with check-only hook, and publication/CI ownership under
`dough-execute-plan`.

## Acceptance reading for deletion

At acceptance, read the remaining task and sweep product code, tests and current
guidance for identifiers or advice used only by the deleted bundle-existence
shortcut. Remove exclusive dependents transitively. Record the literal `rg`
search and classified result here, separately from the surviving-behavior proof.

Acceptance reading (2026-10-20): `rg -n "existsSync|Rebuild the bundle before|bundlePath" e2e_test/config/mcpClientTasks.ts .agents/skills/mcp-server/SKILL.md`
returned no matches (exit 1). The refactor sweep over `e2e_test/config`,
`e2e_test/support`, `e2e_test/step_definitions`, the MCP skill,
`mcp-server/package.json` and `package.json` classified remaining hits as the
surviving spawn guard in `mcp_client.ts`, the build outfile, the hook's
`bundleMcpServer` registration and the updated guidance — no remnant of the
deleted shortcut.
Preserve the seed's dated incident and observation evidence with its original
provenance. Add no permanent absence assertion, runtime guard or checker for the
deleted shortcut.
