package com.odde.donut.controllers;

import static com.odde.donut.controllers.dto.Randomization.RandomStrategy.first;
import static com.odde.donut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static com.odde.donut.services.QuestionGenerationBatchOutputCollectionTestSupport.stubCompletedOpenAiBatch;

import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.Grade;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.QuestionGenerationBatchImportService;
import com.odde.donut.services.QuestionGenerationBatchJsonlRenderer;
import com.odde.donut.services.QuestionGenerationBatchOutputCollectionService;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.odde.donut.testability.builders.RecallPromptBuilder;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

abstract class MemoryTrackerBatchImportControllerTestBase extends MemoryTrackerControllerTestBase {
  static final String ORIGINAL = "---\ntype: Note\n---\nThe observatory opens at nine.";
  static final String CORRECTED = "---\ntype: Note\n---\nThe observatory opens at ten.";
  static final String BATCH_STEM = "When does the observatory open?";

  @Autowired QuestionGenerationBatchJsonlRenderer renderer;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollector;
  @Autowired QuestionGenerationBatchImportService importer;
  @Autowired RecallPromptRepository recallPrompts;
  @Autowired NotebookGitHistoryService notebookGitHistory;
  @Autowired TextContentController textContentController;
  @Autowired EntityManager entityManager;
  final JsonMapper objectMapper = ObjectMapperConfig.handWrittenJsonMapper();

  MemoryTracker tracker;
  QuestionGenerationBatch batch;
  QuestionGenerationBatchRequest request;
  OpenAiBatchApiMock batchApi;
  OpenAiStructuredResponseMock synchronousApi;
  Timestamp now;

  @BeforeEach
  void prepareAcceptedFocusAndBatch() {
    var note =
        makeMe
            .aNote("Observatory")
            .notebookOwnedBy(currentUser.getUser())
            .content(ORIGINAL)
            .please();
    notebookGitHistory.resetHistory(note.getNotebook(), Instant.parse("2025-01-01T00:00:00Z"));
    tracker = ownedTracker(note);
    now = makeMe.aTimestamp().please();
    batch =
        makeMe
            .aQuestionGenerationBatch()
            .forUser(currentUser.getUser())
            .completedAt(now)
            .openaiBatchId("batch-openai-1")
            .please();
    request = makeMe.aQuestionGenerationBatchRequest().batch(batch).memoryTracker(tracker).please();
    batchApi = new OpenAiBatchApiMock(officialClient);
    synchronousApi = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.setRandomization(new Randomization(first, 0));
  }

  JsonNode renderRequestMetadata() throws Exception {
    JsonNode line = objectMapper.readTree(renderer.renderInputJsonl(batch));
    return line.at("/body/metadata");
  }

  RecallPrompt importProviderQuestion(JsonNode requestMetadata) throws Exception {
    collectAndImportProviderResult(requestMetadata);
    return savedPrompts().getFirst();
  }

  void collectAndImportProviderResult(JsonNode requestMetadata) throws Exception {
    var generated = makeMe.aGeneratedMcq().stem(BATCH_STEM).correctAnswer("Nine").please();
    String outputLine = batchSuccessLine(request, generated, requestMetadata);
    stubCompletedOpenAiBatch(batchApi, outputLine, "");
    outputCollector.collectOutputForCompletedBatches(now);
    makeMe.entityPersister.flushAndClear();
    importer.importCompletedBatches(now);
    makeMe.entityPersister.flushAndClear();
  }

  record StaleWaitingQueue(
      RecallPrompt answered,
      RecallPrompt contested,
      RecallPrompt stale,
      Grade grade,
      Timestamp nextRecallAt) {}

  StaleWaitingQueue prepareAStaleWaitingQueueAfterAnAcceptedEdit()
      throws UnexpectedNoAccessRightException {
    var answered = promptFromFocus().answerChoiceIndex(0).recorded().please();
    var contested = promptFromFocus().contested().please();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).withMcq(answered.getMcq()).please();
    var stale = promptFromFocus().please();
    makeMe.entityPersister.flushAndClear();
    var queue =
        new StaleWaitingQueue(
            answered,
            contested,
            stale,
            Grade.fromCorrect(answered.getAnswer().getCorrect()),
            reloadTracker().getNextRecallAt());
    publishCorrectedFocus();
    return queue;
  }

  private RecallPromptBuilder promptFromFocus() {
    return makeMe.aRecallPrompt().forMemoryTracker(tracker).generatedFrom(tracker.getNote());
  }

  void publishCorrectedFocus() throws UnexpectedNoAccessRightException {
    var update = new NoteUpdateContentDTO();
    update.setContent(CORRECTED);
    textContentController.updateNoteContent(reloadTracker().getNote(), update);
  }

  List<RecallPrompt> savedPrompts() {
    return recallPrompts.findAllByMemoryTracker_IdOrderByIdDesc(tracker.getId());
  }

  MemoryTracker reloadTracker() {
    return entityManager.find(MemoryTracker.class, tracker.getId());
  }
}
