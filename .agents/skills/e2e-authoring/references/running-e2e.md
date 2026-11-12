# Running E2E tests

For browser authoring, use [E2E authoring](../SKILL.md).

## Environment Assumption

The `pnpm cy:run` wrapper owns its stack and compiles current backend source once per batch, without a continuous backend compiler. Local batches retain frontend HMR; backend edits need a new invocation. Use `pnpm cy:open` for an interactive session with backend reload and frontend HMR. Required mocks are owned by the selected invocation.

If services are not running, suggest running `pnpm cy:run --spec <feature>` in a separate terminal.

## Cypress Origin

Primary unconfigured checkouts and CI use `http://localhost:5173` as the Cypress
app origin through the local load balancer. A linked worktree (or a primary
with `.worktree.local.json`) uses that checkout's isolated origin
`http://127.0.0.1:<lbListenPort>` — see [worktree browser tests](../../../../docs/worktree-browser-tests.md). Isolated
Cypress accepts one or more selections from the assessed active feature set
declared in `scripts/isolated-cypress-spec-selection.mjs` (application-only
active specs plus the active CLI, MCP, OpenAI-mock, and Wikidata-mock focused
specs) per run; a new feature file must be added to that set (e.g.
`APPLICATION_ONLY_ACTIVE_SPECS` in `scripts/isolated-cypress-active-specs.mjs`) before an
isolated run accepts it. The CLI command is scoped to the assessed active CLI workflows.
The MCP command is scoped to that one search/graph feature. OpenAI-mock specs
start a runner-owned private OpenAI Mountebank; Wikidata-mock specs start a
runner-owned private Wikidata Mountebank; application-only, CLI, and MCP specs
do not require Mountebank. Each isolated checkout needs its own healthy E2E
allocation. Paired CLI-vs-browser reset proof:

```bash
CURSOR_DEV=true nix develop -c node scripts/worktree-reset-isolation-harness.mjs --mode cli --peer <cli-checkout> --resetter <browser-checkout>
```

CI vs `pnpm cy:run`, `wait-on`, `NO_PROXY`, and env vars are documented in
[production and local environment](../../../../docs/gcp/prod_env.md), Local dev / Cypress under section 6.

## Run E2E Tests

Agents should default to `pnpm cypress run --spec` with one or more `.feature` paths tied to the changed capability. Add adjacent specs only when regression risk warrants it. Do not run bare `pnpm cypress run`, `pnpm verify`, or the whole suite unless the user explicitly asks for a full E2E run or the instructions require reproducing CI.

```bash
CURSOR_DEV=true nix develop -c pnpm cypress run --spec e2e_test/features/ai_generated_recall_questions/question_contest.feature
```

Important notes:

- Do not use `cypress run -- --spec`; the empty `--` causes the `--spec` parameter to be ignored.
- The default tag filter is `expose.tags` in `e2e_test/config/ci.ts`, for example `not @ignore`. Override per raw `pnpm cypress run` with `--expose tags='...'` (`-x`; the `pnpm cy:run` wrapper drops it), not `--env tags=...` (ignored by `@badeball/cypress-cucumber-preprocessor` v27 on Cypress >= 15.17).
- `@skipOptimizationDueToKnownNecessarySlowness` is Donut's profile-only
  exclusion for known-necessary slowness. `dough-test-optimization` profiles
  exclude it as described under "Test-optimization profiling" below.
  Normal CI/dev runs still execute these scenarios. Do not add this tag without
  developer review, except when an explicit `dough-test-optimization --resolve`
  pass makes an evident exclusion under the public skill's candidate-resolution
  rules.
- `@focus` and `@only` are local debugging aids only. Do not commit them; CI runs `./scripts/check_focus_tags.sh` on `.feature` files to block `@focus`, and `@only` should be treated the same.
- If any node in a feature file is tagged `@focus` or `@only`, `@badeball/cypress-cucumber-preprocessor` filters scenarios in that file to `@focus or @only` only.

### Test-optimization profiling

For per-scenario timing, run the selected feature scope through the owning
wrapper and tee its output to a local file:

```bash
CURSOR_DEV=true nix develop -c pnpm cy:run --spec '<feature,feature,...>' 2>&1 | tee /tmp/donut-e2e-profile.log
```

- The wrapper forwards only `--spec` (and `--browser`) to Cypress
  (`scripts/e2e-cypress-process.mjs`); `--reporter` and `--expose tags=...`
  are dropped. Apply the profile-only exclusion by leaving
  `@skipOptimizationDueToKnownNecessarySlowness` features out of the spec list,
  and subtract excluded scenarios inside a selected feature from the profile.
- Per-scenario times are the spec reporter's `✓ <scenario> (<N>ms)` lines;
  associate them with the preceding `Running: <feature>` line.
- An isolated checkout (a linked worktree, or a primary with
  `.worktree.local.json`) refuses glob spec selections; list the features
  comma-separated. If mirroring CI, also leave out `@wip` features.

This exclusion is profiling-only and must not narrow normal verification.

## Debugging

Use the canonical log-tail command for backend E2E log inspection:

```bash
CURSOR_DEV=true nix develop -c pnpm logs:tail backend-e2e
```

Frontend Vite checker terminal errors still matter even though the browser overlay is disabled for E2E stability. For canonical lint and format commands, use the `linting_formating` skill.
