package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.json.AiCompletion;
import com.odde.doughnut.entities.json.AiCompletionRequest;
import com.odde.doughnut.entities.json.QuizQuestion;
import com.odde.doughnut.factoryServices.quizFacotries.QuizQuestionNotPossibleException;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.openAiApis.OpenAIChatAboutNoteRequestBuilder;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import io.reactivex.Single;
import java.util.List;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(locations = {"classpath:repository.xml"})
@Transactional
class RestAiControllerTest {
  RestAiController controller;
  UserModel currentUser;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;

  AiCompletionRequest params =
      new AiCompletionRequest() {
        {
          this.prompt = "describe Earth";
        }
      };

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().asHeadNoteOfANotebook().please();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Nested
  class AskSuggestion {
    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModel())
                  .getCompletion(note, params));
    }

    @Test
    void askSuggestionWithRightPrompt() {
      Note cosmos = makeMe.aNote("cosmos").please();
      Note solar = makeMe.aNote("solar system").under(cosmos).please();
      Note earth = makeMe.aNote("Earth").under(solar).please();
      when(openAiApi.createChatCompletion(
              argThat(
                  request -> {
                    assertThat(request.getMaxTokens()).isLessThan(200);
                    assertThat(request.getMessages()).hasSize(2);
                    assertEquals("describe Earth", request.getMessages().get(1).getContent());
                    assertThat(request.getMessages().get(0).getContent())
                        .contains("Current context of the note: cosmos › solar system");
                    return true;
                  })))
          .thenReturn(buildCompletionResult("blue planet"));
      controller.getCompletion(earth, params);
    }

    @Test
    void askSuggestionWithIncompleteAssistantMessage() {
      params.incompleteContent = "What goes up,";
      when(openAiApi.createChatCompletion(
              argThat(
                  request -> {
                    assertThat(request.getMessages()).hasSize(3);
                    return true;
                  })))
          .thenReturn(buildCompletionResult("blue planet"));
      controller.getCompletion(note, params);
    }

    @Test
    void askSuggestionAndUseResponse() {
      when(openAiApi.createChatCompletion(any())).thenReturn(buildCompletionResult("blue planet"));
      AiCompletion aiCompletion = controller.getCompletion(note, params);
      assertEquals("blue planet", aiCompletion.getMoreCompleteContent());
    }
  }

  @Nested
  class AskEngagingStory {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote = makeMe.aNote("sanskrit").creatorAndOwner(currentUser).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new RestAiController(openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModel())
                  .generateImage(params));
    }

    @Test
    void askEngagingStoryWithRightPrompt() {
      when(openAiApi.createImage(
              argThat(
                  request -> {
                    assertEquals("describe Earth", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildImageResult("This is an engaging story."));
      controller.generateImage(params);
    }

    @Test
    void generateImage() {
      when(openAiApi.createImage(Mockito.any()))
          .thenReturn(buildImageResult("this is supposed to be a base64 image"));
      final String aiImage = controller.generateImage(params).b64encoded();
      assertEquals("this is supposed to be a base64 image", aiImage);
    }
  }

  @Nested
  class GenerateQuestion {
    String jsonQuestion =
        """
        {"stem": "What is the first color in the rainbow?", "correctChoiceIndex": 0, "choices": ["red", "black", "green"]}
        """;

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () -> {
            RestAiController restAiController =
                new RestAiController(
                    openAiApi, makeMe.modelFactoryService, makeMe.aNullUserModel());
            restAiController.generateQuestion(note);
          });
    }

    @Test
    void createQuizQuestion() throws JsonProcessingException, QuizQuestionNotPossibleException {
      when(openAiApi.createChatCompletion(any()))
          .thenReturn(buildCompletionResultForFunctionCall(jsonQuestion));
      QuizQuestion quizQuestion = controller.generateQuestion(note);
      assertThat(quizQuestion.stem).contains("What is the first color in the rainbow?");
    }

    @Test
    void createQuizQuestionFailed() throws JsonProcessingException {
      when(openAiApi.createChatCompletion(any()))
          .thenReturn(buildCompletionResultForFunctionCall("{\"stem\": \"\"}"));
      assertThrows(QuizQuestionNotPossibleException.class, () -> controller.generateQuestion(note));
    }

    @Nested
    class WithMockedChatCompletion {
      ChatCompletionRequest requestForQuestion;

      @BeforeEach
      void setup() throws JsonProcessingException {
        mockChatCompletionWithFunctionCall(
            "ask_single_answer_multiple_choice_question",
            (request) -> requestForQuestion = request,
            jsonQuestion);
      }

      @Test
      void usingABiggerMaxToken() throws QuizQuestionNotPossibleException {
        controller.generateQuestion(note);
        assertThat(requestForQuestion.getMaxTokens()).isGreaterThan(1000);
        assertThat(requestForQuestion.getModel()).isEqualTo("gpt-4");
      }

      @Nested
      class WhenTheContentIsLong {
        OpenAIChatAboutNoteRequestBuilder.QuestionEvaluation questionEvaluation =
            new OpenAIChatAboutNoteRequestBuilder.QuestionEvaluation();

        @BeforeEach
        void setup() throws JsonProcessingException {
          questionEvaluation.correctChoices = new int[] {0};
          note.setDescription(makeMe.aStringOfLength(1000));
        }

        @Test
        void usingGPT3_5() throws QuizQuestionNotPossibleException, JsonProcessingException {
          mockChatCompletionWithFunctionCall(
              "evaluate_question", null, new ObjectMapper().writeValueAsString(questionEvaluation));
          controller.generateQuestion(note);
          assertThat(requestForQuestion.getModel()).isEqualTo("gpt-3.5-turbo-16k");
        }

        @Test
        void askAiToEvaluateTheQuestionAgain()
            throws QuizQuestionNotPossibleException, JsonProcessingException {
          mockChatCompletionWithFunctionCall(
              "evaluate_question",
              (request) -> assertThat(request.getModel()).isEqualTo("gpt-3.5-turbo-16k"),
              new ObjectMapper().writeValueAsString(questionEvaluation));
          controller.generateQuestion(note);
        }

        @Test
        void tryWithGPT4IfTheEvaluationIsIncorrect()
            throws QuizQuestionNotPossibleException, JsonProcessingException {
          questionEvaluation.correctChoices = new int[] {0, 1};
          mockChatCompletionWithFunctionCall(
              "evaluate_question", null, new ObjectMapper().writeValueAsString(questionEvaluation));
          controller.generateQuestion(note);
          assertThat(requestForQuestion.getModel()).isEqualTo("gpt-4");
        }
      }
    }

    private void mockChatCompletionWithFunctionCall(
        String functionName, Consumer<ChatCompletionRequest> consumer, String result)
        throws JsonProcessingException {
      when(openAiApi.createChatCompletion(
              argThat(
                  request -> {
                    if (request == null) return false;
                    boolean askSingleAnswerMultipleChoiceQuestion =
                        request.getFunctions().get(0).getName().equals(functionName);
                    if (askSingleAnswerMultipleChoiceQuestion && consumer != null) {
                      consumer.accept(request);
                    }
                    return askSingleAnswerMultipleChoiceQuestion;
                  })))
          .thenReturn(buildCompletionResultForFunctionCall(result));
    }
  }

  private Single<ImageResult> buildImageResult(String s) {
    ImageResult result = new ImageResult();
    Image image = new Image();
    image.setB64Json(s);
    result.setData(List.of(image));
    return Single.just(result);
  }

  private Single<ChatCompletionResult> buildCompletionResult(String text) {
    return Single.just(makeMe.openAiCompletionResult().choice(text).please());
  }

  private Single<ChatCompletionResult> buildCompletionResultForFunctionCall(String jsonString)
      throws JsonProcessingException {
    return Single.just(
        makeMe
            .openAiCompletionResult()
            .functionCall("", new ObjectMapper().readTree(jsonString))
            .please());
  }
}
