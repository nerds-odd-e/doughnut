# Backend unit tests → "small test" style

**Status:** in progress (Phase 25a done; 25b planned)
**Type:** test renovation (no product behavior change)
**Verify each phase:** `CURSOR_DEV=true nix develop -c pnpm backend:test_only`
**Style:** `.cursor/rules/unit-testing.mdc` + `.cursor/rules/backend-testing.mdc`
**Resume:** this `PLAN.md` progress log only — **do not edit** trunk `.planning/STATE.md` (parallel trunk-based work).

---

## Rubric (apply every Behavior phase)

For each file in the phase file list:

| Check | Action |
|-------|--------|
| **Stable boundary** | Prefer controller (or other intentional domain contract). Do **not** add a test class per internal collaborator. If a service test only re-covers a path already exercised via a controller with realistic `makeMe` data, **move unique assertions up** and **delete** the redundant service test. |
| **Keep when domain-stable** | Pure algorithms, validators, serialization, and job/pipeline services that *are* the contract (e.g. QuestionGeneration batch internals, health rules) stay at that boundary — renovate style in place. |
| **Mocks** | Only true externals. Backend defaults: `OpenAIClient` via `OpenAiStructuredResponseMock`; `HttpClientAdapter` where used. Drop collaborator mocks/`@InjectMocks`-style isolation. GCS/third-party storage mocks OK when the boundary is that adapter. |
| **Focused assertions** | One behavior per test. Canonical shape once; siblings assert **delta only**. Drop repeated “full payload” asserts across related cases. |
| **Concise makeMe** | Prefer `notebookOwnedBy` / ownership inheritance; drop unused user/notebook wiring; prefer builder APIs over post-construction mutation; **extend MakeMe** when the same verbose shape repeats (Structure sub-slice before finishing the phase if blocked). |
| **No production drive-bys** | Do not refactor production for “testability” beyond MakeMe/builders unless the phase cannot complete otherwise — then Jidoka. |

**Phase done when:** listed files comply with the rubric; dead/redundant test code removed; `backend:test_only` green; plan status updated.

**Sizing:** If a phase blows past ~10 minutes wall-clock including tests, stop, revert WIP for that attempt, split the remaining file list into a sub-`*-PLAN.md` in this directory, and continue.

**Naming:** Do not rename test files/classes after phase numbers — keep capability names.

---

## Design decisions

1. **Controller-first consolidation** — Prefer fewer, higher-boundary tests over polishing duplicate service suites.
2. **Batch/job services stay service-level** — QuestionGeneration batch (~32 files), health rules, book storage adapters: renovate style; do not force through HTTP controllers.
3. **Large controller files = multiple phases** — Split by `@Nested` groups so each phase stays agent-sized.
4. **MakeMe extensions are Structure, only for the next Behavior** — No speculative builder framework.
5. **Stop-safe** — After any phase the suite is green; renovated cohorts are better; remaining debt is still listed below.

---

## Phases

### Phase 1 — Algorithms package (light pass)
- **Status:** done
- **Type:** Behavior
- **Observable:** `algorithms/` tests follow rubric (boundary already mostly correct; fix assertion/makeMe noise only).
- **Files:** all under `backend/src/test/java/com/odde/doughnut/algorithms/` (~17).
- **Done when:** rubric applied; suite green.

### Phase 2 — Validators, utils, factoryServices, integration
- **Status:** done
- **Type:** Behavior
- **Observable:** small pure-contract packages follow rubric.
- **Files:**
  - `validators/*`
  - `utils/**`
  - `factoryServices/*`
  - `integration/*`
- **Done when:** rubric applied; suite green.

### Phase 3 — Configs
- **Status:** done
- **Type:** Behavior
- **Observable:** `configs/` tests follow rubric (keep as framework contracts).
- **Files:** `backend/src/test/java/com/odde/doughnut/configs/*`
- **Done when:** rubric applied; suite green.

### Phase 4 — Entities and entity repositories
- **Status:** done
- **Type:** Behavior
- **Observable:** entity/repo tests follow rubric; illicit mocks removed where possible.
- **Files:** `backend/src/test/java/com/odde/doughnut/entities/**`
- **Done when:** rubric applied; suite green.

### Phase 5 — Controllers: note show / stats / spelling
- **Status:** done
- **Type:** Behavior
- **Observable:** note show / note-info / spelling controller tests follow rubric.
- **Files / scope:**
  - `controllers/NoteControllerShowTests.java`
  - `controllers/NoteControllerNoteInfoTests.java`
  - `controllers/NoteControllerVerifySpellingTests.java`
  - (split from oversized `NoteControllerTests`; leave delete/upload/graph/AI for Phase 6)
- **Done when:** those files renovated; suite green.

### Phase 6 — Controllers: note delete / upload / graph / AI context
- **Status:** done
- **Type:** Behavior
- **Observable:** remaining note-controller tests follow rubric.
- **Files / scope:**
  - `NoteControllerDeleteReduceToSourceTests.java`
  - `NoteControllerDeleteTests.java`
  - `NoteControllerUploadNoteImageTests.java`
  - `NoteControllerUpdateNoteRecallSettingTests.java`
  - `NoteControllerGraphTests.java`
  - `NoteControllerAiContextMarkdownTests.java`
- **Done when:** those files complete vs rubric; suite green.

