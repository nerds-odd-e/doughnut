package com.odde.donut.controllers;

import static com.odde.donut.testability.CommittedTransactionTestSupport.inCommittedTransaction;
import static com.odde.donut.testability.CommittedUserCleanup.deleteByUserExternalIdentifierLike;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ai.QuestionEvaluation;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import jakarta.persistence.EntityManager;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

class MemoryTrackerRecallPromptControllerTest extends MemoryTrackerControllerTestBase {
  @Autowired ObjectMapper objectMapper;
  @Autowired PlatformTransactionManager transactionManager;
  @Autowired EntityManager entityManager;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;

  @BeforeEach
  void setupOpenAiMock() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  private MemoryTracker spellingTracker() {
    Note note =
        makeMe
            .aNote("moon")
            .content("partner of earth")
            .notebookOwnedBy(currentUser.getUser())
            .please();
    return makeMe.aMemoryTrackerFor(note).spelling().please();
  }

  @Test
  void shouldReturnSpellingRecallPromptForSpellingMemoryTracker()
      throws UnexpectedNoAccessRightException {
    com.odde.donut.controllers.dto.RecallPrompt recallPrompt =
        controller.getRecallPrompt(spellingTracker());
    assertThat(recallPrompt.getSpellingQuestion(), notNullValue());
    assertThat(recallPrompt.getMcq(), nullValue());
  }

  @Test
  void shouldRecycleUnansweredSpellingRecallPromptForSpellingMemoryTracker()
      throws UnexpectedNoAccessRightException {
    MemoryTracker memoryTracker = spellingTracker();
    RecallPrompt existingPrompt =
        makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).spelling().please();

