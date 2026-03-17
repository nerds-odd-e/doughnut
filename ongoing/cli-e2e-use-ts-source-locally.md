# CLI E2E: Use TypeScript Source Locally, Bundled in CI

## Goal

- **Local development**: CLI e2e tests run the TypeScript source directly (no build step).
- **GitHub Actions / CI**: CLI e2e tests run the bundled output (`cli/dist/doughnut-cli.bundle.mjs`).
- **Installation tests**: Always use the bundled/installed binary (no change).

## Rationale for bundled in CI

**Speed**: Avoid compiling the CLI per scenario. The bundle is built **once** alongside the frontend (`pnpm bundle:all`), same as frontend build frequency. No per-scenario or per-spec compilation.

## CI build flow (no per-scenario compilation)

The E2E job runs `build: pnpm bundle:all` **once** before Cypress starts. That builds:

1. MCP server  
2. CLI (`cli:bundle-and-copy` → `cli/dist/doughnut-cli.bundle.mjs`)  
3. Frontend  

The CLI bundle exists before any scenario runs. `getCliRunConfig` in CI path must only return the path—never invoke `pnpm cli:bundle` or `tsx` (which would compile per run). No spawn of bundler or transpiler.

## Current Behavior

All CLI e2e tests (except `runInstalledCli`) use `ensureCliBundleExists()` → `cli/dist/doughnut-cli.bundle.mjs`. Running `pnpm cli:bundle` is required before e2e tests pass.

## Scope

### Tasks to change (use TS locally, bundle in CI)

| Task                     | Used by |
|--------------------------|---------|
| `runCliDirectWithInput`  | recall, install_and_run (non-install scenarios) |
| `runCliDirectWithInputAndPty` | recall (ESC, arrow scenarios), access_token (ESC cancel) |
| `runCliDirectWithArgs`   | access_token, install_and_run (-c, version) |
| `runCliDirectWithGmailAdd` | gmail |
| `runCliDirectWithLastEmail` | gmail |

### Tasks unchanged (always bundled)

| Task             | Reason |
|------------------|--------|
| `runInstalledCli`| Uses `doughnutPath` from install script; install downloads bundled binary |
| `installCli`     | Returns path to installed binary |
| `bundleAndCopyCli*` | Backend must serve bundled CLI for install to work |

### Feature files

- `cli_recall.feature` – all scenarios use direct run (TS locally)
- `cli_access_token.feature` – all use direct run (TS locally)
- `cli_gmail.feature` – all use direct run (TS locally)
- `cli_install_and_run.feature` – mix: install scenarios use `runInstalledCli` (bundled), others use direct run (TS locally)

## Phases

### Phase 1: Add `getCliRunConfig()` helper ✅

- Add `getCliRunConfig(repoRoot: string): { command: string; baseArgs: string[] }`.
- Logic:
  - **CI**: Return `{ command: process.execPath, baseArgs: [join(repoRoot, CLI_BUNDLE_PATH)] }`. Do **not** call `ensureCliBundleExists`—the bundle is pre-built by `bundle:all` before Cypress. No per-scenario compilation.
  - **Local**: Return `{ command: 'pnpm', baseArgs: ['-C', join(repoRoot, 'cli'), 'exec', 'tsx', 'src/index.ts'] }`.
- Ensure `tsx` is available in `cli` devDependencies (already present).

**Deliverable**: Helper in place, no behavior change yet.

---

### Phase 2: Update `runCliDirectWithInput` ✅

- Replace `bundlePath` usage with `getCliRunConfig(repoRoot)`.
- Change spawn to `spawn(config.command, [...config.baseArgs], ...)` for pipe mode.
- Run `cli_recall` and `cli_install_and_run` (non-install) locally to verify.

**Deliverable**: `runCliDirectWithInput` uses TS locally, bundle in CI.

---

### Phase 3: Update `runCliDirectWithArgs` ✅

- Same pattern: `getCliRunConfig` → `spawn(config.command, [...config.baseArgs, ...args], ...)`.
- Verify `cli_access_token` and `cli_install_and_run` (-c, version) locally and in CI.

**Deliverable**: `runCliDirectWithArgs` uses TS locally.

---

### Phase 4: Update `cliPtyRunner` and `runCliDirectWithInputAndPty` ✅

- `cliPtyRunner` previously did `pty.spawn(process.execPath, [opts.executablePath, ...(opts.args ?? [])])`.
- Extend options to accept either:
  - `{ executablePath, args? }` (current, for bundle), or
  - `{ command, args }` (for pnpm + tsx).
- Simpler alternative: keep `executablePath` as the program and `args` as full argument list. For local, pass `executablePath: 'pnpm'` and `args: ['-C', 'cli', 'exec', 'tsx', 'src/index.ts']`.
- Update `runCliDirectWithInputAndPty` to use `getCliRunConfig` and pass `command` + `baseArgs` + input-related args to the runner.
- Verify recall ESC/arrow scenarios and access_token ESC cancel locally (with `pnpm sut` running).

**Deliverable**: PTY flows use TS locally.

---

### Phase 5: Update `runCliDirectWithGmailAdd` and `runCliDirectWithLastEmail`

- Apply the same `getCliRunConfig` pattern to both tasks.
- Verify `cli_gmail` locally and in CI.

**Deliverable**: Gmail scenarios use TS locally.

---

### Phase 6: Cleanup and docs

- Remove any redundant bundle checks for local-only flows.
- Document in `CLAUDE.md` or `e2e_test` README: “CLI e2e tests run TS source locally, bundled in CI.”
- Add `CI=1` in GitHub Actions env if not already set (usually automatic).

---

## Implementation notes

- **CI detection**: Use `process.env.CI === '1'` (standard for GitHub Actions, CircleCI, etc.).
- **CI path**: Never spawn `pnpm cli:bundle` or `tsx`—only `node path/to/bundle.mjs`. Bundle built once by `bundle:all`.
- **Local path**: `pnpm -C cli exec tsx src/index.ts` so `tsx` is resolved from `cli` package.
- **cwd**: For `pnpm -C cli`, `-C` takes an absolute path; use `join(repoRoot, 'cli')`.
- **Path resolution**: `repoRoot` is `path.resolve(__dirname, '..', '..')` in common.ts.

## Out of scope

- Changing `runInstalledCli` or install flows.
- Changing how the bundled CLI is built or served for install.
- Changing Cloud VM e2e setup (cloud-agent-setup already sets `CI=1` for pipe fallback; same env can be used for “use bundle”).