### Phase 7 — Controllers: text content
- **Status:** done
- **Type:** Behavior
- **Files:**
  - `controllers/TextContentControllerTestBase.java`
  - `controllers/TextContentControllerUpdateNoteTitleTests.java`
  - `controllers/TextContentControllerUpdateNoteTitleInboundWikiReferencesTests.java`
  - `controllers/TextContentControllerUpdateNoteContentTests.java`
  - `controllers/TextContentControllerRejectInvalidAuthoredAliasesTests.java`
- **Done when:** rubric applied; suite green.

### Phase 8 — Controllers: note satellites
- **Status:** done
- **Type:** Behavior
- **Files:**
  - `RelationControllerTests.java`, `RelationControllerMoveNoteToFolderTests.java`
  - `AttachmentControllerTests.java`
  - `NoteControllerRecentNotesTests.java`, `NoteTitlePersistenceTest.java`
  - `FolderNotebookParentNameUniquePersistenceTest.java`
  - `SoftDeletedTitleConflictMvcTest.java`
- **Done when:** rubric applied; suite green.

### Phase 9 — Controllers: notebook folder create / rename / dissolve
- **Status:** done
- **Type:** Behavior
- **Files / scope:**
  - `NotebookFolderCreateControllerTest.java`
  - `NotebookFolderRenameControllerTest.java`
  - `NotebookFolderDissolveControllerTest.java`
  - shared helpers: `NotebookFolderManagementControllerTestBase.java`
- **Done when:** those files renovated; suite green.

### Phase 10 — Controllers: notebook folder move
- **Status:** done
- **Type:** Behavior
- **Files / scope:**
  - `NotebookFolderMoveControllerTest.java` (same-notebook move / merge)
  - `NotebookFolderCrossNotebookMoveControllerTest.java`
  - `NotebookFolderCrossNotebookMoveMergeControllerTest.java`
  - `NotebookFolderMoveLinkRewriteControllerTest.java`
  - shared: `NotebookFolderManagementControllerTestBase.java`
- **Done when:** those files renovated; suite green.

### Phase 11 — Controllers: notebook CRUD / notes-folder / sharing / export / health / groups
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `NotebookCrudControllerTest.java`, `NotebookUpdateControllerTest.java`
  - `NotebookNoteCreateControllerTest.java`, `NotebookFolderListingControllerTest.java`, `NotebookFolderPageControllerTest.java`
  - `NotebookSharingGroupControllerTest.java`, `NotebookCatalogControllerTest.java`, `NotebookGroupControllerTest.java`
  - `NotebookExportControllerTest.java` (already clean)
  - `NotebookHealthControllerTest.java`
  - `NotebookRootNoteCreationWithWikidataTests.java` (wikidata-only; folder assignment covered by note-create)
  - base: `NotebookControllerTestBase.java` (`ownedNotebook` / `ownedFolder` lifted from folder-management base)
- **Done when:** rubric applied; suite green.

### Phase 12 — Controllers: notebook books (attach / retrieve / reading)
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `NotebookBooksAttachControllerTest.java`, `NotebookBooksAttachContentControllerTest.java`
  - `NotebookBooksRetrievalControllerTest.java`, `NotebookBooksBookFileControllerTest.java`
  - `NotebookBooksReadingPositionControllerTest.java`, `NotebookBooksGetReadingPositionControllerTest.java`
  - `NotebookBooksReadingRecordControllerTest.java`
  - base: `NotebookBooksControllerTestBase.java` (`textBlock` / `contentListAttachRequest`)
- **Done when:** rubric applied; suite green.

### Phase 13 — Controllers: notebook books (block content / depth)
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `NotebookBooksCreateBlockFromContentControllerTest.java`
  - `NotebookBooksSuggestLayoutControllerTest.java`, `NotebookBooksApplyLayoutControllerTest.java`
  - `NotebookBooksBlockDepthControllerTest.java`
  - bases: `NotebookBooksBlockControllerTestBase.java` (`blockByTitle`), `NotebookBooksLayoutReorganizationControllerTestBase.java` (`suggestionWithDepths` / `nestBAndCDepths`); reuse Phase 12 `NotebookBooksControllerTestBase` (`textBlock`, etc.)
- **Done when:** rubric applied; suite green.

### Phase 14 — Controllers: memory tracker
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `MemoryTrackerThresholdControllerTest.java` (wrong-answer threshold + auth)
  - `MemoryTrackerAskQuestionControllerTest.java`
  - `MemoryTrackerShowControllerTest.java` (show / recalled-note shape)
  - `MemoryTrackerTrackingControllerTest.java` (remove / re-enable / mark-as-recalled)
  - `MemoryTrackerRecentControllerTest.java` (recent trackers + recently recalled)
  - `MemoryTrackerRecallPromptsControllerTest.java` (list + delete unanswered)
  - `MemoryTrackerUpdatePropertyKeyControllerTest.java`
  - base: `MemoryTrackerControllerTestBase.java` (`ownedNote` / `ownedTracker` / `addWrongAnswers` / `renameTo`)
- **Done when:** rubric applied; suite green.

### Phase 15 — Controllers: recall prompts and recalls
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `RecallPromptAnswerQuizControllerTest.java` (answer quiz + wrong-answer curve)
  - `RecallPromptRegenerateControllerTest.java`
  - `RecallPromptContestControllerTest.java`
  - `RecallPromptAnswerSpellingControllerTest.java` (spelling answer / curve / auth)
  - `RecallPromptSpellingStemMaskingControllerTest.java`
  - `RecallPromptAccidentalMatchGradingTests.java`, `RecallPromptAccidentalMatchEdgeTests.java`
  - `RecallPromptOverlapTryAgainTests.java`
  - `RecallsControllerTests.java` (due recalling + previously answered)
  - base: `RecallPromptControllerTestBase.java` (`ownedNote` / `ownedSpellingTracker` / `mcqPrompt` / `spellingPrompt` / OpenAI stub)
