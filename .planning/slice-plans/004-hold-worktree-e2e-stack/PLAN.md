# Hold a worktree's E2E stack for observing unmerged branch code

## Source

- Story: [Observe unmerged branch code against real services from an execution worktree](../../seeds/SEED-069-agent-development-tooling.md#observe-branch-code-against-real-services)
- Identity: SEED-069#observe-branch-code-against-real-services
- Provenance: the story's Instances table links the four recorded executions
  (DD-161 and the Donut-side occurrences of ODF-190).

## Goal and scope

A developer or executing agent in a linked worktree runs one command to get its
branch code as a running app, observes it by browser, CLI, or HTTP, and
interrupts the command to stop it. The primary checkout and the Development
stack are not touched.

- `pnpm e2e:hold` starts this checkout's own disposable E2E stack, prints the
  browser origin, and keeps the stack up until interrupted.
- The held stack has no OpenAI token unless `--paid-openai` is given.
- The seeded accounts can sign in as soon as the command reports ready.
- `.agents/agent-map.md` and `docs/worktree-browser-tests.md` name the command,
  the paid option's authorization rule, and CI as the route for real-service
  Cypress specs on branch code.

Excluded: running real-service Cypress specs in a linked worktree; a
Development stack in a linked worktree; driving a real browser; rebuilding
`.venv-mineru`; shared Open Dough planning guidance (ODF-190).

Considered and left out: a separate stop command (interrupt is the stop, as
for `cy:open`); a detached start (commit `8d1d77fb39` removed those so each
stack has one owning command); seeding extra notebooks.

## Architecture

- Existing solution reused: `runOwnedE2eInvocation` in
  `scripts/e2e-owned-invocation.mjs`, through a third entry in
  `scripts/e2e-runner.mjs` beside `runE2eBatch` and `runE2eInteractive`. It
  already owns provisioning, readiness, owner claim, service-exit handling, and
  shutdown. The hold replaces only the Cypress step with a wait for
  cancellation. No second lifecycle, lock, or port logic is added.
- Seeding reuses the product's own
  `POST /api/testability/clean_db_and_reset_testability_settings` on the held
  origin. It is the same call each Cypress scenario starts with.
- [ADR 0007](../../../docs/adrs/0007-environments-and-isolation-accepted.md):
  the held stack is the worktree's own E2E environment (its database and
  ports), disposable and owned by the command. Nothing here touches
  Development. No North Star topic governs this work.
- [ADR 0006](../../../docs/adrs/0006-failure-handling-accepted.md): a failed
  seed or a service that exits ends the hold with a nonzero outcome; nothing is
  swallowed.

## Outside-in proof

| Promise | Slice | Proof |
| --- | --- | --- |
| The command keeps the owned stack up until interrupted, then stops every owned process and releases ownership; a clean interrupt exits 0 | 1 | New cases file beside `scripts/e2e-runner-interactive-lifetime.cases.mjs`, using the same stand-in owned tree: alive while held, gone after cancel, owner lock removed, outcome 0 |
| A service that exits during the hold ends it with a nonzero outcome | 1 | Case in the same file, following `scripts/e2e-runner-service-exit.cases.mjs` |
| From a linked worktree the real command serves branch code and stops cleanly | 1 | One real run in the execution worktree: `GET <origin>/` is 200 while held; after Ctrl-C the origin refuses connections and `pnpm worktree:retire --check` reports an idle snapshot |
| Without the option the stack's services have no `OPENAI_API_TOKEN`, even when the shell has one | 2 | Case asserting the environment of the spawned SUT services (the `spawnFn` seam of `scripts/sut-start-spawn.mjs`) |
| With `--paid-openai` the shell's token reaches the stack's services | 2 | Case at the same seam. No real paid call is part of this proof |
| An AI feature in the default held app makes no OpenAI call | 2 | Real run without the option, after one manual reset `POST` (slice 3 removes that step): `curl -u old_learner:password <origin>/api/ai/available-gpt-models` answers with "OpenAI is not available (no API key configured)" |
| Seeded accounts sign in once the command reports ready | 3 | Case: the hold posts the reset to the held origin after readiness and before reporting ready (local stand-in HTTP listener). Real run: `curl -u old_learner:password <origin>/api/user/current-user-info` returns a non-null `user` with no manual step |
| A failed seed ends the hold nonzero and stops the stack | 3 | Case: the stand-in listener answers 500 |
| The CLI reaches the held app | 4 | Real run: a CLI command with `DONUT_API_BASE_URL=<origin>` gets an answer from the held app, not "Donut service is not available" |
| Guidance names the hold command, the paid rule, and CI for real-service specs | 4 | Read `.agents/agent-map.md` "Development vs E2E" and `docs/worktree-browser-tests.md` |
| Linked worktrees still refuse Development and real-service specs | all | Existing `scripts/dev-start.test.mjs` and `scripts/isolated-cypress.test.mjs` stay green, unchanged |

Commands (run from the repository root of the execution worktree):

- Focused: `CURSOR_DEV=true nix develop -c node --test scripts/e2e-runner.test.mjs`
- Group: `CURSOR_DEV=true nix develop -c pnpm test:browser-worktree-isolation`
  (and `pnpm test:sut-start` when `scripts/sut-start-spawn.mjs` changes)
- Real run: `CURSOR_DEV=true nix develop -c pnpm e2e:hold` in the background,
  wait for the ready line, observe, then send SIGINT to the node process.

The real runs need local MySQL on 3309 and take about one minute to become
ready. They are unpaid: none uses `--paid-openai`.

## Decisive premises

