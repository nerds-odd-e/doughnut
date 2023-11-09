package com.odde.doughnut.services;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.doReturn;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.controllers.json.QuizQuestionContestResult;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.QuizQuestionEntity;
import com.odde.doughnut.services.ai.MCQWithAnswer;
import com.odde.doughnut.services.ai.QuestionEvaluation;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import io.reactivex.Single;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class AiAdvisorServiceWithDBTest {

  private AiAdvisorService aiAdvisorService;
  @Mock private OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  @BeforeEach
  void Setup() {
    MockitoAnnotations.openMocks(this);
    aiAdvisorService = new AiAdvisorService(openAiApi);
  }

  @Nested
  class ContestQuestion {
    QuizQuestionEntity quizQuestionEntity;
    QuestionEvaluation questionEvaluation = new QuestionEvaluation();

    @BeforeEach
    void setUp() {
      questionEvaluation.correctChoices = new int[] {0};
      questionEvaluation.feasibleQuestion = true;
      questionEvaluation.comment = "what a horrible question!";

      MCQWithAnswer aiGeneratedQuestion =
          makeMe
              .aMCQWithAnswer()
              .stem("What is the first color in the rainbow?")
              .choices("red", "black", "green")
              .correctChoiceIndex(0)
              .please();
      Note note = makeMe.aNote().please();
      quizQuestionEntity =
          makeMe.aQuestion().ofAIGeneratedQuestion(aiGeneratedQuestion, note.getThing()).please();
    }

    @Test
    void rejected() throws JsonProcessingException {
      mockChatCompletionAndReturnFunctionCall(
          "evaluate_question", new ObjectMapper().writeValueAsString(questionEvaluation));
      QuizQuestionContestResult contest = aiAdvisorService.contestQuestion(quizQuestionEntity);
      assertTrue(contest.rejected);
      Assertions.assertThat(contest.reason)
          .isEqualTo("This seems to be a legitimate question. Please answer it.");
    }

    @Test
    void acceptTheContest() throws JsonProcessingException {
      questionEvaluation.feasibleQuestion = false;
      mockChatCompletionAndReturnFunctionCall(
          "evaluate_question", new ObjectMapper().writeValueAsString(questionEvaluation));
      QuizQuestionContestResult contest = aiAdvisorService.contestQuestion(quizQuestionEntity);
      assertFalse(contest.rejected);
      Assertions.assertThat(contest.reason).isEqualTo("what a horrible question!");
    }

    @Test
    void noFunctionCallInvoked() throws JsonProcessingException {
      Single<ChatCompletionResult> toBeReturned =
          Single.just(
              makeMe
                  .openAiCompletionResult()
                  .functionCall("", new ObjectMapper().readTree(""))
                  .please());
      mockChatCompletionAndMatchFunctionCall("evaluate_question", toBeReturned);
      assertThrows(
          RuntimeException.class, () -> aiAdvisorService.contestQuestion(quizQuestionEntity));
    }

    private Single<ChatCompletionResult> buildCompletionResultForFunctionCall(String jsonString)
        throws JsonProcessingException {
      return Single.just(
          makeMe
              .openAiCompletionResult()
              .functionCall("", new ObjectMapper().readTree(jsonString))
              .please());
    }

    private void mockChatCompletionForGPT3_5MessageOnly(String result) {
      Single<ChatCompletionResult> just =
          Single.just(makeMe.openAiCompletionResult().choice(result).please());

      doReturn(just)
          .when(openAiApi)
          .createChatCompletion(argThat(request -> request.getFunctions() == null));
    }

    private void mockChatCompletionAndReturnFunctionCall(String functionName, String result)
        throws JsonProcessingException {
      mockChatCompletionAndMatchFunctionCall(
          functionName, buildCompletionResultForFunctionCall(result));
    }

    private void mockChatCompletionAndMatchFunctionCall(
        String functionName, Single<ChatCompletionResult> toBeReturned) {
      doReturn(toBeReturned)
          .when(openAiApi)
          .createChatCompletion(
              argThat(
                  request ->
                      request.getFunctions() != null
                          && request.getFunctions().get(0).getName().equals(functionName)));
    }
  }
}
