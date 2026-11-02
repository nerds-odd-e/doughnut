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

Baseline in this worktree: `./scripts/run.sh node --test e2e_test/config/cucumberProofRecorder.test.mjs e2e_test/config/cucumberProofLifecycle.test.mjs e2e_test/config/cucumberProofProcess.test.mjs` passed 20/20 at claim revision. With no recording option set these tests observe generated files, demonstrating the implicit-write behavior being migrated. Six committed legacy files contain 43 observations for 23 identities; all latest observations passed. New contract tests must discriminate before implementation; existing passing assertions are retained only where their promise survives.

The actual isolated selection consumer `assertSupportedIsolatedCypressSpecs` accepted `note_creation.feature` here and returned both private-mock requirements false. `./scripts/run.sh mysql --protocol=TCP --host=127.0.0.1 --port=3309 --user=root --batch --skip-column-names --execute 'SELECT VERSION()'` reached MySQL 8.4.11. Application allocation/start remains the slice-2 probe; no foreign database or listener may be adopted. Installation CI run 41420017308 completed successfully for exact d43e39016a; it is independent from migration proof.

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

### 1. Enable proof recording explicitly

Type: Behavior
Status: done

Pre-condition: an ordinary terminal Cypress run shares configuration with deliberate proof generation.
Trigger: run tests with recording omitted or explicitly set to `RECORD_CUCUMBER_PROOF=1`.
Result: only the explicit run touches proof; both paths keep the selected tests, output, verdict and ownership. Gate before recorder discovery, proof I/O or Messages activation. Cover the existing pipeline's wrapper and raw terminal consumers. Configure the single Gherkin `proofGeneration.command` with root wrapper and focused feature; document selection and deliberate committing. Update implicated retry fixture commentary without changing its behavior.

Proof: add discriminating normal passing/failing checks with absent and preexisting proof, compare enabled output/exit to normal, and run `./scripts/run.sh pnpm test:cucumber-proof` plus the event composer suite and `./scripts/run.sh pnpm cy:lint`. Keep focused/tagged/dry/report/retry/hook/scoped behavior. All-skip does not claim execution.

Sizing: target 5–10 minutes implementation plus serial real-Cypress proof; the existing fixture starts many real browsers, so the proof loop may exceed 10 minutes. This bounded compatibility loop is kept cohesive rather than split by test modes. Slice 2 replaces the accepted interim run-file layout; no accumulation is claimed final.

Accepted proof: new runner check first failed in 8.28s because an ordinary run created proof; the public recorder check first failed because it configured itself without the option. After the single early recorder gate and adapter reorder, `./scripts/run.sh pnpm test:cucumber-proof` passed 23/23 in 66.14s, including 12 serial real Cypress launches. One metadata-only mirror assertion was subsequently removed, leaving 22 retained behavioral tests all covered by that run. Composer 3/3 passed. `./scripts/run.sh pnpm cy:lint` passed 1958 files after direct equivalent-boolean/layout repairs. No browser replay was needed for those repairs, restored original 180-second harness timeout or docs/comments. Parent inspected the four ordinary verdict × absent/existing inventories, explicit reporter/selection/exit/ownership comparisons and all 44 consumer dispositions in `/tmp/codex-deliberate-proof-slice1-trace.md`. Implementation active elapsed was about 15 minutes including the integrated compatibility loop and its obligatory consumer/contract trace; retained as the stated cohesive proof-loop exception, without expanding scope. Committed legacy proof remained untouched.

### 2. Refresh one maintained successful snapshot

Type: Behavior
Status: done

Pre-condition: recording is explicitly enabled and legacy or maintained proof may exist.
Trigger: a selected scenario finishes its last executed attempt.
Result: under existing checkout lock, reread all proof, reduce legacy observations latest-first with unsuccessful ties winning, apply the completed observation, retain passes in stable identity order, atomically update `latest.json` and delete consumed files or empty snapshots. Preserve unexecuted/newer identities and immediate completed progress. Do not persist failure markers or commit automatically.

Proof: recorder/lifecycle/process tests cover old and new layouts, repeated refresh, both timestamp tie orders, nested files, concurrency/interruption, non-runs, final failure and restoration; real Cypress focused refresh preserves an unrelated identity and normal output/exit. Run the affected full proof suite, composer suite and E2E lint. Deliberately refresh note creation using the configured root command, retaining provenance and verifying readable compact proof plus consumed-file cleanup in the same commit.

Sizing: target 5–10 minutes implementation plus serial proof. Real application proof is one unpaid focused owned-stack batch; probe its setup/selection before launch, stop dependent work on unavailable allocation, and keep this result distinct from the fixture runner's observations.

