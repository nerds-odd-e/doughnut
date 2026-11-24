package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.FocusNoteInput;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.models.responses.StructuredResponseCreateParams;
import jakarta.persistence.EntityManager;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.sql.SQLIntegrityConstraintViolationException;
import java.sql.Timestamp;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

class MemoryTrackerQuestionPersistenceControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired EntityManager entityManager;
  @Autowired JsonMapper webJsonMapper;

  OpenAiStructuredResponseMock openAiMock;

  @BeforeEach
  void setupGeneration() {
    openAiMock = new OpenAiStructuredResponseMock(officialClient);
    testabilitySettings.setRandomization(new Randomization(Randomization.RandomStrategy.first, 0));
  }

  @Test
  void persistsTheSelectedQuestionWithItsCurrentTracker() throws Exception {
    MemoryTracker tracker = ownedTracker();
    Note current = reifyTrackedProperty(tracker, "Current generation evidence");
    GeneratedMcq selected =
        makeMe.aGeneratedMcq().stem("Selected question").correctAnswer("Selected answer").please();
    selected.setTestedFocus("Current tracker knowledge");
    selected.setValidationRationale("Selected answer follows the current focus");
    openAiMock.stubStructuredResponse(selected);
    long questionsBefore = questionCount();

    var result = controller.getRecallPrompt(tracker);
    makeMe.entityPersister.flushAndClear();

    RecallPrompt persisted = entityManager.find(RecallPrompt.class, result.getId());
    Mcq question = persisted.getMcq();
    assertThat(persisted.getMemoryTracker().getId(), equalTo(tracker.getId()));
    assertThat(question.getId(), notNullValue());
    assertThat(question.getQuestionStem(), equalTo("Selected question"));
    assertThat(
        question.getResponseChoices(),
        equalTo(List.of("Selected answer", "distractor1", "distractor2", "distractor3")));
    assertThat(question.getCorrectAnswerIndex(), equalTo(0));
    assertThat(question.getTestedFocus(), equalTo("Current tracker knowledge"));
    assertThat(
        question.getValidationRationale(), equalTo("Selected answer follows the current focus"));
    assertThat(question.getContextSeed(), notNullValue());
    assertThat(questionCount(), equalTo(questionsBefore + 1));

    assertThat(
        generationInput(), containsString("<focus_note>\nTitle: " + current.getTitle() + "\n"));
  }

  @Test
  void preservesTheGenerationFocusWithThePersistedQuestion() throws Exception {
    String title = "Observatory at Ångström Peak";
    String markdown = "---\nsite: North ridge\n---\nThe observatory opens at nine.";
    Note focus =
        makeMe.aNote(title).notebookOwnedBy(currentUser.getUser()).content(markdown).please();
    MemoryTracker tracker = ownedTracker(focus);
    Timestamp readAt = testabilitySettings.timeTravelTo(makeMe.aTimestamp().of(2, 3).please());
    openAiMock.stubStructuredResponse(
        makeMe
            .aGeneratedMcq()
            .stem("When does the observatory open?")
            .correctAnswer("Nine")
            .please());

    var result = controller.getRecallPrompt(tracker);
    makeMe.entityPersister.flushAndClear();

    String input = generationInput();
    assertThat(input, containsString("<focus_note>\nTitle: " + title + "\n"));
    assertThat(input, containsString(markdown));
    Mcq persisted = entityManager.find(RecallPrompt.class, result.getId()).getMcq();
    assertThat(
        persisted.getFocusNoteInput(),
        equalTo(
            new FocusNoteInput(
                focus.getId(), readAt, expectedFingerprint(focus.getId(), title, markdown))));
    assertThat(persisted.getCorrectAnswerIndex(), equalTo(0));
    JsonNode mcqJson = webJsonMapper.valueToTree(result.getMcq());
    assertThat(mcqJson.get("questionStem").asString(), equalTo("When does the observatory open?"));
    assertThat(
        result.getMcq().getResponseChoices(),
        equalTo(List.of("Nine", "distractor1", "distractor2", "distractor3")));
    assertThat(mcqJson.hasNonNull("correctAnswerIndex"), is(false));
    for (String privateField :
        List.of("focusNoteFingerprint", "focusNoteInput", "focusNoteId", "focusNoteReadAt")) {
      assertThat(mcqJson.has(privateField), is(false));
      assertThat(webJsonMapper.valueToTree(persisted).has(privateField), is(false));
    }
  }

  private String generationInput() {
    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> requests =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiMock.responseService(), times(1)).create(requests.capture());
    return requests.getValue().rawParams().input().flatMap(input -> input.text()).orElseThrow();
  }

  private static String expectedFingerprint(int noteId, String title, String markdown)
      throws Exception {
    byte[] titleBytes = title.getBytes(StandardCharsets.UTF_8);
    byte[] markdownBytes = markdown.getBytes(StandardCharsets.UTF_8);
    byte[] framedFocus =
        ByteBuffer.allocate(3 * Integer.BYTES + titleBytes.length + markdownBytes.length)
            .putInt(noteId)
            .putInt(titleBytes.length)
            .put(titleBytes)
            .putInt(markdownBytes.length)
            .put(markdownBytes)
            .array();
    return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(framedFocus));
  }

  @Test
  void reusesThePromptCreatedWhileGenerationIsInFlight() throws Exception {
    MemoryTracker tracker = ownedTracker();
    AtomicReference<RecallPrompt> concurrentPrompt = new AtomicReference<>();
    openAiMock.onBeforeCreate(() -> {
      openAiMock.onBeforeCreate(null);
      concurrentPrompt.set(
          makeMe
              .aRecallPrompt()
              .forMemoryTracker(tracker)
              .withMcq(makeMe.anMcq().generatedFrom(tracker.getNote()).inMemoryPlease())
              .please());
    });
    openAiMock.stubStructuredResponse(makeMe.aGeneratedMcq().please());
    long questionsBefore = questionCount();

    var result = controller.getRecallPrompt(tracker);
    makeMe.entityPersister.flushAndClear();

    RecallPrompt persisted = entityManager.find(RecallPrompt.class, result.getId());
    assertThat(persisted.getId(), equalTo(concurrentPrompt.get().getId()));
    assertThat(persisted.getMcq().getId(), equalTo(concurrentPrompt.get().getMcq().getId()));
    assertThat(questionCount(), equalTo(questionsBefore + 1));
  }

  private long questionCount() {
    return entityManager
        .createQuery("SELECT COUNT(question) FROM Mcq question", Long.class)
        .getSingleResult();
  }

  @Nested
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  class AtomicQuestionWrite {
    private static final String FIXTURE_PREFIX = "atomic-recall-question-";

    @Autowired PlatformTransactionManager transactionManager;

    @BeforeEach
    void removeCommittedSetupUser() {
      String setupUser = currentUser.getUser().getExternalIdentifier();
      inCommittedTransaction(
          transactionManager,
          () -> deleteByUserExternalIdentifierLike(entityManager, mcqRepository, setupUser));
    }

    @AfterEach
    void cleanupCommittedState() {
      inCommittedTransaction(
          transactionManager,
          () ->
              deleteByUserExternalIdentifierLike(
                  entityManager, mcqRepository, FIXTURE_PREFIX + "%"));
    }

    @Test
    void failedPromptWriteRollsBackTheSelectedQuestion() {
      MemoryTracker tracker =
          inCommittedTransaction(
              transactionManager,
              () -> {
                currentUser.setUser(makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please());
                return ownedTracker();
              });
      long questionsBefore =
          inCommittedTransaction(
              transactionManager,
              () -> {
                return questionCount();
              });
      openAiMock.onBeforeCreate(() -> {
        openAiMock.onBeforeCreate(null);
        inCommittedTransaction(
            transactionManager,
            () -> {
              entityManager
                  .createQuery("DELETE FROM MemoryTracker tracker WHERE tracker.id = :id")
                  .setParameter("id", tracker.getId())
                  .executeUpdate();
            });
      });
      openAiMock.stubStructuredResponse(makeMe.aGeneratedMcq().please());

      RuntimeException failure =
          assertThrows(RuntimeException.class, () -> controller.getRecallPrompt(tracker));
      Assertions.assertThat(failure)
          .hasRootCauseInstanceOf(SQLIntegrityConstraintViolationException.class);

      long questionsAfter =
          inCommittedTransaction(
              transactionManager,
              () -> {
                return questionCount();
              });
      assertThat(questionsAfter, equalTo(questionsBefore));
    }
  }
}
