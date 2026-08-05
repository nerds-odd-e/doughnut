# Backend unit tests → "small test" style

**Status:** in progress (Phase 13 done)
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
- **Status:** planned
- **Type:** Behavior
- **Files:** `MemoryTrackerControllerTest.java`
- **Done when:** rubric applied; suite green.

### Phase 15 — Controllers: recall prompts and recalls
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `RecallPromptControllerTests.java`
  - `RecallPromptAccidentalMatchEdgeTests.java`
  - `RecallPromptAccidentalMatchGradingTests.java`
  - `RecallPromptOverlapTryAgainTests.java`
  - `RecallsControllerTests.java`
- **Done when:** rubric applied; suite green.

### Phase 16 — Controllers: assimilation + search
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `AssimilationControllerTests.java`
  - `SearchControllerTests.java`, `SearchControllerWithinTests.java`
  - `SearchControllerAliasTests.java`, `SearchControllerSemanticTests.java`
- **Done when:** rubric applied; suite green.

### Phase 17 — Controllers: AI
- **Status:** planned
- **Type:** Behavior
- **Files:** all `AiController*.java`, `AiAudioControllerTests.java`, `PredefinedQuestionControllerTests.java`, `PredefinedQuestionRefinementControllerTests.java`
- **Done when:** rubric applied; suite green. Keep `OpenAiStructuredResponseMock` usage.

### Phase 18 — Controllers: remaining HTTP surfaces
- **Status:** planned
- **Type:** Behavior
- **Files:** remaining under `controllers/` not covered above, including but not limited to:
  - `UserControllerTest.java`, `CurrentUserInfoControllerTest.java`, `currentUser/*`
  - `CircleControllerTest.java`, `BazaarControllerTest.java`, `SubscriptionControllerTest.java`
  - `ConversationMessageControllerTest.java`, `BooksControllerTest.java`
  - `AdminUserControllerTest.java`, `AdminQuestionGenerationBatchController*.java`
  - `UserQuestionGenerationBatchScheduleControllerTest.java`
  - `WikidataControllerTests.java`, `GlobalSettingsControllerTest.java`
  - `FailureReportControllerTest.java`, `InstallControllerTest.java`
  - `ControllerTestBase.java` — only if shared helpers need concise makeMe fixes for this wave
- **Done when:** all controller tests renovated; suite green.

### Phase 19 — Services: assimilate / memory / recall — consolidate into controllers
- **Status:** planned
- **Type:** Behavior
- **Observable:** unique coverage lives at controller (or intentional service contract); redundant service tests deleted or slimmed.
- **Files (start list — adjust after reading):**
  - `AssimilationService*.java`, `AssimilationServiceTestBase.java`
  - `UnassimilatedPropertyServiceTest.java`
  - `MemoryTrackerServiceTest.java`
  - `RecallService*.java`, `RecallQuestionServiceTest.java`, `RecallStatsServiceTest.java`
- **Method:** For each test method, find controller coverage; move unique asserts up if missing; delete duplicates; keep only domain-stable leftovers.
- **Done when:** no redundant service suites for this theme; suite green.

### Phase 20 — Services: note / wiki / embedding / property / alias
- **Status:** planned
- **Type:** Behavior
- **Files:** service tests matching Note*, Wiki*, Embedding*, *Property*, *Alias* under `services/` (root), e.g. `NoteRealmServiceTest`, `WikiTitleCacheServiceTest`, `WikiLinkResolver*`, `NotePropertyIndex*`, `NoteAliasIndexServiceTest`, `EmbeddingService*.java`, `NoteAutomationServiceTests`, `NoteQuestionGenerationServiceTests`, etc.
- **Method:** Same consolidate-or-keep-as-contract rule.
- **Done when:** rubric applied; suite green.

### Phase 21 — Services: focus context
- **Status:** planned
- **Type:** Behavior
- **Files:** `services/focusContext/**` (includes large `FocusContextRetrievalServiceTest`)
- **Done when:** rubric applied; keep if this service is the intentional contract for focus-context assembly; suite green.

### Phase 22 — Services: QuestionGeneration batch (planning / eligibility / candidates)
- **Status:** planned
- **Type:** Behavior
- **Files:** QGen tests for planning, eligibility, candidates, request builder, user schedule, metrics, local planning — roughly half of `QuestionGenerationBatch*.java` excluding import/output/submit/maintenance (see Phase 23–24). Exact split: take alphabetically first half of remaining QGen files after skimming for cohesion.
- **Done when:** rubric applied in place (service boundary OK); suite green.

### Phase 23 — Services: QuestionGeneration batch (submit / poll / import / output)
- **Status:** planned
- **Type:** Behavior
- **Files:** submit/poll/import/output/collection/retention/jsonl/row-import related `QuestionGenerationBatch*.java`
- **Done when:** rubric applied; suite green.

### Phase 24 — Services: QuestionGeneration batch (maintenance / admin / concurrency)
- **Status:** planned
- **Type:** Behavior
- **Files:** remaining `QuestionGenerationBatch*.java` (maintenance, admin status, concurrency, locks, jobs)
- **Done when:** all QGen batch tests renovated; suite green.

### Phase 25 — Services: nested packages + leftovers
- **Status:** planned
- **Type:** Behavior
- **Files:**
  - `services/ai/**`
  - `services/book/**`
  - `services/health/**`
  - `services/search/**`
  - `services/notebookExport/**`
  - `services/openAiApis/**`
  - `services/entities/**`
  - root leftovers: `AuthorizationServiceTest`, `ApproximateUtf8TokenBudgetTest`, `SRTProcessorTests`, `AiOpenAiAssistantFactory*`, `Conversation*`, `NotebookReindexing*`, etc.
- **Done when:** all service tests renovated or deleted as redundant; suite green.

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
| 14–26 | planned | — |

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
