# Deliberately retain Cucumber proof

**Identity:** SEED-099#deliberate-cucumber-proof
**Source:** [Deliberate proof recording](../../seeds/SEED-099-deliberate-cucumber-proof.md#deliberate-proof-recording)
**Source review:** [Independent commitments](source-review.md)

## Outcome and scope

Ordinary tests leave retained proof untouched; an explicit recording option refreshes compact successful proof through Donut's existing Cypress Messages boundary. Follow [the proof contract](../../../.agents/skills/dough-test-optimization/references/cucumber-proof.md), including legacy latest-first reduction and unsuccessful timestamp ties. Retained proof and CI remain independent observations. No other adapters or deployment.

## Execution context

- Story Branch Mode; owned worktree `/Users/terryyin/git/doughnut/.worktrees/codex-deliberate-cucumber-proof`, branch `codex/deliberate-cucumber-proof`, created for this story.
- Originating/default checkout `/Users/terryyin/git/doughnut`; remote `origin`, trunk `refs/heads/main`; execution increments target `refs/heads/codex/deliberate-cucumber-proof`.
- Start `d43e39016a0d5941dfe0d4f86d41a1015a1e1737`; accepted claim `b34d5c232bc41b8be1ae08d939302ed0a0ea4d58`; publisher `codex-deliberate-cucumber-proof-20261102`; agent Yui-chan.
- Checkout setup passed `./scripts/run.sh bash scripts/worktree_setup.sh`; Cypress package and binary 16.2.1 verified here with `./scripts/run.sh pnpm exec cypress version`.
- CI: GitHub Actions `ci.yml`, workflow 5208694, `donut CI`; observer coordinator `codex-deliberate-proof-20261102`, directory `/tmp/dough-ci-501/watch-sZBfTx`, PID 99644, stream cell `g91:24`, session 57877, execution branch target. Trunk claim is initially unobserved. Installation CI run 41420017308 for d43e39016a remains owned through its final result.
- Ordinary independent refactor, selective formatter once, check-only commit hook, publication, retrospective, wrap-up, final trunk CI and safe resource retirement are required. Replanning within this authorized scope is allowed.
- Failure policy: [Open Dough failing and flaky tests](/Users/terryyin/git/open-dough/AGENTS.md#failing-and-flaky-tests); diagnosis does not waive repair responsibility.

## Existing solution and decisive premises

Research in this checkout found one proof pipeline: `common.ts` → `cucumberProof.mjs` → `cucumberProofRecorder.mjs` → `cucumberProofStore.mjs`. Both wrapper `ci.ts` and terminal `cypress.config.ts` use it. There is no Cucumber.js producer to migrate; its package supplies types. Change this owner, add no parallel recorder.

Existing scenarios preserve run-start SHA, tested feature blob, retry attempts/durations, parsed outline identity, final hook results, scoped features and immediate persistence. Store locking and atomic writes already protect checkout-local writers. Keep these responsibilities in their owners; replace run filenames with one snapshot. ADR 0006 permits best-effort recording without disturbing results; ADR 0007 owns isolated test resources. North Star product topics do not constrain this local tooling migration.

Baseline in this worktree: `./scripts/run.sh node --test e2e_test/config/cucumberProofRecorder.test.mjs e2e_test/config/cucumberProofLifecycle.test.mjs e2e_test/config/cucumberProofProcess.test.mjs` passed 20/20 at claim revision. With no recording option set these tests observe generated files, demonstrating the implicit-write behavior being migrated. Six committed legacy files contain 43 observations. New contract tests must discriminate before implementation; existing passing assertions are retained only where their promise survives.

## Proof ownership

| Promise and trigger | Owner | Observable proof |
| --- | --- | --- |
| Normal passing/failing terminal and wrapper-consumed adapter runs leave absent/existing proof and locks untouched | 1 | Real Cypress fixture runs with default option, full filesystem/byte snapshots, normal verdict and output |
| Explicit option uses identical tests, selection, reporter, diagnostics, exit and owner cleanup | 1 | Existing real Cypress comparator extended to explicit opt-in, focused/tagged/dry/report modes |
| Each Gherkin Intent has one actionable generation instruction; normal commands/CI/profiling stay ordinary | 1 | Configuration assertion, command tracing and documentation reading |
| Completed explicit attempts update only their identity, preserving unexecuted/newer proof and final retry durations | 2 | Recorder/lifecycle/process suite and focused real Cypress pass → failure → recovery with unrelated retained proof |
| Stable atomic concurrent snapshots and latest-first legacy reduction, including failure ties | 2 | Repeated/interleaved/interrupted process refresh; nested legacy adoption fixtures in both tie orders |
| Old committed proof stays readable and is compacted during deliberate generation | 2 | One explicit root generation command, compare retained identity reduction before/after with actual executed refresh, preserve provenance |
| All required validation, publication, CI and complete owned-resource retirement | Closure | Ordinary execution and wrap-up receipts; installation and final migration CI |

## Ordered slices

### Slice 1 — Enable proof recording explicitly

Type: Behavior
Status: planned

Pre-condition: an ordinary terminal Cypress run shares configuration with deliberate proof generation.
Trigger: run tests with recording omitted or explicitly set to `RECORD_CUCUMBER_PROOF=1`.
Result: only the explicit run touches proof; both paths keep the selected tests, output, verdict and ownership. Gate before recorder discovery, proof I/O or Messages activation. Cover the existing pipeline's wrapper and raw terminal consumers. Configure the single Gherkin `proofGeneration.command` with root wrapper and focused feature; document selection and deliberate committing. Update implicated retry fixture commentary without changing its behavior.

Proof: add discriminating normal passing/failing checks with absent and preexisting proof, compare enabled output/exit to normal, and run `./scripts/run.sh pnpm test:cucumber-proof` plus the event composer suite and `./scripts/run.sh pnpm cy:lint`. Keep focused/tagged/dry/report/retry/hook/scoped behavior. All-skip does not claim execution.

Sizing: target 5–10 minutes implementation plus serial real-Cypress proof; the existing fixture starts many real browsers, so the proof loop may exceed 10 minutes. This bounded compatibility loop is kept cohesive rather than split by test modes. Slice 2 replaces the accepted interim run-file layout; no accumulation is claimed final.

### Slice 2 — Refresh one maintained successful snapshot

Type: Behavior
Status: planned

Pre-condition: recording is explicitly enabled and legacy or maintained proof may exist.
Trigger: a selected scenario finishes its last executed attempt.
Result: under existing checkout lock, reread all proof, reduce legacy observations latest-first with unsuccessful ties winning, apply the completed observation, retain passes in stable identity order, atomically update `latest.json` and delete consumed files or empty snapshots. Preserve unexecuted/newer identities and immediate completed progress. Do not persist failure markers or commit automatically.

Proof: recorder/lifecycle/process tests cover old and new layouts, repeated refresh, both timestamp tie orders, nested files, concurrency/interruption, non-runs, final failure and restoration; real Cypress focused refresh preserves an unrelated identity and normal output/exit. Run the affected full proof suite, composer suite and E2E lint. Deliberately refresh note creation using the configured root command, retaining provenance and verifying readable compact proof plus consumed-file cleanup in the same commit.

Sizing: target 5–10 minutes implementation plus serial proof. Real application proof is one unpaid focused owned-stack batch; probe its setup/selection before launch, stop dependent work on unavailable allocation, and keep this result distinct from the fixture runner's observations.

## Current decisions

- Recording option is exactly `RECORD_CUCUMBER_PROOF=1`; no default, CI or optimization command enables it.
- Snapshot schema remains version 1, successful observations only, preserving original timestamps and source versions during adoption.
- Filesystem no-write assertions establish the explicit ongoing opt-in contract requested by Terry. The one-time product sweep for obsolete automatic-recording wording and run filename allocation is an acceptance reading, separate from retained behavioral proof.
- Expensive proof runs serially on Terry's Mac. Other Open Dough Playwright work is active; leave it untouched. Before launching real Cypress, inspect memory/load; use one fixture process or one application stack at a time. Reconsider on contention, failure or changed workload; each command owner follows its terminal result.

## Concern review

Refinement was not needed: the two behavior slices separate explicit authorization from maintained storage, each with one evaluable proof loop. Remaining concerns: none; application allocation is an explicitly bounded early probe before its dependent live test run.

## Learnings

None beyond the recorded existing-solution assessment.
