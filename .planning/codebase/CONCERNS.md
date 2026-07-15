# Codebase Concerns

**Analysis Date:** 2026-07-15

## Tech Debt

**Oversized modules (past the 250-line cohesion guideline):**
- Issue: Many production files far exceed the post-change-refactor 250-line split guideline, concentrating shotgun-surgery risk and slowing navigation.
- Files:
  - `backend/src/main/java/com/odde/doughnut/services/book/EpubStructureExtractor.java` (~890 lines)
  - `backend/src/main/java/com/odde/doughnut/services/book/BookService.java` (~703 lines)
  - `frontend/src/store/StoredApiCollection.ts` (~592 lines)
  - `e2e_test/step_definitions/note.ts` (~930 lines)
  - `e2e_test/start/pageObjects/notePage.ts` (~725 lines)
  - `frontend/src/pages/FolderPage.vue` (~481 lines)
  - `frontend/src/components/book-reading/BookReadingContent.vue` (~474 lines)
  - `backend/src/main/java/com/odde/doughnut/controllers/NotebookController.java` (~450 lines)
  - `backend/src/main/java/com/odde/doughnut/services/focusContext/FocusContextRetrievalService.java` (~403 lines)
- Impact: Changes to notes, books, or search touch megamodules; regressions are harder to localize; agents and humans both burn context reading unrelated behavior.
- Fix approach: Split by capability (EPUB parse vs attach orchestration; note store mutations vs reads; note E2E steps by feature folder) when the next behavior change touches these files — do not big-bang refactor.

**HTTP security is permit-all; auth is manual per controller:**
- Issue: Spring Security allows almost every request at the filter chain (`anyRequest().permitAll()`), with CSRF disabled. Authorization is enforced only when controllers call `AuthorizationService`.
- Files: `backend/src/main/java/com/odde/doughnut/configs/CommonConfiguration.java`, `backend/src/main/java/com/odde/doughnut/configs/ProductionConfiguration.java`, `backend/src/main/java/com/odde/doughnut/services/AuthorizationService.java`
- Impact: Any new endpoint that forgets `assertAuthorization` / `assertLoggedIn` / `assertReadAuthorization` is publicly reachable. This is a recurring footgun, not a one-off bug.
- Fix approach: Prefer method-security or a default-deny HTTP policy for `/api/**` with explicit permit lists; add controller-review checklist / static guard for missing auth calls on mutating endpoints.

**Unauthenticated OpenAI-cost endpoint:**
- Issue: `AiAudioController.audioToText` has no `AuthorizationService` usage; Spring permits anonymous POSTs to `/api/audio/audio-to-text`, which calls OpenAI transcription/completion.
- Files: `backend/src/main/java/com/odde/doughnut/controllers/AiAudioController.java`, caller `frontend/src/components/notes/widgets/NoteAudioTools.vue`
- Impact: Cost abuse and DoS via expensive AI calls if the API is reachable without a session.
- Fix approach: Add `assertLoggedIn()` (and preferably note-scoped auth if audio is bound to a note) before any OpenAI call; add a controller test that anonymous access returns 401/403.

**Unauthenticated attachment image serving:**
- Issue: `AttachmentController` serves image bytes by numeric image id with no ownership check.
- Files: `backend/src/main/java/com/odde/doughnut/controllers/AttachmentController.java`, `backend/src/main/java/com/odde/doughnut/entities/Image.java`
- Impact: IDOR for note images if ids are guessable or leaked; private notebook images may be readable without login.
- Fix approach: Resolve image → note → `assertReadAuthorization(note)` before returning bytes; cover with a controller test for foreign-notebook denial.

**E2E god objects:**
- Issue: Note and book-reading Cucumber glue / page objects accumulate every scenario’s helpers in a few files.
- Files: `e2e_test/step_definitions/note.ts`, `e2e_test/start/pageObjects/notePage.ts`, `e2e_test/step_definitions/book_reading.ts`, `e2e_test/start/pageObjects/bookReadingPage.ts`
- Impact: Merge conflicts, unclear ownership, slow discovery of the right helper.
- Fix approach: Continue splitting by feature (pattern already used for search steps in `ongoing/link-target-recent-mode.md`); keep page objects feature-scoped.