- **Done when:** rubric applied; suite green.

### Phase 16 — Controllers: assimilation + search
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor):**
  - `AssimilationControllerTests.java`
  - `SearchControllerTests.java`, `SearchControllerWithinTests.java`
  - `SearchControllerAliasTests.java`, `SearchControllerSemanticTests.java`
  - base: `SearchControllerTestBase.java` (`ownedNote` / `searchTerm` / `searchTermInMyNotebooksOnly` / `searchTermWithoutGlobalScope`)
- **Done when:** rubric applied; suite green.

### Phase 17 — Controllers: AI
- **Status:** done
- **Type:** Behavior
- **Files / scope (post-refactor capability splits):**
  - `AiControllerTest.java` (models / suggest title)
  - `AiControllerNoteRefinementTest.java`, `AiControllerRemoveRefinementSuggestionTest.java`
  - `AiControllerExtractNotePreviewTest.java`, `AiControllerCreateExtractedNoteTest.java`
  - `AiControllerExtractNoteValidationTest.java`, `AiControllerExportRefinementLayoutRequestTest.java`
  - `AiAudioControllerTests.java`
  - `PredefinedQuestionControllerTests.java`, `PredefinedQuestionRefinementControllerTests.java`
  - support: `AiControllerExtractNoteTestSupport.java` (`EXTRACTABLE_CONTENT` / `nestedLayout` / `selectSingleLayoutItem` / `assertBadRequestContaining`)
- **Done when:** rubric applied; suite green. Keep `OpenAiStructuredResponseMock` usage.

### Phase 18a — Controllers: user / circle / bazaar / subscription
- **Status:** done
- **Type:** Behavior
- **Observable:** user, circle, bazaar, and subscription controller/DTO tests follow rubric.
- **Files / scope (post-refactor capability splits):**
  - `UserControllerTest.java` (profile), `UserTokenControllerTest.java`, `UserMenuDataControllerTest.java`, `UserRecallStatsControllerTest.java`
  - `CurrentUserInfoControllerTest.java`
  - `currentUser/CurrentUserFetcherFromRequestTest.java` (already rubric-clean; reviewed)
  - `CircleControllerTest.java`, `BazaarControllerTest.java`, `SubscriptionControllerTest.java`
  - `dto/NoteUpdateTitleDTOTest.java`, `dto/UserDTOTest.java`
- **Done when:** those files renovated; suite green.

### Phase 18b — Controllers: conversation / books / admin / remaining HTTP
- **Status:** done
- **Type:** Behavior
- **Observable:** remaining HTTP controller tests follow rubric.
- **Files / scope (post-refactor capability splits):**
  - `ConversationMessageControllerTest.java` (mark/reply/get/export), `ConversationListingControllerTest.java`, `ConversationStartControllerTest.java`, `ConversationAiReplyControllerTest.java`
  - base: `ConversationMessageControllerTestBase.java`
  - `BooksControllerTest.java`
  - `AdminUserControllerTest.java`, `AdminQuestionGenerationBatchControllerTest.java` (resume happy path folded in; deleted mock-heavy `AdminQuestionGenerationBatchControllerResumeTest`)
  - `UserQuestionGenerationBatchScheduleControllerTest.java`
  - `WikidataControllerTests.java`, `GlobalSettingsControllerTest.java`
  - `FailureReportControllerTest.java`, `InstallControllerTest.java` (already clean; reviewed)
  - `dto/DisplayNameTrimmingDeserializerTest.java`
- **Done when:** remaining controller tests renovated; suite green.

### Phase 19 — Services: assimilate / memory / recall — consolidate into controllers
- **Status:** done
- **Type:** Behavior
- **Observable:** unique coverage lives at controller (or intentional service contract); redundant service tests deleted or slimmed.
- **Deleted:**
  - `MemoryTrackerServiceTest.java` — assimilate/prompts/soft-delete lifted to AssimilationController + MemoryTrackerTracking/AskQuestion; prompt listing already at RecallPrompts controller
  - `RecallQuestionServiceTest.java` — recycle/generate lifted to `MemoryTrackerAskQuestionControllerTest`
- **Kept (domain-stable contracts; style renovated):**
  - `AssimilationServiceTestBase` + DailyCap / QueueOrdering / SubscriptionQueue / PropertyUnits / PropertyWikiLinkGate
  - `UnassimilatedPropertyServiceTest`
  - `RecallServiceWithSpacedRepetitionAlgorithmTest`
  - `RecallStatsServiceTest` + `RecallStatsTestFixtures` + `RecallStatsPerformanceTest` (aggregate algorithm + N+1 guard)
- **Done when:** no redundant service suites for this theme; suite green.

### Phase 20 — Services: note / wiki / embedding / property / alias
- **Status:** done
- **Type:** Behavior
- **Deleted:**
  - `NoteAutomationServiceTests` — null-title / empty-layout lifted to AiController* ; happy paths already at AI controllers
  - `NoteMotionServiceTest` — move-into-folder / peers / notebook-root covered by RelationController*
  - `WikiLinkRewriteServiceFolderNotebookMoveTest` — exact duplicate of NotebookFolderMoveLinkRewriteControllerTest
