# Test Optimization Candidates

## Latest pass

- **Command / scope:** planned execution of SEED-098#remaining-test-costs, covering the two transferred candidates: first-batch E2E startup attribution and two AI-reply sibling-spec merges. Preserved owned-stack readiness and cleanup, HMR, browser-file isolation, Cucumber proof and normal selection.
- **Revision:** measured on `6c045ccfd5` (E2E attribution), `0d343fdc2a` (protocol merge experiment) and `4ae0329ed8` (mounted merge experiment and re-profile); 2026-11-02, macOS 26.6.2, 16 cores, shared host load 3–20 from unrelated sessions.
- **Before / after:** ordinary LFS batch 55.0s first in a fresh checkout vs 41.9/43.3s warm. Focused AI-reply command: protocol merge no gain (~60–80ms slower, within noise); mounted merge faster in 6/6 pairs (Vitest ~−0.2s, ranges not overlapping under calm load). Full frontend suite before 85.03/91.70/84.82s vs after 84.10/94.35/87.37s; final re-profile 87.37s, 396 files / 2454 tests.
- **Outcome:** measured gain — focused command only; the mounted streaming/unknown-tool merge is kept. Protocol merge undone for no demonstrated gain. No removable E2E startup stage was found.
- **Limits:** one natural first E2E sample, which cannot be repeated without deleting caches. The full suite's ~10s load-following spread cannot resolve per-file savings. Host was not quiet.
- **Evidence:** [execution plan results](slice-plans/080-remaining-local-test-costs/PLAN.md); raw logs stayed outside Git.

## Pending candidates

### e2e-batch-cold-start

- **Locations / scope:** first cold local `pnpm cy:run` batch versus later warm
  runs of the same owned stack; seen on `cli_notebook_lfs.feature`.
- **Observed cost:** 2026-11-01 at `ee6ecf2549`, cold first LFS ~4m18s vs warm under ~1 minute. 2026-11-02 at `6c045ccfd5`, first run in a fresh checkout with a warm `~/.gradle` build cache 55.0s vs warm 41.9/43.3s, 10/10 scenarios each. Of the ~12.4s gap, Cypress healthy→run-start was +6.8s (unattributed) and backend readiness +4.2s (Gradle first configuration plus `compileJava` FROM-CACHE, Flyway first migration ~0.7s). Mountebank and Vite are off the critical path.
- **Unique protection:** fresh owned stack with cold caches/boot on a session's
  first batch.
