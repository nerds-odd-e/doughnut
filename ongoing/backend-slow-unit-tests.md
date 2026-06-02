# Backend slow unit test optimization

Profiled with full `pnpm backend:test_only` (JUnit XML under `backend/build/test-results/test/`).
Top 10 slowest individual tests (total suite: 1164 tests).

**Grouping:** per test file (10 groups of 1 test each — smaller batch size than one group of 10).

**Optimization rules (all phases):**

- Remove or simplify redundant tests first.
- Strictly no fixed-time waits (`Thread.sleep`, etc.).
- Flaky = failure; tests must be deterministic.

| # | Time | Class#method |
|---|------|----------------|
| 1 | 0.773s | `ImageUtilsTest#shouldResizeLargeImage` |
| 2 | 0.580s | `OpenApiDocsTests#openApiDocsApprovalTest` |
| 3 | 0.480s | `ControllerSetupTest#shouldRecordUserInfo` |
| 4 | 0.205s | `ConversationMessageControllerAiReplyTests$RecallPromptConversationTests#shouldUseNoteFromRecallPrompt` |
| 5 | 0.191s | `AiAudioControllerTests$ConvertAudioToTextTests#convertAudioToText` |
| 6 | 0.169s | `NullToNotFoundResponseBodyAdviceTest#shouldReturn404WhenAskAQuestionReturnsNull` |
| 7 | 0.091s | `ConversationMessageControllerTest$ConversationOrderingTests#returnsAtMost50Conversations` |
| 8 | 0.082s | `GcsBookStorageTest#delete_callsStorageDeleteForAllowedRef` |
| 9 | 0.082s | `AiControllerTest$GetModelVersions#shouldGetModelVersionsCorrectly` |
| 10 | 0.078s | `AiControllerExtractNoteTest$ExtractNote#shouldLimitExtractionOutputToThreeThousandTokensAndRequestWikiLinks` |

---

### Phase 1: Speed up ImageUtilsTest
Status: done

Target: `backend/src/test/java/com/odde/doughnut/algorithms/ImageUtilsTest.java` — `shouldResizeLargeImage()` (~0.77s).

Reduce test cost: smaller fixture image, fewer pixels, mock heavy IO if appropriate, merge redundant resize assertions, or move pure resize logic to a fast unit test without loading a huge asset. Keep behavior coverage.

Verify: `CURSOR_DEV=true nix develop -c backend/gradlew -p backend test -Dspring.profiles.active=test --tests "com.odde.doughnut.algorithms.ImageUtilsTest"`

---

### Phase 2: Speed up OpenApiDocsTests
Status: planned

Target: `backend/src/test/java/com/odde/doughnut/integration/OpenApiDocsTests.java` — `openApiDocsApprovalTest()` (~0.58s).

Avoid full Spring context or redundant doc generation if a lighter check suffices; dedupe with other OpenAPI tests if any. No sleep.

Verify: `--tests "com.odde.doughnut.integration.OpenApiDocsTests"`

---

### Phase 3: Speed up ControllerSetupTest
Status: planned

Target: `backend/src/test/java/com/odde/doughnut/configs/ControllerSetupTest.java` — `shouldRecordUserInfo()` (~0.48s).

Slim setup: shared fixtures, `@MockBean` instead of full stack where behavior unchanged, remove duplicate coverage.

Verify: `--tests "com.odde.doughnut.configs.ControllerSetupTest"`

---

### Phase 4: Speed up ConversationMessageControllerAiReplyTests recall prompt
Status: planned

Target: `ConversationMessageControllerAiReplyTests` nested `RecallPromptConversationTests#shouldUseNoteFromRecallPrompt` (~0.21s).

Minimize DB/API work; remove redundant AI reply tests if covered elsewhere.

Verify: `--tests "com.odde.doughnut.controllers.ConversationMessageControllerAiReplyTests"`

---

### Phase 5: Speed up AiAudioControllerTests convert audio
Status: planned

Target: `AiAudioControllerTests$ConvertAudioToTextTests#convertAudioToText` (~0.19s).

Smaller audio fixture, mock transcription service, avoid real file processing if not the contract under test.

Verify: `--tests "com.odde.doughnut.controllers.AiAudioControllerTests"`

---

### Phase 6: Speed up NullToNotFoundResponseBodyAdviceTest
Status: planned

Target: `NullToNotFoundResponseBodyAdviceTest#shouldReturn404WhenAskAQuestionReturnsNull` (~0.17s).

Use slice test or mock MVC without full `@SpringBootTest` if equivalent.

Verify: `--tests "com.odde.doughnut.configs.NullToNotFoundResponseBodyAdviceTest"`

---

### Phase 7: Speed up ConversationMessageControllerTest ordering
Status: planned

Target: `ConversationMessageControllerTest$ConversationOrderingTests#returnsAtMost50Conversations` (~0.09s).

Create fewer conversations in DB or test limit logic at a cheaper boundary.

Verify: `--tests "com.odde.doughnut.controllers.ConversationMessageControllerTest"`

---

### Phase 8: Speed up GcsBookStorageTest delete
Status: planned

Target: `GcsBookStorageTest#delete_callsStorageDeleteForAllowedRef` (~0.08s).

Ensure GCS client is mocked; no real network; trim Spring context.

Verify: `--tests "com.odde.doughnut.services.book.GcsBookStorageTest"`

---

### Phase 9: Speed up AiControllerTest model versions
Status: planned

Target: `AiControllerTest$GetModelVersions#shouldGetModelVersionsCorrectly` (~0.08s).

Mock AI client; avoid loading full model list from external config if redundant.

Verify: `--tests "com.odde.doughnut.controllers.AiControllerTest"`

---

### Phase 10: Speed up AiControllerExtractNoteTest extraction limit
Status: planned

Target: `AiControllerExtractNoteTest$ExtractNote#shouldLimitExtractionOutputToThreeThousandTokensAndRequestWikiLinks` (~0.08s).

Mock extraction; assert limits via stub response not heavy AI pipeline.

Verify: `--tests "com.odde.doughnut.controllers.AiControllerExtractNoteTest"`