- **Slimmed:**
  - `NoteServiceTest` — keep selective restore-by-deletedAt contract; cascade soft-delete lifted to NoteControllerDeleteTests; destroy/tracker exclusion duplicates removed
- **Kept (domain-stable; style renovated + post-refactor splits ≤250):**
  - NoteRealm: `NoteRealmServiceTest`, `NoteRealmScopedMetadataTest`, `NoteRealmJsonSerializationTest`
  - Wiki: `WikiTitleCacheServiceTest`, `WikiTitleCacheTitleResolutionTest`, `WikiTitleCacheViewerQueriesTest`, `WikiLinkResolverYamlAndBodyIntegrationTest`
  - Alias/Property: `NoteAliasIndexServiceTest`, `NotePropertyIndexServiceTest`, `NotePropertyIndexWikiLinkTargetTest`, `NotePropertyIndexTargetNoteBackfillTest`, `NotePropertyTrackingBackfillTest`
  - Embedding: `EmbeddingServiceTest`, `EmbeddingServiceTests`, `NoteEmbeddingServiceTests`, `EmbeddingMaintenanceJobTests` (thin scheduled loop — collaborator mocks kept)
  - `NoteQuestionGenerationServiceTests` (OpenAI external mock; request-shape contract)
- **Deferred to Phase 25:** NotebookExport / NotebookReindexing / NotebookGroup
- **Done when:** rubric applied; suite green.

### Phase 21 — Services: focus context
- **Status:** done
- **Type:** Behavior
- **Kept (domain-stable contract for focus-context assembly; style renovated + capability splits ≤250):**
  - `FocusContextRetrievalTestBase` + `FocusContextRetrievalServiceTest` (focus note / outgoing / inbound / dedup)
  - `FocusContextRetrievalInboundSamplingTest`
  - `FocusContextRetrievalDepthTraversalTest`
  - `FocusContextRetrievalFolderSiblingTest`
  - `FocusContextMarkdownRendererTest`, `RetrievalConfigTest`
- **MakeMe:** `FolderBuilder.notebookOwnedBy` for folder-sibling fixtures
- **Note:** graph HTTP metadata stays in `NoteControllerGraphTests`; retrieval assembly stays at service boundary
- **Done when:** rubric applied; suite green.

### Phase 22 — Services: QuestionGeneration batch (planning / eligibility / candidates)
- **Status:** done
- **Type:** Behavior
- **Kept (job/pipeline contract; style renovated in place):**
  - Planning: `QuestionGenerationBatchPlanningServiceTest`, `QuestionGenerationBatchLocalPlanningTest`
  - Eligibility: `QuestionGenerationBatchManualEligibilityTest`, `…OverdueEligibilityTest`, `…RetryEligibilityTest`
  - Candidates: `QuestionGenerationBatchCandidateMemoryTrackersTest`, `…QueuedRequestCandidateTest`
  - Schedule/metrics/request: `QuestionGenerationBatchUserScheduleTest`, `…MetricsTest`, `QuestionGenerationRequestBuilderTests`
- **MakeMe:** `QuestionGenerationBatchBuilder` / `QuestionGenerationBatchRequestBuilder`; `MemoryTrackerBuilder.deletedAt`
- **Deleted:** mocked `QuestionGenerationBatchUserScheduleNoCandidateTrackersTest` — merged into UserScheduleTest with real DB (unanswered prompt excludes candidates)
- **Done when:** rubric applied in place (service boundary OK); suite green.

### Phase 23 — Services: QuestionGeneration batch (submit / poll / import / output)
- **Status:** done
- **Type:** Behavior
- **Kept (job/pipeline contract; style renovated in place):**
  - Submit: `QuestionGenerationBatchSubmissionServiceTest`, `…SubmitDueUsersTest` (+ TestBase), `…SubmitDueUsersServiceLoopTest`
  - Poll: `QuestionGenerationBatchPollingServiceTest`
  - Import/row-import: `…ImportServiceTest`, `…RowImportServiceTest`, `…RowImportServiceAtomicTest` (+ AtomicTestSupport)
  - Output: `…OutputCollectionServiceTest`, `…DirectBatchTest`, `…ScopeTest`, `…OutputFixtureTest`, `…OutputCollectionTestSupport`
  - Retention/jsonl: `…RetentionServiceTest`, `…JsonlRendererTest`
- **MakeMe:** batch builder gains `importedAt` / `outputCollectedAt`; shared `ImportPayloadSupport` for success JSONL
- **Exception:** `SubmitDueUsersServiceLoopTest` keeps collaborator mocks — isolates continue-after-failure summary aggregation (REQUIRES_NEW Spring path is heavy)
- **Done when:** rubric applied in place (service boundary OK); suite green.

### Phase 24 — Services: QuestionGeneration batch (maintenance / admin / concurrency)
- **Status:** done
- **Type:** Behavior
- **Kept (job/pipeline contract; style renovated in place):**
  - `QuestionGenerationBatchMaintenanceServiceTest` — builders + autowired service; reuse OutputCollectionTestSupport OpenAI fixtures
  - `QuestionGenerationBatchMaintenanceJobTests` — merged duplicate resume-failure cases; focused orchestration asserts
  - `QuestionGenerationBatchMaintenanceJobSchedulerLockTest` — already clean (annotation contract)
  - `QuestionGenerationBatchMaintenanceConcurrencyTest` (+ `ShedLockConfigProdTest`) — already clean (JDBC lock / prod config)
  - `QuestionGenerationBatchAdminStatusServiceTest` — SpringBootTest + real repos/runs; ScheduledTask mock only for scheduler-active