**Derived index coherence (wiki title / property / alias):**
- Issue: Note content properties live as YAML frontmatter; discoverability depends on derived tables refreshed via `WikiTitleCacheService.refreshForNote` and related backfills. Missed refresh sites recreate assimilation/search bugs.
- Files: `backend/src/main/java/com/odde/doughnut/services/WikiTitleCacheService.java` (and call sites in `NoteService`, `NoteConstructionService`, `TextContentController`, wiki-link rewrite), `backend/src/main/java/com/odde/doughnut/services/NotePropertyIndex*.java`, `ongoing/note-property-index-and-backfill.md`
- Impact: Stale assimilation queues, wrong wiki resolution, skipped property trackers after content edits if a write path skips refresh.
- Fix approach: Keep one refresh boundary for content saves; when adding a write path, always call the same refresh seam; prefer integration tests that assert index rows after content PATCH.

## Known Bugs

**Nested delete + undo recovery (`@ignore`):**
- Symptoms: Scenario for deleting a note then its parent and undoing both is skipped in CI.
- Files: `e2e_test/features/note_creation_and_update/note_deletion.feature` (scenario tagged `@ignore`)
- Trigger: Delete child, delete parent, undo parent, undo child.
- Workaround: Avoid relying on multi-level undo recovery in product flows until re-enabled.

**EPUB reader E2E suite fully skipped:**
- Symptoms: Entire `epub_book.feature` is tagged `@ignore`, so CI does not prove EPUB attach/navigation/resume.
- Files: `e2e_test/features/book_reading/epub_book.feature`
- Trigger: Any EPUB reading regression.
- Workaround: Rely on backend/unit coverage and manual checks; un-`@ignore` when Cypress EPUB scenarios are stable.

**Quill rich-editor wiki-link selection crash class:**
- Symptoms: Replacing `quill.root.innerHTML` while the browser selection still points at detached text nodes can throw `Cannot read properties of null (reading 'offset')` inside Quill selection normalization.
- Files: `frontend/src/components/form/QuillEditor.vue` (`updateQuillContent` still assigns `root.innerHTML`), investigation `ongoing/rich-wiki-link-quill-selection-investigation.md`
- Trigger: Completing `[[wiki]]` tokens / refreshing wiki link HTML while a native selection exists in the editor.
- Workaround: Avoid broad HTML root replacement for wiki-link-only transitions; use Quill APIs that preserve selection (per investigation). Add the documented browser-mode reproduction before changing this path.

## Security Considerations

**Application-level auth only:**
- Risk: Missing auth on a single controller method exposes user data or expensive side effects.
- Files: Controllers under `backend/src/main/java/com/odde/doughnut/controllers/` without `AuthorizationService` include at least `AiAudioController.java`, `AttachmentController.java`, `WikidataController.java`, `InstallController.java`, `CurrentUserInfoController.java`
- Current mitigation: Most note/notebook controllers call `AuthorizationService`; testability APIs are `@Profile({"e2e", "test"})` in `backend/src/main/java/com/odde/doughnut/testability/TestabilityRestController.java`
- Recommendations: Default-deny for `/api/**`; require login for Wikidata proxy and AI audio; authorize attachments; keep testability out of prod profiles (already profile-gated — verify prod packaging never includes `e2e`/`test`).

**CSRF disabled in all profiles:**
- Risk: Browser session cookies can be abused by cross-site POST if cookie auth is used without other mitigations.
- Files: `ProductionConfiguration.java`, `NonProductConfiguration.java`
- Current mitigation: SPA + API design; OAuth2 login in prod
- Recommendations: Revisit CSRF tokens or SameSite cookie policy when session-cookie auth is in use for mutating APIs.

**Secrets management:**
- Risk: Mis-handling of git-secret / GPG collaborators exposes production credentials.
- Files: `docs/secrets_management.md`, git-secret managed files (existence only — do not commit plaintext)
- Current mitigation: git-secret + GnuPG collaborator list
- Recommendations: Follow `docs/secrets_management.md` for add/remove collaborators; never commit revealed secrets.

