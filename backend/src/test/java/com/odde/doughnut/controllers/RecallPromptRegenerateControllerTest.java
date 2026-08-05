package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.QuestionContestResult;
import com.odde.doughnut.controllers.dto.RecallQuestion;
import com.odde.doughnut.entities.MemoryTracker;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.RecallPrompt;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.server.ResponseStatusException;

class RecallPromptRegenerateControllerTest extends RecallPromptControllerTestBase {
  RecallPrompt recallPrompt;

  @BeforeEach
  void setUp() {
    Note note = ownedNote();
    MemoryTracker memoryTracker = makeMe.aMemoryTrackerFor(note).please();
    recallPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(memoryTracker)
            .withPredefinedQuestionForNote(note)
            .please();
  }

  private QuestionContestResult contestAdvice(String advice) {
    QuestionContestResult contestResult = new QuestionContestResult();
    contestResult.advice = advice;
    return contestResult;
  }

  @Test
  void askWithNoteThatCannotAccess() {
    currentUser.setUser(null);
    assertThrows(
        ResponseStatusException.class,
        () -> controller.regenerate(recallPrompt, contestAdvice("test")));
  }

  @Test
  void shouldNotBeAbleToRegenerateForOthersMemoryTracker() {
    MemoryTracker othersTracker = memoryTrackerOwnedByAnotherUser();
    RecallPrompt othersPrompt =
        makeMe
            .aRecallPrompt()
            .forMemoryTracker(othersTracker)
            .withPredefinedQuestionForNote(othersTracker.getNote())
            .please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.regenerate(othersPrompt, contestAdvice("test")));
  }

  @Test
  void shouldPassOldQuestionAndContestResultToOpenAiApi()
      throws JsonProcessingException, UnexpectedNoAccessRightException {
    MCQWithAnswer jsonQuestion =
        makeMe.aMCQWithAnswer().stem("What is the first color in the rainbow?").please();
    openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

    RecallQuestion regeneratedQuestion = controller.regenerate(recallPrompt, contestAdvice("test"));

    Assertions.assertThat(regeneratedQuestion.getMultipleChoicesQuestion().getQuestionStem())
        .contains("What is the first color in the rainbow?");

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<MCQWithAnswer>> paramsCaptor =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiStructuredResponseMock.responseService(), atLeastOnce())
        .create(paramsCaptor.capture());

    String inputText =
        paramsCaptor.getValue().rawParams().input().flatMap(input -> input.text()).orElse("");
    assertThat(inputText, containsString("Previously generated non-feasible question"));
    assertThat(inputText, containsString("Improvement advice"));
    assertThat(inputText, containsString("test"));
    assertThat(
        inputText,
        containsString("Please regenerate or refine the question based on the above advice"));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailable() {
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(
        OpenAiNotAvailableException.class,
        () -> controller.regenerate(recallPrompt, contestAdvice("test")));
  }
}
