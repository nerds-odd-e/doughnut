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

Slice 1 published as `f3bb7162effc6c4f3e004bb3097541c56216abdf` to origin's story branch, suffix base `b1713646ac1cd488a15e9b8366b332d6aa4b0171`, zero reconciliations; existing observer reused. This is the next increment's previously published base. Delivery freshness was unchanged at trunk `439f3fea6888084507d6fe4a022e9b89c911d08b`; unrelated main movement created no named-sibling hold. Git worktree was clean after delivery.

## Consumers, structure and acceptance reading

Guidance reaches projection-drift, history-reset and folder-publication safety controller tests, CLI
rebased-rejection/resolved-continuation suites and sync docs. Preserve their refusal/local-work promises.
Record actual consumer trace. App closure reaches entry/workflow/payload/publication/retry/bootstrap tests,
CI/deploy YAML and operator docs; fixture presence is not behavioral proof.

Expected edit candidates are below 250 lines at `8c0f3a25d2`: CLI dispatch 203, LFS suite 234, app CI 246,
publication suite/fixture 202/196, retry suite 207, encoder 228, Git HTTP 98, publisher 201, drift tests 187.
Keep new large-tree proof in its capability suite and notebook orchestration in its owner if dispatch would
exceed 250. TreeShape is 251: prefer direct reuse; if editing is required, 3 must assign cohesive
inspection/classification versus correspondence/admission reorganization, callers and retained controller
proof before implementation.

8's separate one-time whole-product reading sweeps `CLI_DIST_ARTIFACT`, `CLI_BUNDLE_SOURCE`, `CAPTURED_CLI`,
`captured-cli` and app CLI artifact/publication references. Delete exclusive dependents transitively; classify
surviving shared CLI/LB/E2E uses. Preserve dated provenance; delete obsolete synthetic fixture branches rather
than renaming their actions. No permanent absence test.

## Slice 2 acceptance

Current-head drift and stale-base triggers were compared with the original source and the implementation. `cli/tests/notebookDiagnosis.test.ts#reviews exact server differences while retaining pending commits and dirty work` uses a real bound checkout with one pending commit, staged/unstaged edits and an untracked file; only the backend SDK is mocked. It observes A/tree IDs, classifications, exact README trailing spaces, EOF marker, LFS pointer text, empty marker, exact binary base64, and unchanged `localGitObservation` plus commit count. Matching content has a separate positive case. Product uses existing authentication/binding and a disposable directory for ordinary Git diffs.

Retained source journeys were inspected: stale accepted head advice/refusal in `notebookPublish.ancestry.suite.ts`; Pull rebase plus subsequent publication in `notebookPull.rebase.suite.ts`; genuine conflict pause in `notebookPull.conflict.suite.ts`; resolved child bundle/head/parent in `notebookPublish.resolvedContinuation.suite.ts`; distinct drift rejection after rebase and resolution in the two rejection suites. Backend `NotebookGitProjectionDriftControllerTest.rejectWhenAnUnsynchronizedNoteHasDriftedTheProjection` retains accepted binding/content and current private IDs on refusal. All 15 applicable source-review rows cite their observation. Later actual owner recovery stays with slices 3–5/11.

`CURSOR_DEV=true nix develop -c pnpm cli:test` passed 68 files / 442 tests after the final typed SDK-helper correction (45.25s). Its existing Python discovery ran zero tests; no Python changed and it proves no Python behavior. `CURSOR_DEV=true nix develop -c pnpm backend:test_only` passed 3,055 tests, zero failures/errors, two skipped (1m52s). The backend change is message-only; slice-1 API generation remains compatible. [Consumer trace](consumer-trace-cli.md) records dispositions. Active implementation was approximately nine minutes excluding the explicit backend-runtime exception; first clock evidence followed reading, so this estimate is approximate. No observed hard-limit overrun required refinement.

Optional `CURSOR_DEV=true nix develop -c pnpm -C cli exec tsc --noEmit` remains failed with 113 existing configuration/import/type diagnostics. Coordinator inspected the package's supported esbuild/Vitest/Biome commands and the unchanged Node16 config, generated import and `Key.name` site at `f3bb7162`; G1 records the bounded no-user-cost disposition. The new SDK-envelope error was corrected with existing `runDefaultBackendJson<Report>` and required CLI proof rerun. This optional diagnostic is not accepted as passing proof. Raw evidence `/tmp/seed121-slice2-tsc-diagnostic.log` may expire; its command, representative baseline sources and limitation remain here and in the trace.

