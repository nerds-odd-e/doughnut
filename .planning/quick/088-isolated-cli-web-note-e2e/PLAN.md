# Run one non-interactive CLI E2E workflow against the owning worktree's environment

Source: [SEED-015 Story 4](../../seeds/SEED-015-concurrent-worktree-environments.md#story-4).
Status: planned.

## Goal and scope

In an isolated linked worktree with an existing application allocation
(1a–1c, 2, 2a), run `pnpm sut` then
`pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`.
The installed CLI's clone/create/pull/publish commands must reach that
worktree's own backend and act on its own local client state (config, access
token, clone checkouts), so this spec run alongside another worktree's CLI or
browser run does not read or reset either side's data.

Add exactly this one CLI spec to the isolated-run allowlist. No other CLI spec
becomes runnable in an isolated worktree as a result of this plan; the
unconfigured primary checkout keeps its current default origin and behavior.

Excluded: every other CLI feature file (`cli_notebook_existing_note_edits`,
`cli_notebook_folder_relocation`, `cli_notebook_clone`, `cli_install_and_run`),
interactive mode, access-token, and Gmail/OAuth scenarios (already `@ignore`
and outside the active suite), MCP, and any change to the runner-lease or
retirement mechanisms themselves.

## Current decisions

- `e2e_test/config/cliEnv.ts`'s `cliEnv()` is the single choke point for the
  `DONUT_API_BASE_URL` given to every spawned CLI process — both the managed
  PTY path (`cliE2eManagedPty.ts`, used by interactive and clone/pull/publish
  commands via `cliE2eInstalledCli.ts`) import it. It currently hardcodes
  `E2E_APP_BASE_URL` instead of the isolated origin. Fixing it there covers
  install, version, clone, pull, and publish uniformly; no other spawn site
  sets this env var. `cliE2eNotebookCloneTasks.ts` only runs local `git`
  commands on the checkout and never spawns the `donut` binary.
- `scripts/isolated-cypress.mjs`'s `guardCypressNodeSetup` already computes
  the isolated origin and assigns it to `config.baseUrl` before returning
  (falling through unchanged when isolation doesn't apply, i.e. an
  unconfigured/primary checkout keeps `E2E_APP_BASE_URL`). `e2e_test/config/common.ts`
  calls `guardCypressNodeSetup(repoRoot, config, …)` and only afterward
  constructs `createCliE2ePluginTasks(repoRoot, {…})`, so `config.baseUrl` is
  already resolved and available to thread straight through — no second
  allocation lookup needed.
- Config, access-token, and clone-checkout directories already come from
  per-run `mkdtempSync(tmpdir(), …)` (`cliE2ePluginConfigDirTasks.ts`,
  `cliE2eNotebookCloneTasks.ts`); this plan makes no change there.
- `scripts/isolated-cypress-spec-selection.mjs`'s
  `SUPPORTED_ISOLATED_CYPRESS_SPEC(S)` gates every isolated run today via
  `assertSupportedIsolatedCypressSpecs`, called before the generic Cypress
  runner lease (`scripts/sut-owner.mjs`) is acquired. Any CLI spec is
  currently refused unconditionally in an isolated worktree. Adding the one
  selected spec to that list is sufficient to admit it into the existing
  lease, health-verified-owner (2a), and retirement-veto (6/6a, "busy Cypress
  runner lease") machinery unchanged — none of that machinery is
  spec-conditional beyond the allowlist gate itself.
- CLI subprocesses are already spawned synchronously and awaited to exit
  (`runInstalledCliExpectingExit` / `waitForPtyExit`), so no 2b-style
  outlives-its-parent shutdown case applies to CLI processes themselves; no
  new process-ownership mechanism is needed.

## Outside-in proof map

| Example | Observable result | Proof owner |
| --- | --- | --- |
| Two isolated worktrees each resolve a distinct backend origin for spawned CLI processes | `cliEnv()` returns the per-checkout origin, not the shared constant | Slice 1 |
| Unconfigured primary checkout | `cliEnv()` keeps returning `E2E_APP_BASE_URL`, unchanged | Slice 1 |
| Isolated worktree runs the allowlisted spec | `assertSupportedIsolatedCypressSpecs` admits it; clone/create/pull/publish reach that worktree's own backend and notebook data | Slice 2 |
| Isolated worktree runs any other CLI spec | Refused before fixture/client setup, exactly as today | Slice 2 (regression case, existing behavior) |
| `pnpm worktree:retire --check` during an active allowlisted-spec run | Refuses reclamation via the existing "busy Cypress runner lease" veto | Slice 2 (existing mechanism, exercised against a CLI run) |

## Ordered slices

### 1. Thread the isolated backend origin into spawned CLI processes
Type: Structure
Status: planned

Internal change: Give `cliEnv()` an optional base-URL override parameter,
defaulting to today's `E2E_APP_BASE_URL` when omitted. In
`e2e_test/config/common.ts`, read the already-resolved `config.baseUrl` after
`guardCypressNodeSetup` returns and pass it into
`createCliE2ePluginTasks(repoRoot, { …, appBaseUrl: config.baseUrl })`; thread
that value down to every `cliEnv(...)` call site in `cliE2eInstalledCli.ts`
and `cliE2eManagedPty.ts`. This changes no observable behavior yet — no CLI
spec is admitted to isolated runs until Slice 2 — and immediately enables it.

Proof: The ordinary (non-isolated) `e2e_test/features/cli/**` CI shard stays
green (`pnpm cy:run --spec 'e2e_test/features/cli/**'`), and
`pnpm test:browser-worktree-isolation` stays green. No behavior change is
observable outside CLI env resolution.

### 2. Admit the web-created-note CLI workflow into isolated concurrent runs
Type: Behavior
Status: planned

Behavior: Given two linked worktrees each with an existing isolated
application allocation, when each runs `pnpm sut` then
`pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`
concurrently, each installed CLI clones, creates, pulls, and publishes against
its own worktree's backend and notebook data, and neither run's result,
fixtures, or process teardown affects the other. The unconfigured primary
checkout keeps running the same spec unchanged. Any other CLI spec in an
isolated worktree still refuses before fixture/client setup. An active run
still causes `pnpm worktree:retire --check` to refuse reclamation via the
existing busy-runner-lease veto.

Add `e2e_test/features/cli/cli_notebook_web_created_note.feature` to
`SUPPORTED_ISOLATED_CYPRESS_SPEC(S)` in
`scripts/isolated-cypress-spec-selection.mjs`.

Proof: Run the spec in one isolated linked worktree while a peer worktree's
SUT/Cypress run is active, and confirm both complete with correct, uncrossed
notebook data (targeted end-to-end check — the stable proof boundary for this
main user behavior). Extend `scripts/isolated-cypress.test.mjs` with the
allowlist addition and a two-allocation case asserting distinct resolved
origins and unchanged refusal for an unlisted CLI spec, run via
`pnpm test:browser-worktree-isolation`. Manually exercise
`pnpm worktree:retire --check` against the worktree while the run is active
to confirm the existing veto fires; this manual step is an accepted
external-wait exception — decomposing it further would only re-run the same
existing retirement mechanism this slice adds no code to.

## SLICE PLAN WRITTEN
