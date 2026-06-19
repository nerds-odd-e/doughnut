package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.first;
import static com.odde.doughnut.controllers.dto.Randomization.RandomStrategy.last;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.odde.doughnut.controllers.dto.Randomization;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.PredefinedQuestion;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class PredefinedQuestionRefinementControllerTests extends ControllerTestBase {

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @Autowired PredefinedQuestionController controller;
  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  Note note;
  PredefinedQuestion predefinedQuestion;

  @BeforeEach
  void setup() {
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    predefinedQuestion = makeMe.aPredefinedQuestion().please();
  }

  @AfterEach
  void cleanupRandomization() {
    testabilitySettings.setRandomization(new Randomization(first, 0));
  }

  @Test
  void givenQuestion_thenReturnPostProcessedRefinedQuestion()
      throws UnexpectedNoAccessRightException {
    testabilitySettings.setRandomization(new Randomization(last, 0));
    MCQWithAnswer mcqWithAnswer =
        makeMe
            .aMCQWithAnswer()
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(0)
            .choicesMayBeShuffled(true)
            .please();
    openAiStructuredResponseMock.stubStructuredResponse(mcqWithAnswer);

    PredefinedQuestion result = controller.refineQuestion(note, predefinedQuestion);
    assertThat(
        result.getMcqWithAnswer().getQuestion().getResponseChoices(),
        equalTo(List.of("Red", "Green", "Blue")));
    assertThat(result.getMcqWithAnswer().getSolutionChoiceIndex(), equalTo(2));
  }

  @Test
  void invalidRefinedQuestionIsRejected() throws UnexpectedNoAccessRightException {
    MCQWithAnswer invalidQuestion =
        makeMe
            .aMCQWithAnswer()
            .choices("Blue", "Green", "Red")
            .correctChoiceIndex(3)
            .choicesMayBeShuffled(true)
            .please();
    openAiStructuredResponseMock.stubStructuredResponse(invalidQuestion);

    PredefinedQuestion result = controller.refineQuestion(note, predefinedQuestion);

    assertThat(result, equalTo(null));
  }

  @Test
  void refineQuestionFailedWithGpt35WillNotTryAgain() {
    openAiStructuredResponseMock.stubStructuredResponseMalformed("{invalid json}");
    assertThrows(RuntimeException.class, () -> controller.refineQuestion(note, predefinedQuestion));
    verify(openAiStructuredResponseMock.responseService(), Mockito.times(1))
        .create(ArgumentMatchers.any(StructuredResponseCreateParams.class));
  }

  @Test
  void shouldThrowWhenOpenAiNotAvailable() {
    testabilitySettings.setOpenAiTokenOverride("");
    assertThrows(
        OpenAiNotAvailableException.class,
        () -> controller.refineQuestion(note, makeMe.aPredefinedQuestion().please()));
  }
}
