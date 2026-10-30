# Test Optimization Candidates

## Latest pass

- **Command / scope:** `CURSOR_DEV=true nix develop -c pnpm frontend:test`; complete frontend unit suite, 2026-09-26. macOS, local Nix shell, warm
  pnpm/Vite caches, default file parallelism, headless Chromium, no filters or
  retries; worker count was not recorded.
- **Revision:** baseline `8d79d96a09`; optimized `8df4a87262`; results recorded at `fe990eff58`.
- **Before / after:** executed cases 1937 / 1900; files 346 / 295. Alternating whole-suite Vitest elapsed durations: 47.28/46.61/55.84s before and
  37.49/42.24/39.58s after (means 49.9s / 39.8s). Whole-command wall time: initial baseline 75.8s; comparable final before/after totals unavailable.
- **Outcome:** incomplete measurement — runner-reported elapsed time improved ~20% through responsibility-based sibling-spec merges and shared setup. Removed cases
  were named duplicates or folded into `it.each` rows with surviving proof.
- **Limits:** alternating runs passed at load averages 4.6–12.4; whole-command wall-time improvement is unmeasured. This record was migrated on 2026-10-30
  using retained evidence; migration ran no new optimization pass.
- **Evidence:** [completed pass and measurements](https://github.com/nerds-odd-e/doughnut/blob/fe990eff58/.planning/quick/041-faster-frontend-unit-tests/PLAN.md).

## Pending candidates

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
- **Disposition / rationale:** pending — evaluate proposal-crafting duplication separately from the already shared plain tree read.
- **Date:** 2026-09-20; reader names updated 2026-09-24.
- **Human decision needed:** owner to choose whether a `GitBundleTestWriter` mirror merits a sweep of the 51 callers or the local idiom is preferred.
- **Evidence / follow-up:** [attachment-work observation](https://github.com/nerds-odd-e/doughnut/commit/6b906462dd),
  [reader-name update](https://github.com/nerds-odd-e/doughnut/commit/c50a90c159). Establish a focused timing baseline before proposing a speedup.

### e2e-vite-first-page-load

- **Locations / scope:** local `pnpm cy:run`, Vite-served frontend; 14 CLI features in the 2026-09-25 pass.
- **Observed cost:** ~0.7–1s for the first page load of each web-using scenario; a production build was estimated to save ~15–25s across that scope.
- **Unique protection:** frontend HMR in local E2E batches.
- **Alternatives attempted:** production-build serving was proposed and declined
  by the owner on 2026-09-25 when the CLI E2E pass stopped at ~41% improvement.
- **Disposition / rationale:** pending owner decision; retain the owner's
  existing decision to keep local HMR. The estimated saving was not measured.
- **Date:** 2026-09-25.
- **Human decision needed:** owner to authorize trading local `cy:run` HMR for
  a built frontend before this alternative can be attempted.
- **Evidence / follow-up:** [CLI E2E pass](https://github.com/nerds-odd-e/doughnut/blob/e2f9929fe9/.planning/quick/032-halve-cli-e2e-time/PLAN.md),
  [candidate recording](https://github.com/nerds-odd-e/doughnut/commit/753dbd9b5d).

### e2e-batch-startup

- **Locations / scope:** fixed startup of a local `pnpm cy:run` batch.
- **Observed cost:** ~33s per batch on 2026-09-25: SUT 13.5s (backend boot ~9s,
  Gradle compile check, mocks, Vite), Cypress launch 2.7s, ~1.2s per spec load.
- **Unique protection:** a fresh owned stack per batch.
- **Alternatives attempted:** none recorded.
- **Disposition / rationale:** pending — retain the recorded startup-cost
  candidate for investigation with fresh-stack protection intact.
- **Date:** 2026-09-25.
- **Evidence / follow-up:** [CLI E2E pass](https://github.com/nerds-odd-e/doughnut/blob/e2f9929fe9/.planning/quick/032-halve-cli-e2e-time/PLAN.md),
  [candidate recording](https://github.com/nerds-odd-e/doughnut/commit/753dbd9b5d).

### cli-git-lfs-hook-installation

- **Locations / scope:** CLI E2E Git checkouts with attachments and git-lfs 3.7.1.
- **Observed cost:** on 2026-09-25, commits cost ~213ms rather than ~15ms after
  the first real git-lfs command reinstalls `post-checkout`, `post-commit`,
  `post-merge`, and `pre-push` hooks despite filter installation with `--skip-repo`.
- **Unique protection:** none recorded for these hooks; the observation states
  that Donut uses neither LFS locking nor `git push`.
- **Alternatives attempted:** no change attempted; no setting to prevent hook
  reinstallation was found in the installed git-lfs 3.7.1.
- **Disposition / rationale:** pending — retain the hook overhead for a later
  measured experiment.
- **Date:** 2026-09-25.
- **Evidence / follow-up:** [CLI E2E pass](https://github.com/nerds-odd-e/doughnut/blob/e2f9929fe9/.planning/quick/032-halve-cli-e2e-time/PLAN.md),
  [candidate recording](https://github.com/nerds-odd-e/doughnut/commit/753dbd9b5d).

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
- **Disposition / rationale:** pending — preserve this legacy candidate and
  its protected profile differences for later assessment.
- **Date:** 2026-09-26.
- **Evidence / follow-up:** [backend re-profile](https://github.com/nerds-odd-e/doughnut/commit/db643ef844),
  [shared-context guidance](https://github.com/nerds-odd-e/doughnut/commit/97ce679b18).

### frontend-sibling-spec-setup

- **Locations / scope:** the 2026-09-26 pass left ~25 families of 2–3 sibling
  frontend specs; BookReadingPage's stub-book/mount/wait-for-PDF setup repeated
  across about six support files, and `BookReadingPage.snap.spec.ts` was 404 lines.
- **Observed cost:** retained merges saved ~70–250ms per removed file in focused
  runs. The remaining opportunities were unpicked and have no measured saving.
- **Unique protection:** each family's distinct component behavior, harness,
  mock boundaries, and real/fake timer requirements.
- **Alternatives attempted:** responsibility-based merges reduced the measured
  suite from 346 to 295 files; incompatible file-level `vi.mock` choices and
  the then-applicable 250-line refactor limit bounded those merges.
- **Disposition / rationale:** pending — carry the legacy note's unpicked
  opportunities into the problem list; establish current family costs before
  selecting work. Individual 13–34ms test bodies alone were below file-cost noise.
- **Date:** 2026-09-26.
- **Evidence / follow-up:** [completed frontend pass, remaining families, and merge constraints](https://github.com/nerds-odd-e/doughnut/blob/fe990eff58/.planning/quick/041-faster-frontend-unit-tests/PLAN.md).

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

## Profile-only exclusions

Existing mechanism: `@skipOptimizationDueToKnownNecessarySlowness` on a
Scenario, Scenario Outline, or Feature. For `pnpm cy:run --spec`, leave tagged
features out of the spec list and subtract tagged scenarios inside selected
features from the profile; the wrapper drops `--expose tags=...`. Normal
verification and CI still run them. See
[E2E profiling guidance](../.agents/skills/e2e-authoring/SKILL.md#test-optimization-profiling).
The entries below inventory existing tags; their original decisions are retained.

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
- **Disposition / rationale:** profile-only exclusion — existing outline tag for cost judged inherent to the journey.
- **Date:** 2026-07-23.
- **Evidence / follow-up:** [original exclusion decision](https://github.com/nerds-odd-e/doughnut/commit/f975c3dbb6); current tag in the linked feature.

## Transferred obligations

None.