- **Exception:** `MaintenanceJobTests` keeps collaborator mocks for job/resume step-order and continue-after-failure orchestration
- **Done when:** all QGen batch tests renovated; suite green.

### Phase 25a — Services: health / search / export / openAiApis / entities
- **Status:** done
- **Type:** Behavior
- **Files:**
  - `services/health/**` (runner / empty-folder / readme-only / dead-wiki / bulk purge)
  - `services/search/**` (exact-match / user-model / empty-embedding — embedding mock kept)
  - `services/notebookExport/**` + root `NotebookExportServiceTest`
  - `services/openAiApis/**` (stream mapper + structured params — already mostly clean)
  - `services/entities/WikidataEntityTest`
- **Done when:** rubric applied; suite green.

### Phase 25b — Services: ai / book + remaining root leftovers
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `services/ai/**`
  - `services/book/**`
  - root leftovers: `AuthorizationServiceTest`, `ApproximateUtf8TokenBudgetTest`, `SRTProcessorTests`, `AiOpenAiAssistantFactory*`, `Conversation*`, `NotebookReindexing*`, `NotebookGroupServiceTest`, and any other unrenovated service tests
- **Done when:** remaining service leftovers renovated or deleted as redundant; suite green.

### Phase 26 — Final anti-pattern sweep
- **Status:** planned
- **Type:** Behavior
- **Observable:** repo-wide backend test smells from the rubric are cleared or explicitly excepted in this plan.
- **Method:**
  1. Grep for remaining verbose `creatorAndOwner` + separate notebook wiring where `notebookOwnedBy` fits.
  2. Grep for collaborator mocks beyond allowed externals.
  3. Spot-check largest remaining files for repeated full-payload asserts.
  4. Fix stragglers; update this plan with any permanent exceptions (and why).
- **Done when:** sweep clean (or exceptions documented); suite green; plan marked complete.

---

## Structure slices (insert when blocked)

If a Behavior phase cannot express fixtures concisely:

- Add a **Structure** sub-phase immediately before continuing that Behavior: extend the relevant MakeMe builder(s) under `backend/src/test/java/com/odde/doughnut/testability/`.
- Do not add builders “for later phases.”
- Record the Structure sub-phase in this PLAN (or a sibling `*-PLAN.md`) with status.

---

## Progress log