- **Alternatives attempted:** SEED-039#candidate-resolution measured warm readiness polling only. SEED-098#remaining-test-costs attributed stages from existing logs and found no measured removable stage, so no startup change was made.
- **Disposition / rationale:** pending — missing evidence: a stage breakdown of the Cypress launch window (config, `setupNodeEvents`, browser launch) on a genuine first run in another freshly prepared worktree versus a warm run, and a first run with a `compileJava` build-cache miss to show whether real compilation explains the historical ~4m18s. Flyway first migration of the per-worktree database is inherent under ADR 0007. Retain real ownership, readiness, cleanup, HMR and Cucumber proof.
- **Date:** observed 2026-11-01; attributed 2026-11-02.
- **Evidence / follow-up:** [plan premises and learnings](https://github.com/nerds-odd-e/doughnut/blob/744541489cee5b4a24ae7277cab65668fad452e3/.planning/slice-plans/001-cheaper-local-test-feedback/PLAN.md); [stage attribution](https://github.com/nerds-odd-e/doughnut/blob/0d343fdc2aa9484665381b89545b24f66ac14139/.planning/slice-plans/080-remaining-local-test-costs/PLAN.md) slice 1 Result.

### frontend-sibling-spec-setup

- **Locations / scope:** remaining cohesive sibling frontend Vitest families
  after the reading-panel visibility/target merge (~25 families of 2–3 siblings
  historically). BookReadingPage still has separate marking, notebookSwitch,
  readingPosition, and base specs beside the merged visibility file.
- **Observed cost:** retained merges historically ~70–250ms per removed file. 2026-11-02: merging the mounted `AiResponse` streaming/unknown-tool pair saved ~0.2s on the focused command; merging the `aiReplyState` content/toolCalls protocol pair showed no gain. The full suite cannot resolve either.
- **Unique protection:** each family's distinct component behavior, harness,
  mock boundaries, and real/fake timer requirements.
- **Alternatives attempted:** earlier responsibility-based merges (346→295 files); SEED-039#candidate-resolution merged the compatible reading-panel pair (245 lines) on 2026-11-01. SEED-098#remaining-test-costs kept the mounted reply merge (`AiResponse.reply.spec.ts`) and undid the protocol merge for no demonstrated gain on 2026-11-02.
- **Disposition / rationale:** pending for the unpicked families, including the BookReadingPage remainder with its distinct journeys and 245-line visibility spec. Do not retry the `aiReplyState` protocol pair without new evidence. Judge any merge by alternating focused ordinary-command runs, because full-suite noise (~10s) hides per-file savings. Browser isolation and existing exclusions remain intact.
- **Date:** 2026-09-26; unfinished remainder returned 2026-11-01; AI-reply pairs assessed 2026-11-02.
- **Evidence / follow-up:** [completed frontend pass, remaining families, and merge constraints](https://github.com/nerds-odd-e/doughnut/blob/fe990eff58/.planning/quick/041-faster-frontend-unit-tests/PLAN.md); [AI-reply merge measurements](slice-plans/080-remaining-local-test-costs/PLAN.md) slices 2–3 Results.

## Accepted necessary costs

### frontend-browser-file-isolation

- **Locations / scope:** frontend Vitest browser-page and module-graph isolation
  per spec file; 339-file suite measured on 2026-09-19.
- **Observed cost:** summed reported file durations ~7.6s versus ~64.5s wall
  time; the legacy analysis attributed ~88% to per-file setup/import cost and
  measured ~190–280ms marginal cost per extra spec file. Summed durations are
  not wall time or measured CPU time.
- **Unique protection:** independent browser/global state for each spec file.
- **Alternatives attempted:** `--no-isolate` reused one page across files;
  216 of 339 files failed through shared global state and the run took 325s,
  about 5× slower. Later cohesive sibling merges retained isolation.
- **Disposition / rationale:** accepted necessary cost — preserve the recorded
  decision requiring per-file isolation. Vitest has no profile-exclusion
  mechanism here; acceptance changes no verification or profiling selection.
- **Date:** 2026-09-19.
- **Evidence / follow-up:** [retained profiling observation](https://github.com/nerds-odd-e/doughnut/blob/82e789ee86/.planning/test-optimization-candidates.md#frontend-profiling-note).
  Preserve isolation while assessing the pending sibling-spec opportunity.

### notebook-git-proposal-fixtures

- **Locations / scope:** `backend/src/test/java/com/odde/donut/controllers/NotebookGit*ControllerTest`; 51 files called `GitBundleTestReader.fetchHead` in the 2026-09-20 observation.
- **Observed cost:** unmeasured; repeated proposal crafting and tip-commit inspection scaffolding (`InMemoryRepository`, `readContent`/`readExactTree`).
- **Unique protection:** parent count, first-parent ancestry, and author identity require inspecting the tip commit, beyond reading its tree entries.
- **Alternatives attempted:** three attachment classes shared their live
  projection read. The three byte-identical `acceptedEntriesOf` copies now use
  `NotebookGitControllerTestBase.acceptedHistory(notebook).exactTree()`, backed
  by `testability/GitBundleTestReader.fetchAcceptedHistory`. A helper for only
  3 of 51 callers was rejected as an additional idiom; other commit readers
  correctly open their own repository for the distinct commit observations.
- **Disposition / rationale:** accepted necessary cost — current proposal construction already shares `proposalBundleBytes`, `localCommitOnTopOf`, and `commitOnTopOf` in `NotebookGitCommitFixtureTestSupport`. Multi-commit/range and author/parent observations still need real repositories and history; a blanket writer mirror would duplicate that existing seam without demonstrated removable cost. Retain this real Git proof and the local idiom for distinct history shapes. Acceptance changes no selection.
- **Date:** 2026-09-20; reader names updated 2026-09-24.
- **Evidence / follow-up:** [attachment-work observation](https://github.com/nerds-odd-e/doughnut/commit/6b906462dd),
  [reader-name update](https://github.com/nerds-odd-e/doughnut/commit/c50a90c159). Establish a focused timing baseline before proposing a speedup. Current-source reading on 2026-10-30 at `bf0bbc01b3`: `controllers/NotebookGitCommitFixtureTestSupport.java`, `NotebookGitAttachmentLocalChangeControllerTest.java`, `NotebookGitComposedMoveEditControllerTest.java`, `NotebookGitBundleDownloadControllerTest.java`, and `testability/GitBundleTestReader.java`. No writer sweep or timing run was performed. Resolution: 2026-10-30.

### e2e-vite-first-page-load

- **Locations / scope:** local `pnpm cy:run`, Vite-served frontend; 14 CLI features in the 2026-09-25 pass.
- **Observed cost:** ~0.7–1s for the first page load of each web-using scenario; a production build was estimated to save ~15–25s across that scope.
- **Unique protection:** frontend HMR in local E2E batches.
- **Alternatives attempted:** production-build serving was proposed and declined
  by the owner on 2026-09-25 when the CLI E2E pass stopped at ~41% improvement.
- **Disposition / rationale:** accepted necessary cost — reuse the owner's 2026-09-25 decision to keep local HMR. The current batch wrapper still serves Vite by default; built-asset serving exists for other invocations but does not supersede that decision. Real first-load integration remains necessary under the selected mode. No new authorization question or exclusion is needed. SEED-039#candidate-resolution (2026-11-01) left this unchanged (no built-frontend substitution).
- **Date:** 2026-09-25.
- **Evidence / follow-up:** [CLI E2E pass](https://github.com/nerds-odd-e/doughnut/blob/e2f9929fe9/.planning/quick/032-halve-cli-e2e-time/PLAN.md),
  [candidate recording](https://github.com/nerds-odd-e/doughnut/commit/753dbd9b5d). Current-source reading on 2026-10-30 at `bf0bbc01b3`: `scripts/e2e-runner.mjs`, `scripts/sut-services.mjs`, and `.agents/skills/e2e-authoring/SKILL.md` confirm the default local HMR path. The historical 15–25s estimate remains unmeasured. Resolution: 2026-10-30.

### backend-profile-context-boots

- **Locations / scope:** `DevelopmentAuthenticationConfigurationTest` (`dev`),
  `ShedLockConfigProdTest` (`prod`), and failure-injection profiles
  `notebook-git-publication-atomic-test` and `batch-row-import-atomic-test`.
- **Observed cost:** ~1s per distinct context boot on 2026-09-26. Sharing
  `testability/SpringTestBase` for other `test`-profile tests reduced the suite
  from 21 boots to 5.
- **Unique protection:** profile-specific configuration and injected
  persistence failures require their distinct contexts.
- **Alternatives attempted:** ordinary test-profile contexts were consolidated;
  no alternative for the remaining distinct contexts was attempted.
- **Disposition / rationale:** accepted necessary cost — retain real profile-specific wiring and isolated persistence-failure seams. The dev test exercises sign-in, stored sessions, and cookie behavior; production attachment wiring has its own narrow context. Git and batch-import failure tests already share configuration within their families. Merging failure injection into the ordinary test context would spread exceptional mutable failure state to unrelated tests, while a mock would lose transaction rollback proof. No worthwhile cheaper replacement is evidenced; acceptance changes no selection.
- **Date:** 2026-09-26.
- **Evidence / follow-up:** [backend re-profile](https://github.com/nerds-odd-e/doughnut/commit/db643ef844),
  [shared-context guidance](https://github.com/nerds-odd-e/doughnut/commit/97ce679b18). Current-source reading on 2026-10-30 at `bf0bbc01b3`: `DevelopmentAuthenticationConfigurationTest`, `NotebookAttachmentContentConfigurationProdTest`, `NotebookGitPublicationAtomicTestSupport`, `NotebookGitPublicationAtomicControllerTest`, and `QuestionGenerationBatchRowImportServiceAtomicTest` plus its support. `ShedLockConfigProdTest` is a historical location; it is not present at the inspected revision. Current boot count and cost were not measured. Resolution: 2026-10-30.

## Profile-only exclusions

Existing mechanism: `@skipOptimizationDueToKnownNecessarySlowness` on a
Scenario, Scenario Outline, or Feature. For `pnpm cy:run --spec`, leave tagged
features out of the spec list and subtract tagged scenarios inside selected
features from the profile; the wrapper drops `--expose tags=...`. Normal
verification and CI still run them. See
[E2E profiling guidance](../.agents/skills/e2e-authoring/SKILL.md#test-optimization-profiling).
The entries below inventory existing tags; their original decisions are retained.
SEED-039#candidate-resolution (2026-11-01) added no tags and narrowed no
normal/CI coverage.

### bazaar-subscription-journey

- **Locations / scope:** [bazaar_subscription.feature](../e2e_test/features/bazaar/bazaar_subscription.feature), scenario `Subscribe and unsubscribe from a Bazaar notebook`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** subscribing, browsing the subscribed notebook, and unsubscribing through the web UI.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### book-layout-ai-preview-journey

- **Locations / scope:** [ai_reorganize_layout.feature](../e2e_test/features/book_reading/ai_reorganize_layout.feature), scenario `AI reorganize opens preview dialog and applies on confirm`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** viewing an AI layout suggestion and applying it after confirmation in the browser.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### circle-invitation-new-user-journey

- **Locations / scope:** [creating_circles.feature](../e2e_test/features/circles/creating_circles.feature), scenario `New user joins via circle invitation`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** new-user identification, profile creation, and joining a circle through its invitation.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### cli-saved-access-token-journey

- **Locations / scope:** [cli_access_token.feature](../e2e_test/features/cli/cli_access_token.feature), scenario `Saved access token allows recall status`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** saving a token in the interactive CLI and using it for recall status.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### cli-existing-note-git-journeys

- **Locations / scope:** [cli_notebook_existing_note_edits.feature](../e2e_test/features/cli/cli_notebook_existing_note_edits.feature), entire feature.
- **Observed cost:** 2026-09-19 focused runs reduced the feature from 6 scenarios / ~42s to 4 scenarios / ~21s; three consecutive passes were recorded. Exact command and environment were not retained in the commit summary.
- **Unique protection:** real clone, commit, pull, rebase, and publish subprocess protocol behavior.
- **Alternatives attempted:** duplicate single-commit and single-file-rebase journeys were retired after identifying surviving richer E2E proof.
- **Disposition / rationale:** profile-only exclusion — existing feature tag for the remaining real Git protocol cost.
- **Date:** 2026-09-19.
- **Evidence / follow-up:** [measured experiment and exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/c70ad55a6a); current tag in the linked feature. These are historical observations, not current timings.

### mcq-creation-journey

- **Locations / scope:** [mcq_management.feature](../e2e_test/features/note_creation_and_update/mcq_management.feature), scenario `Add an MCQ to a note`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** creating a multiple-choice question and seeing its stem and correct choice in the note's question list.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### live-audio-real-transcription-journey

- **Locations / scope:** [record_live_audio_with_real_open_ai_service.feature](../e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature), entire feature.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** browser recording and transcription appended to a note through the real OpenAI service.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing feature tag for known necessary live-recording/transcription cost.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/9edb1959c3); current tag in the linked feature.

### recall-browse-and-resume-journey

- **Locations / scope:** [browse_answer_and_notes_while_recalling.feature](../e2e_test/features/recall/browse_answer_and_notes_while_recalling.feature), scenario `Browse notes while recalling and come back`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** browsing notes and resuming an in-progress recall session through the web UI.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### wikidata-real-association-journey

- **Locations / scope:** [associate_wikidata.feature](../e2e_test/features/wikidata/associate_wikidata.feature), scenario `Associate note via real Wikidata when labels differ`.
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** real Wikidata association and confirmation when the suggested label differs.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the real-service journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

### wikidata-person-content-journey

- **Locations / scope:** [associate_wikidata_person_entries.feature](../e2e_test/features/wikidata/associate_wikidata_person_entries.feature), Scenario Outline `Creating a person note from Wikidata fills birthday and country` (both example rows).
- **Observed cost:** no numeric timing or measurement conditions retained with the exclusion decision.
- **Unique protection:** creating person notes whose content includes country and date of birth from Wikidata data.
- **Alternatives attempted:** not recorded with the original tag decision.
- **Disposition / rationale:** profile-only exclusion — existing scenario tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

## Transferred obligations

None.