Read-only release-checkpoint preparation at 2026-11-10T06:31:22Z: production `/api/healthcheck` returned HTTP 200, `prod`, build `86c31d195518cb7183adddbfffd51789db0164b8`. Its response is plain text; an initial JSON-decoding assumption failed and a bounded plain-text read corrected that observation. Remote immutable tag enumeration showed newest app `v1.3.69` and CLI `cli-v0.5.27`; versions must be rechecked at release. No locked incident comparison or identity evidence has yet been obtained. Slice 3 needs its named landing/release go-ahead plus affected notebook/checkout confirmation and ordinary owner/operator access or owner-run diagnostic/private identity observations. The launch's supplied landing context must accompany the first trunk publication.


Slice2 independent refactor completed: notebookDiagnosisDoc owns unchanged command help/usage and has two production callers. Coordinator inspected the three changed CLI paths and /tmp/seed121-slice2-refactor-consumer-trace.md. Full cli:test passed 68 files / 442 tests (85.51s); Python discovery selected zero. Backend and API-generation proof unchanged. Current doc extraction matches original sections and remains within the planned seam. Coordinator selective formatter and file-size check passed; G1 remains the explicit optional-static-diagnostic disposition. No unresolved slice2 behavior/proof gap. After this delivery, slice3 remains at the named production landing/release checkpoint; preserve worktree, accepted history and source records for that decision.

## Approved incident checkpoint and retained landing

On 2026-11-10 the developer approved the proposed keep-worktree landing of `ef93c92ca989a668dd13e882d16530f10bc442e3` and app `v1.3.70` from its exact landed main SHA after CI/artifact admission. They directed discovery from their Japanese learning project / Japanese 2 folder. This settles the first production checkpoint only; actual forward adoption remains exact-state owner review. Read-only binding and ordinary authorized API metadata resolved the affected notebook; private source paths, IDs/content and pending-commit evidence remain under `/Users/terryyin/.codex/tmp/seed121-incident/`, outside Git. The owner checkout is intact.

History-preserving Dough Land accepted `9464923e3953f03b54a0a0733aa8e43d368eb63e` on origin/main, base `ffeb5a5ea29936cc378b1e4f91de891bda090ef8`, one merge/push, no rejection. Queued-closure guard was clear, no closures. Incoming changes were only unrelated SEED-118 preparation records, so accepted product proof remained applicable. Launch landing context was supplied and the accepted comparison was recorded (delivery `52d66a07-e34d-4560-81e8-442aa3ab4f91`, receipt `89e4ebfd-0355-4959-8ee7-a86756d952bc`). Later trunk publications take no landing context. Workspace/branch/Taken/source/plan remain; default checkout was not supplied for refresh. Branch base remains `ef93c92ca989a668dd13e882d16530f10bc442e3`.

Resumed setup in the same checkout reused current locked dependencies; wrapped CLI Vitest version succeeded. Old branch observer `/tmp/dough-ci-501/watch-unYhQk` was stopped and process exit confirmed at the prior approval handoff. New branch observer: coordinator `aino-seed121`, cell `g971:9`, session `51127`, directory `/tmp/dough-ci-501/watch-V4R8mB`, PID `81806`; managed existing-candidate delivery attached `ef93c92c` without another push. Separate main observer for retained landing: coordinator `aino-seed121-main`, cell `g971:8`, session `88761`, directory `/tmp/dough-ci-501/watch-9gPMPx`, PID `65535`; registered `9464923e` and completion command session `97014` owns its exact CI/shutdown observation. Verified source remains GitHub Actions `ci.yml` / `donut CI`.

Exact main CI `9464923e3953f03b54a0a0733aa8e43d368eb63e` succeeded, run `42773580936` attempt1, with confirmed observer shutdown and no unread events. The initial ten-minute completion returned pending with original deadline retained; its exact resume then returned success. Main observer is closed; branch observer remains live. Current production build already contains schema prerequisite `300000354` (introduced 2026-10-14), and there are no Flyway migration differences between deployed `86c31d19` and release candidate `9464923e`; this diagnostic release adds no schema cutover.