| Phase | Status | Notes |
|-------|--------|-------|
| 1 | done | Light pass: focused asserts, merge/dedupe parameterized cases, Optional.empty positives; split FrontmatterAliasesWikiLinkOverlapTest (>250). Suite green after local doughnut_test recreate (post-squash stale history). |
| 2 | done | Focused asserts / parameterized merges; FailureReportFactoryTest → real repo + only GithubService mock; TextContent uses Spring Validator. DisplayNamePathSeparatorsTrim / RealRandomizer / Robots already clean. |
| 3 | done | Real FailureReportRepository for excluded exceptions (was mock that always returned count 0); ControllerSetup uses @MockitoBean GithubService + real TestabilitySettings; merged ExcludedExceptions into ControllerSetupTest; focused asserts / delta-only on duplicate-title + OpenAI handlers; ObjectMapper/NullToNotFound cleaned. TZ migration + DatabaseTimeZone already clean. |
| 4 | done | PredefinedQuestionTest: AiQuestionGenerator → OpenAIClient + OpenAiStructuredResponseMock (+ enqueue for multi-call); OwnershipTest real User/Circle; NoteEmbeddingTests pure (no Spring); deleted empty NoteAsConstructionTest; focused/delta asserts + notebookOwnedBy; ForgettingCurve parameterized. |
| 5 | done | Show/stats/spelling: focused asserts + `.aliases()` / `underSameNotebookAs`; parameterized literal spelling; skipped trackers on own note (drop subscription). Post-refactor split oversized `NoteControllerTests` into capability-named files (show / note-info / spelling / delete*). |
| 6 | done | Delete/upload/graph/AI: `notebookOwnedBy` + `underSameNotebookAs`; `asRelationship` MakeMe; tracker rehome canonical once / delta siblings; drop mid-state restore assert; graph relatedNotes fixture (was vacuous). Post-refactor: split remaining nesteds to capability files; extract `RelationshipNoteMarkdown` from NoteBuilder (>250). |
| 7 | done | Text content: `notebookOwnedBy` / `underSameNotebookAs`; `InboundWiki` helper; canonical wiki/alias asserts + parameterized invalid aliases; `ImageBuilder.forNote`. Post-refactor split grab-bag into capability files + shared base. |
| 8 | done | Note satellites: `notebookOwnedBy` / `underSameNotebookAs` / `.content()`; drop unused root notes & shared BeforeEach fixtures; Attachment → ControllerTestBase; RecentNotes drop unused HttpClientAdapter; SoftDeleted conflict shape once + sibling delta; schema persistence tests already clean. Post-refactor: `ownedFolder`/`ownedNotebook`/`expectSoftDeletedTitleConflict`. |
| 9 | done | Create/rename/dissolve: `ownedNotebook` + `folderCreate` / `listingHasFolder`; drop controller `createNotebook` fixture dance; trim via ObjectMapper only where Jackson deserializer matters; conflict/404 shape once + sibling deltas. Post-refactor: split oversized grab-bag into capability files + shared base (Phase 10 paths updated). |
| 10 | done | Move/cross-notebook/merge/link-rewrite: `ownedNotebook`/`ownedFolder` + `folderMove`/`folderMoveTo`/`folderMerge`/`folderMergeTo`; `.content()` for wiki fixtures; listing via `listingHasFolder`; conflict/404/BAD_REQUEST shape once + sibling deltas; drop overlapping dest-access denial. |
| 11 | done | CRUD/notes-folder/sharing/export/health/groups: lift `ownedNotebook`/`ownedFolder` to `NotebookControllerTestBase`; drop controller `createNotebook` fixtures; focused asserts + parameterized empty-name/reserved-title/health opt-in; catalog suite uses fresh user (avoid `topNote` pollution). Post-refactor: split NotesFolder → note-create/listing/folder-page; CRUD update → `NotebookUpdateControllerTest`; catalog → `NotebookCatalogControllerTest`; Wikidata suite trimmed to wikidata-only. |
| 12 | done | Books attach/retrieve/reading: split mega attach into outline / getBook / file bytes / Full-view; drop duplicate PDF locator + PDF/EPUB position persist twins; canonical reading-record shape once + skimmed/skipped vs invalid status; `textBlock`/`contentListAttachRequest` helpers. Post-refactor: attach-content / book-file / get-reading-position capability splits (≤250). |
| 13 | done | Block content/depth: `textBlock` + chapter helpers; drop indent twins subsumed by subtree move; merge outdent subtree duplicates; suggestion builders shared; OpenAI mock only for suggest. Post-refactor: create-from-content / suggest-layout / apply-layout splits + block/layout-reorg bases (≤250). |
| 14 | done | Memory tracker: `notebookOwnedBy` + drop redundant `.by`; threshold parameterized; recycle asserts id only; deleted-note lists assert contains only; `removedFromTracking()` builder; prompt helpers. Post-refactor: threshold / ask / show / tracking / recent / recall-prompts / update-property-key + base. |
| 15 | done | Split mega `RecallPromptControllerTests` → quiz / regenerate / contest / spelling / stem-masking + base; accidental-match canonical shape once + sibling deltas; drop redundant `.by` on recalls; merge duplicate spelling answer-entity asserts. |
| 16 | done | Assimilation: `rememberSpelling` builder + assimilate return value; drop redundant `.by` / count delta; Search shared base + blank-key params; `.aliases` / `overlapWikiLink`; drop duplicate auth; OpenAI embedding mock only for semantic. |
| 17 | done | AI controllers: drop unused fixtures / verbose notebook wiring; `.content()` + shared extract helpers; canonical reject/export shapes once + sibling deltas; Audio onto ControllerTestBase; split remove-refinement from note-refinement. OpenAI mocks kept. |
| 18a | done | User/circle/bazaar/subscription + DTOs: focused asserts, parameterized auth/blank cases, `hasMember` / drop redundant `.by`, drop unused fixtures. Post-refactor: split User → profile / token / menu-data / recall-stats; drop dead CircleService; fix unread-read-by-receiver fixture. currentUser fetcher already clean. |
| 18b | done | Conversation/books/admin/wikidata/settings/failure-report: focused asserts; drop unused OpenAI mock on Books; fold resume happy path into admin QGen (delete mock ResumeTest — error orchestration covered by MaintenanceJobTests); drop redundant `.by` on schedule; Wikidata HttpClientAdapter only; Install already clean. Post-refactor: conversation → mark/reply / listing / start / AI-reply + base. |
| 19 | done | Deleted MemoryTrackerServiceTest + RecallQuestionServiceTest after lifting unique asserts to Assimilation/Tracking/AskQuestion controllers. Kept AssimilationService* / UnassimilatedProperty / SR algorithm / RecallStats (+ perf) as domain-stable; renovated makeMe + focused asserts. |
| 20 | done | Deleted NoteAutomation / NoteMotion / WikiLinkRewrite (controller duplicates). Slimmed NoteService to restore-by-deletedAt; lifted cascade + AI null/empty responses to controllers. Kept realm/wiki/alias/property/embedding/QGen as domain contracts; notebookOwnedBy + `.aliases`; post-refactor split oversized realm/wiki/property files. Notebook* leftovers → Phase 25. EmbeddingMaintenanceJob keeps collaborator mocks (thin loop). |
| 21 | done | Focus-context retrieval kept at service boundary; capability splits ≤250; `FolderBuilder.notebookOwnedBy`. |
| 22 | done | QGen planning/eligibility/candidates/schedule/metrics/request-builder: focused asserts + drop redundant `.by`; batch/request MakeMe builders; delete mocked NoCandidateTrackers (real DB via unanswered prompt); overdue/retry parameterized. |
| 23 | done | QGen submit/poll/import/output/retention/jsonl: builders for manual batch/request fixtures; ImportPayloadSupport; focused sibling deltas; parameterized terminal poll skip; AtomicTestSupport split ≤250. Loop test keeps collaborator mocks (continue-after-failure). |
| 24 | done | QGen maintenance/admin/concurrency/locks/jobs: builders + OutputCollectionTestSupport; AdminStatus → real DB; merge resume-failure job cases; lock/concurrency already clean. JobTests keeps collaborator mocks (orchestration). |
| 25a | done | Health/search/export/openAi/entities: drop runner DTO tautology + purge opt-in (controller covers); focused sibling deltas; `.aliases` / `.content`; blank search parameterized; Wikidata empty cases parameterized. Zip wiki twin deleted (markdown assemble covers). |
| 25b–26 | planned | — |

