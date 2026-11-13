# Test Optimization Candidates

## Live candidates

### e2e-batch-cold-start

- **Affected test or fixture:** the owned stack that `pnpm cy:run` starts for a session's first local batch (`scripts/e2e-runner.mjs`, `scripts/sut-services.mjs`); seen on `e2e_test/features/cli/cli_notebook_lfs.feature`.
- **Observed cost and conditions:** focused local wall time. 2026-11-01 at `ee6ecf2549`: cold first LFS batch ~4m18s vs warm under ~1 minute. 2026-11-02 at `6c045ccfd5`: first run in a fresh checkout with a warm `~/.gradle` build cache 55.0s vs warm 41.9/43.3s, 10/10 scenarios each; of the ~12.4s gap, Cypress healthy→run-start +6.8s (unattributed) and backend readiness +4.2s (Gradle first configuration plus `compileJava` FROM-CACHE, Flyway first migration ~0.7s). macOS, 16 cores, shared host load 3–20.
- **Unique protection:** a fresh owned stack with cold caches and boot on a session's first batch, with real readiness, cleanup, HMR and Cucumber proof.
- **Alternatives tried:** SEED-039#candidate-resolution measured warm readiness polling only. SEED-098#remaining-test-costs attributed stages from existing logs and found no removable stage, so no startup change was made. Flyway first migration of the per-worktree database is inherent under ADR 0007.
- **Decision or evidence needed:** a stage breakdown of the Cypress launch window (config, `setupNodeEvents`, browser launch) on a genuine first run in another freshly prepared worktree versus a warm run, and a first run with a `compileJava` build-cache miss, to show whether real compilation explains the historical ~4m18s.

### frontend-sibling-spec-setup

- **Affected test or fixture:** remaining cohesive sibling Vitest families under `frontend/tests/` (historically ~25 families of 2–3 siblings), including the BookReadingPage marking, notebookSwitch, readingPosition and base specs beside the merged visibility spec.
- **Observed cost and conditions:** retained merges historically saved ~70–250ms per removed file on the focused ordinary command. 2026-11-02 at `4ae0329ed8`: merging the mounted `AiResponse` streaming/unknown-tool pair saved ~0.2s focused (6/6 alternating pairs); merging the `aiReplyState` content/toolCalls protocol pair showed no gain. The full suite's ~10s load-following spread cannot resolve either.
- **Unique protection:** each family's distinct component behavior, harness, mock boundaries and real/fake timer requirements; per-file browser isolation stays.
- **Alternatives tried:** responsibility-based merges (346→295 files); the reading-panel pair merged 2026-11-01; the mounted reply merge kept and the protocol merge undone 2026-11-02. Do not retry the `aiReplyState` protocol pair without new evidence.
- **Decision or evidence needed:** alternating focused ordinary-command timings for each unpicked family, judging a merge only by that, not by the full suite.

### hosted-assistant-access-expiry

- **Affected test or fixture:** `e2e_test/features/mcp/assistant_account_linking.feature::A hosted SDK assistant renews access until I withdraw permission`; the wait in `e2e_test/support/hosted_assistant_client.mjs` `waitForAccessExpiry`, with the 10s lifetime set in `e2e_test/support/hosted_assistant_fixture.mjs`.
- **Observed cost and conditions:** CI case time 12.6s and 12.8s (runs 43259460645 at `a13ab0cb63` and 43254480442 at `67ad873c49`, 2026-11-12); about 11s of it is waiting for the real 10s access lifetime to pass.
- **Unique protection:** the hosted SDK assistant's first reads use its initial grant, then a real server-side expiry forces an automatic renewal before withdrawal.
- **Alternatives tried:** none tried. A shorter lifetime would save time only if connecting and the first reads still finish before expiry on slow CI runners; otherwise the first reads would silently renew or the scenario would flake. No controllable clock reaches Spring Authorization Server's token expiry.
- **Decision or evidence needed:** CI step timings of the connect and first-read steps to choose a shorter lifetime with margin, or a check that a premature renewal fails loudly.

### cli-git-round-trip-scenarios

- **Affected test or fixture:** the installed-CLI Git round-trip features under `e2e_test/features/cli/` (`cli_notebook_*`: web/local reconciliation, note and folder moves, trash, renames, clone, LFS, publish to clean clone, folder relocation, existing note edits).
- **Observed cost and conditions:** CI case times, run 43259460645 at `a13ab0cb63` (2026-11-12): 32 scenarios at 3.3–5.7s each, together ~142s (from the job logs). The CLI shard is CI's slowest E2E job (7m47s).
- **Unique protection:** real clone, pull, rebase and publish subprocess protocol behavior between the installed CLI, Git and the backend.
- **Alternatives tried:** earlier passes halved CLI E2E time and retired duplicate single-commit and single-file-rebase journeys. This pass found no fixed waits in these scenarios.
- **Decision or evidence needed:** per-step timings (CI keeps only per-feature Cucumber fragments) to separate the per-scenario CLI install, seeding, Git binding snapshot, and each CLI subprocess before choosing an experiment.