## Performance Bottlenecks

**Embedding maintenance scans every notebook every 5 minutes:**
- Problem: Prod job iterates `notebookRepository.findAll()` then indexes each notebook.
- Files: `backend/src/main/java/com/odde/doughnut/services/EmbeddingMaintenanceJob.java`, `backend/src/main/java/com/odde/doughnut/services/NotebookIndexingService.java`
- Cause: Full catalog scan on a fixed cron (`0 */5 * * * *`); each notebook may still call OpenAI for notes needing embeddings.
- Improvement path: Query only notebooks with dirty notes; batch/limit work per tick; keep ShedLock/pool sizing aligned with question-generation maintenance (`ongoing/batch-question-generation-prod-reliability.md` already raised pool size for scheduler contention).

**EPUB structure extraction cost and size:**
- Problem: Large EPUB packages stress a monolithic extractor with XML DOM walks and size caps.
- Files: `backend/src/main/java/com/odde/doughnut/services/book/EpubStructureExtractor.java`, helpers in `EpubPackageIo`
- Cause: Full spine/TOC parse into `BookBlock` trees in one class.
- Improvement path: Split parse stages; stream large spines; keep fail-fast for DRM/unsupported packages; measure attach latency on larger fixtures.

**Frontend Vitest browser mode + pinned test-utils:**
- Problem: Browser-mode tests are slower than jsdom; Vue Test Utils is held below 2.4.9+ due to attachTo cleanup bugs.
- Files: `frontend/tests/setupVitest.ts`, frontend Vitest browser config
- Cause: Real browser rendering preference (project rule) plus upstream VTU issue.
- Improvement path: Re-bump `@vue/test-utils` once https://github.com/vuejs/test-utils/pull/2830 ships; keep avoiding `getByRole` (slow visibility) per `.cursor/rules/frontend-testing.mdc`.

**External OpenAI E2E is intentionally expensive:**
- Problem: Real OpenAI transcription feature is blacklisted from test-optimization and is slow/flaky by nature.
- Files: `e2e_test/features/note_creation_and_update/record_live_audio_with_real_open_ai_service.feature`, `ongoing/test-optimization-blacklist.md`
- Cause: Live network + polling
- Improvement path: Keep mocked coverage in `record_live_audio.feature`; run real-service spec sparingly outside the hot path.

## Fragile Areas

**Rich Quill editor + wiki links:**
- Files: `frontend/src/components/form/QuillEditor.vue`, `frontend/src/components/form/replaceWikiLinksInHtml.ts`, `frontend/src/components/form/quillHtmlToMarkdown.ts`, `ongoing/rich-wiki-link-quill-selection-investigation.md`
- Why fragile: Selection depends on browser native ranges; HTML root replacement detaches nodes Quill still inspects; markdown ↔ HTML round-trip must preserve `data-wiki-title` / display.
- Safe modification: Prefer Quill delta/API mutations for wiki-link upgrades; add browser-mode reproduction from the investigation before changing `updateQuillContent`.
- Test coverage: Unit helpers alone miss the crash; require Vitest browser tests with real Quill + selection.

**Book reading (PDF + EPUB dual path):**
- Files: `frontend/src/components/book-reading/PdfBookViewer.vue`, `EpubBookViewer.vue`, `BookReadingContent.vue`, `BookReadingBookLayout.vue`, `backend/.../services/book/BookService.java`, `BookFormat.java`
- Why fragile: Shared shell (position, records, layout) with format-specific viewers; EPUB E2E currently `@ignore`; PDF attach is CLI/MinerU while EPUB attaches in-browser.
- Safe modification: Change shared reader-shell seams carefully; pin format branches with controller + mounted tests; un-ignore EPUB E2E before large EPUB UX changes.
- Test coverage: Gap on full EPUB Cypress path; skim/skip and reading-record behavior rely more on unit/controller tests (`ongoing/book-reading-phase-4-skim-skip-plan.md`).