    assertThat(controller.getRecallPrompt(memoryTracker).getId(), equalTo(existingPrompt.getId()));
  }

  @Test
  void shouldRecycleMostRecentUnansweredMcqPrompt() throws UnexpectedNoAccessRightException {
    MemoryTracker tracker = ownedTracker();
    makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    RecallPrompt mostRecent = makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    makeMe.entityPersister.flush();

    assertThat(controller.getRecallPrompt(tracker).getId(), equalTo(mostRecent.getId()));
  }

  @Test
  void shouldGenerateMcqWhenNoUnansweredPromptExists() throws UnexpectedNoAccessRightException {
    openAiStructuredResponseMock.stubStructuredResponse(makeMe.aGeneratedMcq().please());

    assertThat(controller.getRecallPrompt(ownedTracker()).getMcq(), notNullValue());
  }

  @Nested
  @Transactional(propagation = Propagation.NOT_SUPPORTED)
  class OpenAiCallDoesNotHoldTransaction {
    private static final String FIXTURE_PREFIX = "recall-prompt-openai-tx-";

    @BeforeEach
    void deleteLeakedSetupUser() {
      String leakedSetupUser = currentUser.getUser().getExternalIdentifier();
      inCommittedTransaction(
          transactionManager,
          () -> deleteByUserExternalIdentifierLike(entityManager, leakedSetupUser));
    }

    @AfterEach
    void cleanupCommittedState() {
      inCommittedTransaction(
          transactionManager,
          () -> deleteByUserExternalIdentifierLike(entityManager, FIXTURE_PREFIX + "%"));
    }

    private MemoryTracker trackerTrackingTransactionActivityDuringOpenAiCalls(
        List<Boolean> transactionActiveDuringOpenAiCalls) {
      MemoryTracker tracker =
          inCommittedTransaction(
              transactionManager,
              () -> {
                currentUser.setUser(makeMe.aUser(FIXTURE_PREFIX + UUID.randomUUID()).please());
                return ownedTracker();
              });
      openAiStructuredResponseMock.onBeforeCreate(
          () ->
              transactionActiveDuringOpenAiCalls.add(
                  TransactionSynchronizationManager.isActualTransactionActive()));
      return tracker;
    }

    @Test
    void shouldGenerateMcqWithoutHoldingATransactionDuringOpenAiCall()
        throws UnexpectedNoAccessRightException {
      List<Boolean> transactionActiveDuringOpenAiCalls = new ArrayList<>();
      MemoryTracker tracker =
          trackerTrackingTransactionActivityDuringOpenAiCalls(transactionActiveDuringOpenAiCalls);
      openAiStructuredResponseMock.stubStructuredResponse(makeMe.aGeneratedMcq().please());

      com.odde.donut.controllers.dto.RecallPrompt recallPrompt =
          controller.getRecallPrompt(tracker);

      assertThat(transactionActiveDuringOpenAiCalls, is(not(empty())));
      assertThat(transactionActiveDuringOpenAiCalls, everyItem(is(false)));
      assertThat(recallPrompt.getMcq(), notNullValue());
    }

    @Test
    void shouldGenerateMcqWithoutHoldingATransactionDuringContestAndRegenerateOpenAiCalls()
        throws UnexpectedNoAccessRightException {
      List<Boolean> transactionActiveDuringOpenAiCalls = new ArrayList<>();
      MemoryTracker tracker =
          trackerTrackingTransactionActivityDuringOpenAiCalls(transactionActiveDuringOpenAiCalls);

      QuestionEvaluation notLegitimateEvaluation = new QuestionEvaluation();
      notLegitimateEvaluation.feasibleQuestion = false;
      notLegitimateEvaluation.correctChoices = new int[] {1};
      notLegitimateEvaluation.improvementAdvices = "the answer is disputable";

      openAiStructuredResponseMock.enqueueStructuredResponse(makeMe.aGeneratedMcq().please());
      openAiStructuredResponseMock.enqueueStructuredResponse(notLegitimateEvaluation);
      openAiStructuredResponseMock.enqueueStructuredResponse(
          makeMe.aGeneratedMcq().stem("regenerated stem").please());

      com.odde.donut.controllers.dto.RecallPrompt recallPrompt =
          controller.getRecallPrompt(tracker);

      assertThat(transactionActiveDuringOpenAiCalls, is(not(empty())));
      assertThat(transactionActiveDuringOpenAiCalls, everyItem(is(false)));
      assertThat(recallPrompt.getMcq(), notNullValue());
      assertThat(recallPrompt.getMcq().getQuestionStem(), equalTo("regenerated stem"));
    }
  }

  @Test
  void unansweredMcqPromptCarriesSolutionOmittedMcq() throws Exception {
    MemoryTracker tracker = ownedTracker();
    RecallPrompt existing = makeMe.aRecallPrompt().forMemoryTracker(tracker).please();
    Integer persistedAnswerIndex = existing.getMcq().getCorrectAnswerIndex();
    assertThat(persistedAnswerIndex, notNullValue());

    com.odde.donut.controllers.dto.RecallPrompt recallPrompt = controller.getRecallPrompt(tracker);
    assertThat(existing.getMcq().getCorrectAnswerIndex(), equalTo(persistedAnswerIndex));

    JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(recallPrompt));
    assertThat(json.has("mcq"), is(true));
    assertThat(json.has("multipleChoicesQuestion"), is(false));
    JsonNode mcqJson = json.get("mcq");
    assertThat(mcqJson.has("questionStem"), is(true));
    assertThat(mcqJson.has("responseChoices"), is(true));
    assertThat(mcqJson.has("multipleChoicesQuestion"), is(false));
    assertThat(mcqJson.has("correctAnswerIndex"), is(false));
    assertThat(mcqJson.has("testedFocus"), is(false));
    assertThat(mcqJson.has("validationRationale"), is(false));
  }

  @Test
  void shouldNotBeAbleToGetRecallPromptForOthersMemoryTracker() {
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class, () -> controller.getRecallPrompt(memoryTracker));
  }

  @Test
  void shouldRequireUserToBeLoggedIn() {
    currentUser.setUser(null);
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerBy(makeMe.aUser().please()).please();
    assertThrows(ResponseStatusException.class, () -> controller.getRecallPrompt(memoryTracker));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailableAndGeneratingQuestion() {
    MemoryTracker memoryTracker = ownedTracker();
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(
        OpenAiNotAvailableException.class, () -> controller.getRecallPrompt(memoryTracker));
  }

  @Test
  void shouldThrowWhenAiFailsToGenerateQuestion() {
    openAiStructuredResponseMock.stubStructuredResponse(null);
    ResponseStatusException exception =
        assertThrows(
            ResponseStatusException.class, () -> controller.getRecallPrompt(ownedTracker()));
    assertThat(exception.getStatusCode(), equalTo(HttpStatus.SERVICE_UNAVAILABLE));
  }
}