| Premise | Consumed by | Observation | Result |
| --- | --- | --- | --- |
| The owned invocation can hold a linked worktree's stack with something other than Cypress as the session | Slice 1 approach | 2026-10-05, preparation worktree: `runE2eInteractive` with a waiting child as `spawnCypress` provisioned `doughnut_e2e_wt_…`, printed `Browser origin: http://127.0.0.1:56055`, was healthy after 7 polls (about one minute), served `/` and `/api/healthcheck` with 200 | Holds |
| Cancellation stops the whole owned tree | Slice 1 | Same run: SIGINT to the node process; the origin then refused connections, no checkout process remained, `pnpm worktree:retire --check` reported an idle snapshot | Holds. The existing cancel path returns 1; slice 1 makes a clean interrupt return 0 for the hold |
| The stack's services take their environment from `process.env`, not from the runner's `env` option | Slice 2 approach | Read `scripts/sut-start-spawn.mjs:51-52`: `{ ...process.env, SUT_LOG_FILE, ...ownerEnv }` | Holds; the token must be withheld where the services are spawned or from the command's own environment |
| The `e2e` profile reads the token from `OPENAI_API_TOKEN` and defaults to the real OpenAI URL | Slice 2 | Read `backend/src/main/resources/application.yml` (`token: ${OPENAI_API_TOKEN:}`) and `TestabilitySettings.defaultServiceUrls`; the owner's plain shell has the variable set | Holds |
| With an empty token every OpenAI operation fails before any network call | Slice 2 proof | Read `OpenAiApiHandler`: each of its eight public operations calls `assertOpenAiAvailable()` first, which throws "OpenAI is not available (no API key configured)" on an empty token; `grep` shows the client is used only there and in `OpenAiApiConfig` | Holds by reading; slice 2's real run observes it through the app |
| A fresh held database has no users, and the testability reset seeds them | Slice 3 | Same run: `current-user-info` for `old_learner` returned `"user":null`; after `POST /api/testability/clean_db_and_reset_testability_settings` (200) it returned the `Old Learner` user | Holds |
| The reset is reachable on the held origin without a Cypress lease | Slice 3 | Same run: the POST above came from `curl` with no runner lease | Holds |
| The CLI takes its server from `DONUT_API_BASE_URL` | Slice 4 | Read `cli/src/main.ts:10` and `e2e_test/config/cliEnv.ts:10`; the isolated CLI specs already point spawned CLIs at the worktree origin this way | Holds |
| CI runs real-service specs on pushed branches | Slice 4 guidance | Read `.github/workflows/ci.yml`: `push` on `"**"`, `OPENAI_API_TOKEN` from secrets; the 2026-10-04 instance got its proof from the CI shard | Holds |

The probe's database was dropped with `pnpm worktree:retire`. That left a
retirement marker in the preparation worktree, which now refuses E2E starts;
execution uses its own worktree.

## Current decisions

- Command name `pnpm e2e:hold`; paid option `--paid-openai`. Owner decisions
  recorded in the story: held E2E stack is the route, paid calls are off unless
  opted in, CI is the named route for real-service specs.
- A clean interrupt is the hold's normal end and exits 0. A service exit,
  failed seed, or failed cleanup exits nonzero.
- The reset also sets testability defaults (first-choice randomizer, GitHub
  stand-in, feature toggle off). The guidance says so in one line; the hold
  does not undo them.
- The command is not restricted to linked worktrees. In an unconfigured
  primary checkout it holds the shared E2E defaults, as `cy:open` does. No
  test or guidance is added for that case.

## Slices

### 1. Hold this checkout's E2E stack until interrupted
Type: Behavior
Status: planned
Proof: new hold cases in the runner test group; one real run from the execution worktree (rows 1–3 above).

Behavior: a linked worktree with MySQL up → `pnpm e2e:hold` → the worktree's
E2E stack starts, the browser origin and a ready line are printed, and the
stack stays up; on SIGINT or SIGTERM every owned process stops, ownership is
released, and the command exits 0. If a required service exits first, the
command stops the rest and exits nonzero.

Interim: until slice 2 the held stack inherits the shell's OpenAI token. The
command is not named in any guidance until slice 4.

### 2. Keep paid OpenAI calls off unless `--paid-openai` is given
Type: Behavior
Status: planned
Proof: two cases at the SUT spawn seam; one unpaid real run (rows 4–6 above).

Behavior: a shell with `OPENAI_API_TOKEN` set → `pnpm e2e:hold` → the stack's
services run without the token, and an AI request in the held app answers that
OpenAI is not available. → `pnpm e2e:hold --paid-openai` → the services run
with the shell's token.

### 3. Seeded accounts can sign in when the hold reports ready
Type: Behavior
Status: planned
Proof: two cases with a stand-in HTTP listener; one real run (rows 7–8 above).

Behavior: a worktree whose E2E database is fresh → `pnpm e2e:hold` reports
ready → `old_learner` / `password` signs in with no further step. If the reset
fails, the command stops the stack and exits nonzero with the failure.

### 4. Guidance names the routes for observing branch code
Type: Behavior
Status: planned
Proof: one real CLI run against a held origin; reading the two documents (rows 9–10 above).

Behavior: an agent in a linked worktree reads `.agents/agent-map.md`
"Development vs E2E" → it finds `pnpm e2e:hold` as the route for manual, CLI,
and real-service observation of branch code; that `--paid-openai` needs the
owner's authorization each time; that the CLI is pointed with
`DONUT_API_BASE_URL=<origin>`; and that real-service Cypress specs on branch
code run in CI after a push. `docs/worktree-browser-tests.md` describes the
hold command beside `cy:run` and `cy:open`, including the reset's testability
defaults.

## Learnings

None yet.