**Property memory trackers + frontmatter edits:**
- Files: `frontend/src/components/form/RichFrontmatterProperties.vue`, `frontend/src/components/notes/core/TextContentWrapper.vue`, property guard composables, `ongoing/property-tracker-guard.md`
- Why fragile: Debounced markdown save vs rich row edits must both run confirmation; race between async guard, debounce, and assimilation `note-info` refresh caused E2E flakiness in early attempts.
- Safe modification: Keep guard in `TextContentWrapper.changerInner` before save; invalidate note-info cache after tracker mutations; chain Cypress confirm + `pageIsNotLoading` before flush.
- Test coverage: Prefer targeted E2E for delete/rename; watch for async bleed between Vitest mocks.

**Assimilation queue ordering / caps:**
- Files: `backend/src/main/java/com/odde/doughnut/services/AssimilationService.java`, `AssimilationUnitSource` implementations, `ongoing/assimilation-queue-restructure.md`
- Why fragile: Unified ordering and subscription daily budgets intertwine note and property units; easy to reintroduce count/queue drift or subscription-cap bugs.
- Safe modification: Extend `AssimilationServicePropertyUnitsTest` / ordering regression tests when changing sources; do not reintroduce eager in-memory sort of full streams.
- Test coverage: Stronger after restructure, but still sensitive to JPQL order vs `AssimilationUnit.ORDER`.

**Generated OpenAPI client sync:**
- Files: `packages/generated/doughnut-backend-api/**`, `open_api_docs.yaml`, skill `.cursor/skills/generate-api-client/SKILL.md`
- Why fragile: Backend DTO/controller changes require `pnpm generateTypeScript`; hand-edits are forbidden; OpenAPI approval tests fail if skipped.
- Safe modification: Regenerate after every controller/DTO change; never manually fix generated whitespace (`scripts/check_diff_whitespace.sh`).
- Test coverage: Backend OpenAPI approval tests; frontend breaks at compile/runtime if stale.

**Testability surface:**
- Files: `backend/src/main/java/com/odde/doughnut/testability/TestabilityRestController.java` (~445 lines)
- Why fragile: Powerful DB clean / time travel / service URL override APIs; safe only under `e2e`/`test` profiles.
- Safe modification: Never widen profiles; keep E2E page objects as the only callers; avoid production dependency on testability beans.
- Test coverage: Profile annotations + packaging checks.

## Scaling Limits

**Notebook embedding maintenance:**
- Current capacity: Cron every 5 minutes over all notebooks; OpenAI embedding generation for dirty notes.
- Limit: Large notebook counts or large note bodies increase per-tick OpenAI and DB work; can contend with other scheduled jobs if pool size regresses.
- Scaling path: Dirty-notebook query; rate limits; optional ANN vector index on Cloud SQL (`ongoing/semantic_search_implementation.md` / `docs/tech_stack.md`).

**Question-generation batch job:**
- Current capacity: Hourly maintenance with ShedLock for multi-instance safety (phases in `ongoing/batch-question-generation-prod-reliability.md` marked done).
- Limit: Stuck `SUBMITTED` batches if polling/scheduling regresses; dual-instance double-submit without ShedLock.
- Scaling path: Keep durable run-state + ShedLock; monitor admin resume path; avoid shrinking `spring.task.scheduling.pool.size` in prod.

**EPUB attach / parse:**
- Current capacity: Size-capped package reads (`MAX_*_BYTES` in `EpubPackageIo`).
- Limit: Very large or exotic EPUBs fail or time out; fixed-layout / DRM intentionally unsupported.
- Scaling path: Clearer fail-fast errors; deferred formats per `ongoing/book-reading-epub-support-plan.md`.

## Dependencies at Risk

**@vue/test-utils < fixed attachTo cleanup:**
- Risk: Stuck on versions that break Testing Library `render()` / double-unmount; cannot take 2.4.9+ until upstream fix ships.
- Impact: Frontend test reliability and upgrade friction.
- Migration plan: Track VTU PR #2830; re-bump and remove the TODO in `frontend/tests/setupVitest.ts`.