Accepted proof: new snapshot cases failed 4/4 against slice 1, then the full proof suite passed 26/26 in 69.19s with 12 serial real Cypress launches; composer passed 3/3. Compact serialization and a proof-only Biome formatter override preserve parsed schema/metadata; identical redundant JSON formatter settings were replaced by inheritance so root config fits the strict 250-line limit. After this adjustment, focused recorder/lifecycle/process/snapshot checks passed 25/25 in 1.44s. No meaningful recorder/runner behavior changed after the full loop, so its evidence was reused. All 84 consumer dispositions and concrete assertions are in `/tmp/codex-deliberate-proof-slice2-trace.md`, with prior routing reused; dated proof files' final disposition is the application adoption below. Active implementation was about 12 minutes including the bounded serial proof exception, without scope widening.

Actual root generation: the exact configured `RECORD_CUCUMBER_PROOF=1 CURSOR_DEV=true nix develop -c pnpm cy:run --spec e2e_test/features/note_creation_and_update/note_creation.feature` passed 9/9, no failures/skips, 19s feature duration. On this Mac, 46% of 48 GiB was free and load 11.66; no owned fixture process remained, so one serial unpaid application batch was chosen, leaving unrelated Playwright work untouched. Runner allocated `wt_a89ae3f51cf3415bb535d38cab70438c`, database `doughnut_e2e_wt_a89ae3f51cf3415bb535d38cab70438c`, origin `127.0.0.1:53700`. Both owner and proof locks were absent at terminal exit.

Adoption comparison: six immutable legacy files/43 observations at run-start SHA `5d43f015c75be52bb50dbc8a26953e760cecd382` reduce to 23 successful identities. New `latest.json` is valid version 1, 28 lines, with all 14 unexecuted observations deep-equal in every field and nine fresh note-creation passes (one attempt, positive durations), source blob `f6bd4975da729f087a51a5817fa8575aa1b9dd70`, finished 2026-11-02T09:57:27.356Z–09:57:41.807Z. Snapshot blob is `8fc909182f46e0ee6d578b33ad444438b559be38`. HEAD remained unchanged; no automatic commit. All six input files were consumed. Post-generation `./scripts/run.sh pnpm cy:lint` passed 1954 files. One-time legacy filename/allocation sweep returned no matches in recorder modules/project guidance and only `latest.json` in the proof directory; this is an acceptance reading, separate from maintained behavior proof. Independent refactor returned `none — already clean`, `REFACTOR COMPLETE`, with no edits or repeated proof; repository whitespace passed and all current files fit the strict size gate. All 46 source-review entries and slice-2 obligations passed; source freshness remained unchanged against trunk a919988a84.

## Execution complete

Product advice: no change. The authorized migration is implemented with no additional framework, deployment or product backlog recommendation.

Retrospective: reviewed the original story/delegated contract and all 46 independently extracted commitments against aggregate implementation, tests, current documentation and retained proof. Manifest: b34d5c232b admission and f6bbd5aeac preparation are provenance; 5d43f015c7 delivers the opt-in behavior, 88e0ad9963 the compact refresh and deliberate adoption. The uncontaminated branch range from d43e39016a contains only this work; trunk a919988a84 is an unrelated backend dependency update. Responsibilities remain with the existing Messages adapter, identity/attempt recorder and checkout-local store; ADRs 0006/0007 are followed, North Star product direction is unchanged. Unit variation tests and the real runner/application journeys retain the promised coverage; the measured serial fixture loop remains bounded, with no unsupported suite rewrite. Process review was enabled and used available coordinator/tool history plus agent returns; no persistent supported finding was identified, and unobserved agent-internal actions were not inferred. No correction plan or queue change is needed. Installation CI d43e39016a passed run 41420017308; migration CI remains pending on its own exact target/revisions. Post-formatter snapshot blob remains `8fc909182f46e0ee6d578b33ad444438b559be38`.

## Current decisions

- Recording option is exactly `RECORD_CUCUMBER_PROOF=1`; no default, CI or optimization command enables it.
- Snapshot schema remains version 1, successful observations only, preserving original timestamps and source versions during adoption.
- Filesystem no-write assertions establish the explicit ongoing opt-in contract requested by Terry. The one-time product sweep for obsolete automatic-recording wording and run filename allocation is an acceptance reading, separate from retained behavioral proof.
- Expensive proof runs serially on Terry's Mac (48 GiB; 45% free memory, load 7.74 at preparation). Other Open Dough Playwright work is active; leave it untouched. Before launching real Cypress, inspect memory/load; use one fixture process or one application stack at a time. Reconsider on contention, failure or changed workload; each command owner follows its terminal result.

## Concern review

Refinement was not needed: the two behavior slices separate explicit authorization from maintained storage, each with one evaluable proof loop. Remaining concerns: none; application allocation is an explicitly bounded early probe before its dependent live test run.

Independent slice-1 refactor returned `none — already clean`, `REFACTOR COMPLETE`; no edits or repeated tests. All affected code files remain below 250 lines. Source freshness was unchanged against trunk a919988a84 (only backend dependency updates beyond preparation); 46 source-review entries passed the slice-1 check.

## Learnings

The strict refactor file-size limit includes generated proof. One observation per line and a narrowly scoped proof formatter override keep this snapshot compact; JSON linter/parser remain enabled and all other formatter settings inherit their unchanged values. This affects representation only, not version-1 meaning.
