# Execution context and accepted proof

Execution resumed 2026-11-10 in the established linked worktree
`/Users/terryyin/git/doughnut/.worktrees/publish-large-notebooks-without-losing-history`, branch
`codex/publish-large-notebooks-without-losing-history`, Story Branch Mode. Publisher
`dashboard-territory.local-doughnut`, agent Aino-chan; starting revision
`adc9f4ed41ac06af34c037851c8dce7682461b04`, published claim/base and initial candidate
`b1713646ac1cd488a15e9b8366b332d6aa4b0171`. Authorized delivery is origin's execution branch;
integration target is origin/main. No new claim was made. Checkout setup
`./scripts/run.sh bash scripts/worktree_setup.sh` and wrapped Gradle `--version` succeeded with
the locked dependencies. The checked-in pre-commit hook runs check-only staged lint.
Existing planned refinement authority remains; the incident gate mandates in-place refinement.

CI observer: Codex yielded cell `g921:8`, coordinator `aino-seed121`, session `37670`,
directory `/tmp/dough-ci-501/watch-unYhQk`, PID `96376`; selected source GitHub Actions
`nerds-odd-e/doughnut`, execution branch, verified workflow `ci.yml` / `donut CI` (5208694).
Claim on main is unobserved by this branch observer. Reuse this exact observer for delivery.

Expensive-proof decision: local Mac host, one full backend command at a time; 48 GiB RAM,
load averages 3.54/4.38/3.96 at 14:03 +08, no observed competing Java test process. Other
sessions may exist and remain untouched. Run required backend suite; implementation owner
retains its terminal result. Reconsider on contention/failure or a changed workload. API generation
and frontend proof follow sequentially at delivery when needed.

## Decisive premises and limits

| Consuming premise | Observation / remainder |
| --- | --- |
| Current CLI small-change selection works above 1 MiB | At `8c0f3a25d2`, `2026-11-10T05:35:26.794Z`: `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsx --input-type=module - < /tmp/seed-121-cli-large-publish-probe.mjs`; 11,000 accepted files/1,529,000 bytes, old scan ENOBUFS, three edits/555-byte change listing, one successful 31,995-byte submission and clean main. Backend fetch mocked; 6/7 own permanent discrimination and actual acceptance. Probe hash and limits are in the seed; temporary script may expire. |
| Affected updater cannot install advertised fixed input | Public object at `2026-11-10 05:31:34 UTC` embedded fixed scan/version 0.5.5; installed binary embedded old scan/0.5.9. Seed retains public digest. `update.ts` admits strictly newer versions. This observes input, not completed update; 9/10 own actual replacement/execution. |
| App overwrite mechanism and retained proof routes | Source shows two callers of one uploader; existing publication tests expect app upload. At `8c0f3a25d2`, `CURSOR_DEV=true nix develop -c node --test scripts/ci/application-release-payload.test.mjs scripts/ci/application-release-publication.test.mjs` passed 19 in 4.43s; workflow/retry pair passed 11 in 3.39s. Route/cost proxies only; 8/9 prove changed behavior. Observed public object's publishing actor remains unknown. |
| Actual discrepancy, origin, safe correspondence/derived state | No consistent production diff or confirmed cause. Only credentialed deployed comparison can settle it. Permitted early probe 3 must capture evidence and refine correction/recovery leaves before implementation. No-drift, unstable or unexplained evidence stops dependents. |
| Integrated fixture and deployed journeys | The two named CLI specs passed executable `assertSupportedIsolatedCypressSpecs` admission with both mock requirements false. No E2E stack or large backend fixture was run in preparation. 7 owns new large-workload fixture/proof; live release/update/recovery are owner-held/state-changing observations in 3/10/11, not established by source inspection. |


## Slice 1 acceptance

At trigger owner requests a diagnostic of the bound notebook, the source expects differing paths and current accepted revision, including live-only/accepted-only/changed content, pointers and structural entries (C14–C19, J04.5–J04.6). The coordinator inspected `NotebookGitDiagnosis.inspect`, shared `NotebookGitProjection.liveTree`, HTTP operation and all five controller observations. They report exact A/accepted/live native tree IDs and bytes, using the same canonical tree as the publication guard. Authorization and both queued sanctioned web publication orders pass. Every applicable source-review row has its specific observing method.

`CURSOR_DEV=true nix develop -c pnpm generateTypeScript` passed. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed 3,055 tests, zero failures/errors, two skipped, 1m19s. The diagnosis XML records five passing cases at 2026-11-10T06:14:33Z on Territory.local. Initial test compilation used a private helper; public committed-transaction support corrected it. The first executable run exposed a fixture builder overwriting earlier README drift; raw drift setup now follows all builders. Product code was unchanged by that correction; the required full suite was rerun to its terminal pass.

[Semantic consumer trace](consumer-trace-diagnosis.md) covers the shared publication guard, its notebook-Git consumers and regenerated SDK/OpenAPI. Existing drift refusal, folder publication and private identity behavior passed the full backend suite. No existing frontend code calls this additive endpoint; generated client frontend proof remains due at delivery. Source freshness returned unchanged at pinned trunk/base `b1713646ac1cd488a15e9b8366b332d6aa4b0171`; no named sibling coordination applies. Independent refactor is pending.

Independent refactor completed: the test native-tree oracle reuses existing raw-JGit commit fixtures plus RevWalk. The refactor backend:test_only passed 3,055 tests, zero failures/errors, two skipped (1m23s); product/API/generation unchanged. Coordinator inspected the replacement oracle and /tmp/seed121-slice1-refactor-consumer-trace.md. Frontend:test passed 399 files / 2,470 tests; vue-tsc --noEmit and tsc --noEmit passed against the generated client. Coordinator selective formatter passed; all owned non-generated files are within 250 lines. No unresolved slice-1 gap.
