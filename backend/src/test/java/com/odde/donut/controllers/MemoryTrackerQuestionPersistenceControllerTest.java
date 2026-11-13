package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.odde.donut.controllers.dto.Randomization;
import com.odde.donut.entities.Mcq;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.services.ai.GeneratedMcq;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.models.responses.StructuredResponseCreateParams;
import jakarta.persistence.EntityManager;
import java.sql.SQLIntegrityConstraintViolationException;
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

class MemoryTrackerQuestionPersistenceControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired EntityManager entityManager;

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
    QuestionEvaluation disagreement = new QuestionEvaluation();
    disagreement.feasibleQuestion = false;
    disagreement.correctChoices = new int[] {1};
    disagreement.improvementAdvices = "Use the current focus";
    openAiMock.enqueueStructuredResponse(makeMe.aGeneratedMcq().please());
    openAiMock.enqueueStructuredResponse(disagreement);
    openAiMock.enqueueStructuredResponse(selected);
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

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> requests =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiMock.responseService(), atLeastOnce()).create(requests.capture());
    String firstInput =
        requests
            .getAllValues()
            .getFirst()
            .rawParams()
            .input()
            .flatMap(input -> input.text())
            .orElse("");
    assertThat(firstInput, containsString("<focus_note>\nTitle: " + current.getTitle() + "\n"));
  }

  @Test
  void reusesThePromptCreatedWhileGenerationIsInFlight() throws Exception {
    MemoryTracker tracker = ownedTracker();
    AtomicReference<RecallPrompt> concurrentPrompt = new AtomicReference<>();
    openAiMock.onBeforeCreate(() -> {
      openAiMock.onBeforeCreate(null);
      concurrentPrompt.set(makeMe.aRecallPrompt().forMemoryTracker(tracker).please());
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
