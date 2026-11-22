package com.odde.donut.controllers;

import static com.odde.donut.controllers.dto.Randomization.RandomStrategy.first;
import static com.odde.donut.services.QuestionGenerationBatchImportPayloadSupport.batchSuccessLine;
import static com.odde.donut.services.QuestionGenerationBatchOutputCollectionTestSupport.stubCompletedOpenAiBatch;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.verifyNoInteractions;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.odde.donut.controllers.dto.NoteUpdateContentDTO;
import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.FocusNoteInput;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.QuestionGenerationBatch;
import com.odde.donut.entities.QuestionGenerationBatchRequest;
import com.odde.donut.entities.QuestionGenerationBatchRequestStatus;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.entities.repositories.NotebookGitBindingRepository;
import com.odde.donut.entities.repositories.QuestionGenerationBatchRequestRepository;
import com.odde.donut.entities.repositories.RecallPromptRepository;
import com.odde.donut.services.FocusNoteFingerprint;
import com.odde.donut.services.QuestionGenerationBatchImportService;
import com.odde.donut.services.QuestionGenerationBatchJsonlRenderer;
import com.odde.donut.services.QuestionGenerationBatchOutputCollectionService;
import com.odde.donut.services.QuestionGenerationBatchRowImportService;
import com.odde.donut.services.notebookGit.NotebookGitHistoryService;
import com.odde.donut.testability.OpenAiBatchApiMock;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import jakarta.persistence.EntityManager;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class MemoryTrackerBatchRecallPromptControllerTest extends MemoryTrackerControllerTestBase {
  private static final String ORIGINAL = "---\ntype: Note\n---\nThe observatory opens at nine.";
  private static final String CORRECTED = "---\ntype: Note\n---\nThe observatory opens at ten.";
  private static final String BATCH_STEM = "When does the observatory open?";

  @Autowired QuestionGenerationBatchJsonlRenderer renderer;
  @Autowired QuestionGenerationBatchOutputCollectionService outputCollector;
  @Autowired QuestionGenerationBatchImportService importer;
  @Autowired QuestionGenerationBatchRowImportService rowImporter;
  @Autowired QuestionGenerationBatchRequestRepository requests;
  @Autowired RecallPromptRepository recallPrompts;
  @Autowired NotebookGitHistoryService notebookGitHistory;
  @Autowired NotebookGitBindingRepository notebookGitBindings;
  @Autowired TextContentController textContentController;
  @Autowired EntityManager entityManager;
  @Autowired ObjectMapper objectMapper;

  private MemoryTracker tracker;
  private QuestionGenerationBatch batch;
  private QuestionGenerationBatchRequest request;
  private OpenAiBatchApiMock batchApi;
  private OpenAiStructuredResponseMock synchronousApi;
  private Timestamp now;

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

  @Test
  void reusesTheImportedQuestionWhenItsRenderedFocusStillMatches() throws Exception {
    RecallPrompt imported = importProviderQuestion(renderRequestMetadata());
    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), is(imported.getId()));
    assertThat(offered.getMcq().getQuestionStem(), is(BATCH_STEM));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Nine"));
    verifyNoInteractions(synchronousApi.responseService());
  }

  @Test
  void recordsTheRenderedFocusInputWithTheImportedQuestion() throws Exception {
    Timestamp readAt = testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 3).please());
    JsonNode requestMetadata = renderRequestMetadata();
    testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(3, 3).please());

    importProviderQuestion(requestMetadata);
    Note focus = reloadTracker().getNote();

    assertThat(
        savedPrompts().getFirst().getMcq().getFocusNoteInput(),
        equalTo(new FocusNoteInput(focus.getId(), readAt, FocusNoteFingerprint.of(focus))));
  }

  @Test
  void replacesTheImportedQuestionWhenAnEditLandsAfterRendering() throws Exception {
    JsonNode requestMetadata = renderRequestMetadata();
    String originalRevision = acceptedRevision();
    var update = new NoteUpdateContentDTO();
    update.setContent(CORRECTED);
    textContentController.updateNoteContent(tracker.getNote(), update);
    assertThat(acceptedRevision(), not(equalTo(originalRevision)));

    RecallPrompt imported = importProviderQuestion(requestMetadata);
    stubCurrentQuestion("Ten");
    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), not(equalTo(imported.getId())));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Ten"));
    assertThat(synchronousApi.requestInputTexts(1).getFirst(), containsString(CORRECTED));
    assertThat(savedPrompts(), hasSize(2));
  }

  @Test
  void renewsAQuestionFromAnOlderBatchResultWithoutRequestMetadata() throws Exception {
    RecallPrompt imported = importProviderQuestion(null);
    stubCurrentQuestion("Nine");

    var offered = controller.getRecallPrompt(reloadTracker());

    assertThat(offered.getId(), not(equalTo(imported.getId())));
    assertThat(offered.getMcq().getResponseChoices().getFirst(), is("Nine"));
    assertThat(synchronousApi.requestInputTexts(1).getFirst(), containsString(ORIGINAL));
  }

  @Test
  void reimportingTheSameBatchRowKeepsTheSavedQuestion() throws Exception {
    RecallPrompt imported = importProviderQuestion(renderRequestMetadata());
    QuestionGenerationBatchRequest importedRow = requests.findById(request.getId()).orElseThrow();

    assertThat(importedRow.getStatus(), is(QuestionGenerationBatchRequestStatus.IMPORTED));
    assertThat(rowImporter.importRow(importedRow), is(false));
    makeMe.entityPersister.flushAndClear();

    assertThat(savedPrompts(), hasSize(1));
    assertThat(controller.getRecallPrompt(reloadTracker()).getId(), is(imported.getId()));
  }

  private JsonNode renderRequestMetadata() throws Exception {
    JsonNode line = objectMapper.readTree(renderer.renderInputJsonl(batch));
    return line.at("/body/metadata");
  }

  private RecallPrompt importProviderQuestion(JsonNode requestMetadata) throws Exception {
    var generated = makeMe.aGeneratedMcq().stem(BATCH_STEM).correctAnswer("Nine").please();
    String outputLine = batchSuccessLine(request.getCustomId(), generated);
    if (requestMetadata != null && requestMetadata.isObject()) {
      JsonNode output = objectMapper.readTree(outputLine);
      ((ObjectNode) output.at("/response/body")).set("metadata", requestMetadata);
      outputLine = objectMapper.writeValueAsString(output);
    }
    stubCompletedOpenAiBatch(batchApi, outputLine, "");
    outputCollector.collectOutputForCompletedBatches(now);
    makeMe.entityPersister.flushAndClear();
    importer.importCompletedBatches(now);
    makeMe.entityPersister.flushAndClear();
    return savedPrompts().getFirst();
  }

  private List<RecallPrompt> savedPrompts() {
    return recallPrompts.findAllByMemoryTracker_IdOrderByIdDesc(tracker.getId());
  }

  private void stubCurrentQuestion(String answer) {
    synchronousApi.stubStructuredResponse(
        makeMe
            .aGeneratedMcq()
            .stem("According to the current note, when does it open?")
            .correctAnswer(answer)
            .please());
  }

  private String acceptedRevision() {
    return notebookGitBindings.findAcceptedRevision(tracker.getNote().getNotebook().getId());
  }

  private MemoryTracker reloadTracker() {
    return entityManager.find(MemoryTracker.class, tracker.getId());
  }
}