**Quill editor selection model:**
- Risk: Quill APIs inspect native selection even for `SILENT` mutations; DOM HTML replacement is unsafe.
- Impact: Production editor crashes during wiki-link completion.
- Migration plan: Selection-safe wiki-link upgrade path documented in `ongoing/rich-wiki-link-quill-selection-investigation.md`; consider Quill major upgrades only with browser-mode selection suites.

**OpenAI Java SDK + batch APIs:**
- Risk: External API shape/latency changes; cost and rate limits.
- Impact: Question generation, embeddings, audio transcription, conversations.
- Migration plan: Keep batch reliability ops docs (`docs/question-generation-batch-operations.md`); isolate SDK usage behind existing service boundaries (`NoteQuestionGenerationService`, `EmbeddingService`, `OtherAiServices`).

**MinerU / CLI PDF attach pipeline:**
- Risk: External Python subprocess for PDF outline/layout (`cli/src/commands/mineruOutline/mineruOutlineSubprocess.ts` is large and environment-sensitive).
- Impact: PDF book attach fails if MinerU/python env drifts; E2E stubs already note generous timeouts.
- Migration plan: Keep CLI attach as the only PDF path; pin fixtures/stubs; document Cloud VM vs Nix differences via `cloud-vm-setup` skill.

## Missing Critical Features

**Obsidian ↔ Doughnut sync (user stories ahead of implementation):**
- Problem: Broad sync/wiki-link product stories in `ongoing/obsidian_sync.md` are not fully productized as a complete CLI sync contract.
- Blocks: Reliable bidirectional Obsidian workflows (dry-run sync, deletion rules, parent property sync).

**Semantic search maturity:**
- Problem: `ongoing/semantic_search_implementation.md` still describes limitations of literal search and a vector roadmap; embedding maintenance exists, but product search UX/quality may still lag the proposed design.
- Blocks: Content-aware fuzzy search and relevance ranking as specified in that plan.

**EPUB CI confidence:**
- Problem: EPUB feature file ignored in CI.
- Blocks: Safe continuous delivery of EPUB reading improvements without manual regression.

**Mobile soft-keyboard completeness:**
- Problem: Primer mechanism shipped for several flows (`SoftKeyboardPrimer.vue`, `primeSoftKeyboard` in `focusTarget.ts`), but remaining tap-to-reveal surfaces may still need wiring; real keyboard visibility is manual-only (`ongoing/mobile-soft-keyboard-on-tap.md`).
- Blocks: Consistent mobile editing UX across all dialogs.

## Test Coverage Gaps

**EPUB end-to-end (High):**
- What's not tested: Full attach → navigate → resume → control panel in Cypress CI.
- Files: `e2e_test/features/book_reading/epub_book.feature` (`@ignore`)
- Risk: EPUB regressions ship unnoticed.
- Priority: High

**Nested note delete/undo (Medium):**
- What's not tested: Parent/child delete undo recovery scenario.
- Files: `e2e_test/features/note_creation_and_update/note_deletion.feature`
- Risk: Data-loss or orphaned tree states after undo.
- Priority: Medium

**Anonymous abuse of AI audio (High):**
- What's not tested: Explicit denial for unauthenticated `/api/audio/audio-to-text`.
- Files: `AiAudioController.java` (no auth assertion found)
- Risk: OpenAI cost exposure.
- Priority: High

**Attachment IDOR (Medium):**
- What's not tested: Foreign-user image access denial.
- Files: `AttachmentController.java`
- Risk: Private image leakage.
- Priority: Medium

**Quill selection crash reproduction (High for editor work):**
- What's not tested: Documented browser-mode reproduction for `innerHTML` + selection may not be permanently locking the safe path.
- Files: `QuillEditor.vue`, investigation doc
- Risk: Editor crash regressions on wiki-link typing.
- Priority: High when editing rich content

**Real OpenAI audio path (Low for CI, Medium for prod confidence):**
- What's not tested in normal CI: Live OpenAI transcription feature (blacklisted from optimization; external dependency).
- Files: `record_live_audio_with_real_open_ai_service.feature`
- Risk: Prod-only transcription failures.
- Priority: Low in CI; Medium for release confidence

---

*Concerns audit: 2026-07-15*