Accepted-history download independently confirmed the pending commit is based on current accepted head. Its native tree listing (core.quotePath=false) is 1,394,153 bytes / 10,955 files, above 1 MiB; exact accepted tree and pending patch remain private. Authentic CLI0.5.9 was preserved privately with original digest `5b868908f33576604389ad9b41f26a21eaa6efef982c570e2a60488d3365525c`. Private preliminary pending-path baseline used four ordinary GETs: one existing Note mapped soundly with no current-owner trackers; two pending additions have no accepted/live exact-title Note correspondence yet. These are preliminary observations, not locked drift or preservation proof.

Approved immutable app tag `v1.3.70` was pushed at exact main `9464923e3953f03b54a0a0733aa8e43d368eb63e` after CI 42773580936 / attempt 1 success and all three unexpired artifacts were verified at that SHA: backend13743086813, frontend13743216533, CLI13742882597. Application Release run42775459737 admitted the release and validated the payload; actual publication is still in progress at this observation. Owner of `gh run watch --exit-status` is this coordinator (session78488). Expected frontend/CLI artifacts were downloaded through the connector, archive digests matched GitHub metadata, and exact expected payloads retained privately for production smoke comparison. No host CLI install changed. Two bounded ordinary NoteRealm GETs returned503 during rollout; their private responses are retained as unavailable observations. Further note requests are held until release/health success.

## Locked incident observation and origin gate

App v1.3.70 admission selected exact release/build9464923e, CI 42773580936 / attempt 1; publication job 129693007692 completed successfully and recorded deployment. Production smoke returned selected build SHA and exact admitted HTML, mainJS and CLI bytes. Admission/deploy logs and smoke inputs/results are retained privately. Rollout503 observations preceded final successful health; they do not establish a notebook mutation or a baseline failure after deployment.

One owner-authorized locked diagnostic completed 2026-11-10T07:37:01Z:4 differences (2 changed Markdown notes and2 live-only Markdown notes), with exact A/accepted/live native IDs and bytes. Complete native reconstruction from accepted history plus all 4reported differences matches the reported live tree exactly; the human-readable exact diff remains private. All 4Note/Folder correspondences are sound in ordinary current metadata. Separate representative current-owner baseline retains3 trackers and38 recall records for one tracker, with validated associations; it is not a whole-notebook or locked private-state preservation observation.

The observed pairs match note extraction: original rewrites link their new sibling notes, and each original update has the same timestamp as the sibling creation (2026-09-29 and 2026-10-10). Accepted history began with a2026-09-17 snapshot and contains no later accepted change for those4 paths. Pre-9cdb6d998d AiController.createExtractedNote directly persisted both notes outside accepted publication. Existing 9cdb6d998d (2026-11-04) routes the operation through WebNoteCreationService/AcceptedWebChangeService; both prior deployed 86c31d19 and new 9464923e contain it. It corrects future extraction, while delta publication deliberately leaves older unrelated drift unchanged. Existing extraction controller proof observes a new parent/path/timestamp; a bounded strengthening should additionally observe exact accepted/live tree equality rather than invent another production fix.

Independent disposable origin review supports this mechanism but leaves actual historical action attribution open (G2): paired content/timestamps and compatible code do not prove the historical caller. Historical GitHub publication/health logs now verify the applicable old code was deployed: v1.3.32/a0ab5999 at2026-09-28T07:55Z (run36393690732/job108835164533), and v1.3.41/d57d8c27 at2026-10-10T08:20Z (run38037174840/job114170007215). Exact source at both revisions calls the unwrapped construction operation. Historical request-log/template access through local gcloud is unavailable because its active account requires interactive reauthentication; no authentication/configuration change was attempted. Owner confirmation of the specific two Extract-note actions is pending. Slice 3's mandatory origin gate holds dependent recovery implementation.

Refinement review is held at that existing gate: completed 1–2 and all compatible proof remain; do not fabricate a source correction or select historical repair from timing alone. Once G2 settles, refine remaining recovery/admission/derived-state leaves in place and carry source commitments to their actual owners. This progress changes records only (records-only refactor disposition); it does not renew readiness or mark slice 3 done.
