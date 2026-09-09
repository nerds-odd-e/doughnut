# Run one non-interactive CLI E2E workflow against the owning worktree's environment

Source: [SEED-015 Story 4](../../seeds/SEED-015-concurrent-worktree-environments.md#story-4).
Status: done.

## Outcome

Every spawned CLI process (install, version, clone, pull, publish) resolves its
backend origin through `cliEnv()`'s new `appBaseUrl` override, threaded from
Cypress's already-resolved `config.baseUrl` — so a linked worktree's CLI
processes reach that worktree's own backend rather than the shared
`E2E_APP_BASE_URL` constant. `cli_notebook_web_created_note.feature` is now
allowlisted for isolated worktree runs
(`SUPPORTED_ISOLATED_CYPRESS_SPEC(S)`); every other CLI spec still refuses
before fixture/client setup, and the unconfigured primary checkout is
unaffected (`guardCypressNodeSetup` returns before touching `config.baseUrl`
when isolation doesn't apply).

## Completed slices

1. Thread the isolated backend origin into spawned CLI processes — done.
   Proof: `pnpm test:browser-worktree-isolation` passed. The plan's literal
   `pnpm cy:run --spec 'e2e_test/features/cli/**'` proof cannot run from
   inside any linked git worktree (isolation applies unconditionally there,
   refusing any non-allowlisted spec before this slice's code runs — a
   pre-existing, diff-independent property); verified equivalence by
   inspection instead (`guardCypressNodeSetup` no-ops when isolation doesn't
   apply; `cliEnv()`'s new parameter defaults to the exact prior value). CI's
   plain-checkout run of this shard on push to `main` is the authoritative
   confirmation.
2. Admit the web-created-note CLI workflow into isolated concurrent runs — done.
   Proof: `pnpm test:browser-worktree-isolation` passed (incl. a two-isolated-
   checkout case asserting distinct resolved origins and unchanged refusal
   for an unlisted CLI spec, in `scripts/isolated-cypress-cli-spec.test.mjs`).
   Live: `pnpm cy:run --spec e2e_test/features/cli/cli_notebook_web_created_note.feature`
   passed (4/4) against this worktree's own isolated SUT allocation; while
   that run was active, `pnpm worktree:retire --check` manually confirmed
   refusal via the existing `busy Cypress runner lease` veto.
