package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.QuestionContestResult;
import com.odde.donut.entities.MemoryTracker;
import com.odde.donut.entities.Note;
import com.odde.donut.entities.RecallPrompt;
import com.odde.donut.exceptions.OpenAiNotAvailableException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ai.GeneratedMcq;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
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
        makeMe.aRecallPrompt().forMemoryTracker(memoryTracker).withMcqForNote(note).please();
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
            .withMcqForNote(othersTracker.getNote())
            .please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.regenerate(othersPrompt, contestAdvice("test")));
  }

  @Test
  void shouldPassOldQuestionAndContestResultToOpenAiApi()
      throws JsonProcessingException, UnexpectedNoAccessRightException {
    makeMe
        .anMcq()
        .mcq(recallPrompt.getMcq())
        .stem("What is the capital of France?")
        .choices("Paris", "London", "Berlin", "Madrid")
        .correctAnswerIndex(1)
        .testedFocus("France's capital city")
        .validationRationale("London is the stored correct choice")
        .please();
    GeneratedMcq jsonQuestion =
        makeMe.aGeneratedMcq().stem("What is the first color in the rainbow?").please();
    openAiStructuredResponseMock.stubStructuredResponse(jsonQuestion);

    com.odde.donut.controllers.dto.RecallPrompt regeneratedPrompt =
        controller.regenerate(recallPrompt, contestAdvice("test"));

    Assertions.assertThat(regeneratedPrompt.getMcq().getQuestionStem())
        .contains("What is the first color in the rainbow?");

    @SuppressWarnings({"unchecked", "rawtypes"})
    ArgumentCaptor<StructuredResponseCreateParams<GeneratedMcq>> paramsCaptor =
        ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
    verify(openAiStructuredResponseMock.responseService(), atLeastOnce())
        .create(paramsCaptor.capture());

    String inputText =
        paramsCaptor.getValue().rawParams().input().flatMap(input -> input.text()).orElse("");
    String previousQuestionJson =
        new ObjectMapperConfig()
            .objectMapper()
            .writerWithDefaultPrettyPrinter()
            .writeValueAsString(
                new GeneratedMcq(
                    "What is the capital of France?",
                    "London",
                    List.of("Paris", "Berlin", "Madrid"),
                    "France's capital city",
                    "London is the stored correct choice"));
    assertThat(inputText, containsString("Previously generated non-feasible question"));
    assertThat(inputText, containsString("Improvement advice"));
    assertThat(inputText, containsString("test"));
    assertThat(
        inputText,
        containsString("Please regenerate or refine the question based on the above advice"));
    assertThat(inputText, containsString(previousQuestionJson));
    assertThat(inputText, not(containsString("correctAnswerIndex")));
    assertThat(inputText, not(containsString("responseChoices")));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailable() {
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(
        OpenAiNotAvailableException.class,
        () -> controller.regenerate(recallPrompt, contestAdvice("test")));
  }
}