---

## Discoveries (update while executing)

- Inventory snapshot 2026-08-05: ~225 test files; services 104 / controllers 66 dominate effort.
- Largest controller files need nested splits (Phases 5–6, 9–10, 14–15).
- QGen batch is ~32 service tests — keep at service boundary; style-only renovation.
- Algorithms package was already domain-stable; main debt was repeated full-payload / negative asserts and a few multi-behavior tests.
- Local `doughnut_test` after Flyway squash may still hold pre-squash history without baseline tables — recreate DB + `migrateTestDB` if `backend:test_only` fails with missing tables/columns.
- Phase 2 pure-contract packages were mostly light debt; main win was dropping collaborator mocks on FailureReportFactory (keep GithubService as external).
- Phase 3: ControllerSetupExcludedExceptionsTest’s mocked `count()` always returned 0 — did not prove no report was saved; real repo is required for that claim. Framework/config contracts stay in place (TZ repair migrations, ObjectMapper, advice).
- Phase 4: PredefinedQuestionTest lived as a service-orchestration test under entities/; keep in place with OpenAI external mock. `OpenAiStructuredResponseMock.enqueueStructuredResponse` needed for same-type multi-call (generate → regenerate). Assert stems not full MCQ equality — postProcess / persistence can flip `choicesMayBeShuffled`. NoteEmbedding float round-trip does not need SpringBootTest.
- Phase 5: Wiki-title canonical shape once; siblings assert noteId (or pipe/qualified deltas). `.aliases()` already refreshes alias index — only refresh wiki cache on the viewer. Note-info skipped-tracker claim does not need subscription wiring when the note is owned by current user. Oversized `NoteControllerTests` split along capability seams during post-change-refactor; Phase 6 paths updated accordingly.
- Phase 6: `NoteBuilder.asRelationship` + extracted `RelationshipNoteMarkdown` replace local relationship-content helpers. Graph `relatedNotesExpose…` was vacuous with only a root note — needs a linked peer fixture. Tracker-rehome siblings assert property-key delta only after the canonical rehome case. Restore test drops mid-delete assert covered by exclusion sibling. Remaining grab-bag `NoteControllerTests` nesteds split to upload / recall-setting / graph / AI-context files.
- Phase 7: Inbound-wiki rename fixtures share `noteWithInboundWiki` (target + carrier via content update). Display-text wiki-title sibling asserts delta only after full-shape canonical. Invalid authored-alias rejects: BINDING_ERROR + unchanged content once; sibling invalid list items parameterized on message only. `ImageBuilder.forNote` replaces post-`please` `setNote` + save. Oversized grab-bag split to title / inbound-wiki / content / aliases (+ shared base).
- Phase 8: Relation move tests: named notebooks still need `creatorAndOwner` + name (wiki link qualification); notes use `notebookOwnedBy` / `underSameNotebookAs` / `.content()`. Drop mid-state folder assert before idempotent rematch; same-notebook no-rewrite drops redundant wiki-title list when content unchanged. SoftDeleted MVC: canonical conflict asserts deletedNoteId once; siblings status+errorType only. Schema/constraint persistence tests (title NOT NULL, folder unique index) already domain-stable — leave in place.
- Phase 9: Folder name trim is Jackson `DisplayNameTrimmingDeserializer` — keep `objectMapper.readValue` for trim cases; plain setters skip trim. Create fixtures via `ownedNotebook()` not `controller.createNotebook`. Post-refactor split grab-bag into create/rename/dissolve + move/cross-notebook/merge/link-rewrite (+ shared base); Phase 10 targets the move files.
- Phase 10: Shared base gains `ownedFolder`, named `ownedNotebook`, and move/merge request helpers. Cross-notebook wiki rewrite still needs named notebooks (`ownedNotebook("NbA")`). Sibling 404/BAD_REQUEST/access-denial cases assert delta only; dest-parent access denial overlaps root dest denial — one case enough.
- Phase 11: Lift `ownedNotebook`/`ownedFolder` to `NotebookControllerTestBase` (folder-management base inherits). Catalog exact-list asserts need a fresh user in `@BeforeEach` because base setup creates `topNote`. Wikidata root-note suite overlaps note-create folder assignment — keep only wikidata enrichment cases. Post-refactor capability splits: note-create / folder-listing / folder-page / update / catalog.
- Phase 12: PDF locator “heading+body” and “match bboxes” were the same claim — keep one. Patch PDF via `lastReadBody` already covers PdfLocator DTO path; EPUB wire helper covers EpubLocator. Put reading-record return shape is enough without re-asserting the repository row. Content-list attach fixtures share `contentListAttachRequest` + `textBlock`. Post-refactor splits: attach-content / book-file / get-reading-position.
- Phase 13: Indent “depth+1” and “full book size” were subsumed by indent-moves-descendants; two outdent-subtree cases collapsed to one with sibling W. Suggest/apply share `suggestionWithDepths` / `nestBAndCDepths`. Create-from-content fixtures use `chapterWithHeadingAndBody` / `textBlock`. Post-refactor: create-from-content / suggest-layout / apply-layout + `blockByTitle` / layout-reorg bases.
- Phase 14: `aMemoryTrackerFor` already inherits notebook owner — drop `.by(currentUser)` when note is `notebookOwnedBy`. Threshold below/at/above → one parameterized case. Ask recycle asserts prompt id only after spelling shape. Deleted-note recent lists: `contains(active)` enough without `not(hasItem)`. Post-refactor split show vs tracking mutations; `spellingTracker()` helper.
- Phase 15: Accidental-match title case is the full-shape canonical; alias/trim/unreadable/floor siblings assert outcome or matched-id delta only. Spelling `thinkingTimeMs` + answer-entity twins collapsed to one persist assert. Stem-masking: mark/not-color once; overlap-wiki siblings keep Other Note / no-mark deltas. Recalling window-end covered by half-day param — status case keeps `totalAssimilatedCount` only; previously-answered spelling asserts type only after MCQ window canonical. Post-refactor: stem-masking split; accidental/overlap onto shared base.
- Phase 16: Assimilation spelling twin uses `.rememberSpelling()` + assimilate return list (drop `NoteRepository` mutation/`findLast100`). Property-index still needs explicit `refreshForNote` (no MakeMe hook yet — one call). Search: shared base for term helpers; blank empty/whitespace parameterized; drop duplicate not-logged-in twins; alias fixtures via `.aliases` / `overlapWikiLink` (drop `NoteAliasIndexService` wiring). Semantic keeps OpenAI embedding mock as true external; empty embedding → literal fallback.
- Phase 17: Extract fixtures use `.content(EXTRACTABLE_CONTENT)` not post-`please` mutation; shared `AiControllerExtractNoteTestSupport` (`selectSingleLayoutItem` / `assertBadRequestContaining`). PredefinedQuestion list drops unused root+N-children notebooks. Create-extracted reserved title is BINDING_ERROR canonical; alias sibling asserts message only. Export refinement/question maps assert keys + unique deltas. Audio extends ControllerTestBase; OpenAI + transcription mocks stay. Post-refactor: remove-refinement split to `AiControllerRemoveRefinementSuggestionTest`; collapse duplicate bad-request helper; wire `selectSingleLayoutItem` into preview/validation.
- Phase 18a: Split Phase 18 mid-flight — ConversationMessage/Books/Admin/Wikidata/etc. deferred to 18b (ConversationMessage ~375 lines, not a quick pass). Circle: prefer `hasMember` over joinAndSave. Bazaar: owner-can + admin-removes share empty-list return; drop duplicate return-shape twin. User menu unread “already read” needs other-sender + `readByReceiver` (own-sender case overlaps zero-own-messages). Token/menu/recall-stats capability split during post-change-refactor.
- Phase 18b: Mock-heavy `AdminQuestionGenerationBatchControllerResumeTest` replaced by real controller resume asserting manual-maintenance timestamps; job/service suites already cover recordError orchestration. Books EPUB/304 assert deltas after PDF canonical. Conversation mark-read merges empty-return + read flag. Wikidata: rename `MakeMeWithoutDB` field (was shadowing MakeMe); drop weak search `verify(any)` twin; parameterize encoding. Install + DisplayName trim already domain-stable.
- Phase 19: Assimilation queue/property units/wiki-link gates and UnassimilatedPropertyService stay at service boundary (scheduling + index contracts). MemoryTrackerServiceTest was almost entirely controller-duplicative once soft-delete/property-assimilate asserts moved up. RecallStats aggregate stays pure-unit (fixtures); PerformanceTest is intentional N+1 guard on `compute()`. Keep `.by(user)` when assimilating subscribed notes owned by someone else.
- Phase 20: Wiki/alias/property index + NoteRealm assembly stay service-level (cache/index/DTO contracts beyond HTTP smoke). NoteAutomation/Motion/WikiLinkRewrite folder-move were pure controller duplicates. NoteService restore matching deletedAt timestamps is intentional service contract (controller undo is all-or-nothing via destroy). EmbeddingMaintenanceJobTests keeps NotebookRepository/IndexingService mocks — thin scheduled for-loop, not worth SpringBoot+OpenAI. Named notebooks still needed for qualified wiki links.
- Phase 22: QGen planning stays at service boundary. Repo-mock NoCandidateTrackers replaced by real DB (answered recall + unanswered non-contested prompt). Batch/request builders for COMPLETED/SUBMITTED/FAILED fixtures. Phase 23 may reuse builders for submit/poll/import manual construction.
- Phase 23: Reused Phase 22 batch/request builders (+ `importedAt`/`outputCollectedAt`). Collapsed triplicate import success-line JSON into `ImportPayloadSupport`. `SubmitDueUsersServiceLoopTest` collaborator mocks kept for continue-after-failure summary — Phase 26 may revisit if Spring coverage appears.
- Phase 24: MaintenanceService uses builders + OutputCollectionTestSupport; AdminStatus counts/runs use real DB (ScheduledTask mock only for scheduler-active string match). JobTests/resume-order Nested keep collaborator mocks — Phase 26 sweep. SchedulerLock + JDBC concurrency already domain-stable.
- Phase 25a: Health rules stay service-level (deeper than NotebookHealthController). Bulk-purge opt-in reject deleted as controller duplicate. HealthRuleRunner DTO-retention test was not exercising the runner — removed. Dead-wiki alias via `.aliases()` (auto-refreshes index). Search exact-match siblings assert first-id delta only after canonical size+title+id. ZipBuilder wiki-link twin dropped; ExportNoteMarkdown already asserts preservation.
