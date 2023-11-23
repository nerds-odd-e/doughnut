package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.ai.ClarifyingQuestion;
import com.odde.doughnut.services.ai.NoteDetailsCompletion;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.chat.ChatCompletionRequest;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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

  AiCompletionParams params = new AiCompletionParams();

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().please();
    controller = new RestAiController(openAiApi, makeMe.modelFactoryService, currentUser);
  }

  @Nested
  class AskSuggestion {
    ArgumentCaptor<ChatCompletionRequest> captor =
        ArgumentCaptor.forClass(ChatCompletionRequest.class);
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
        new NoteDetailsCompletion("blue planet"));
    }

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
      controller.getCompletion(earth, params);
      verify(openAiApi).createChatCompletion(captor.capture());
      assertThat(captor.getValue().getMaxTokens()).isLessThan(200);
      assertThat(captor.getValue().getMessages()).hasSize(3);
      assertThat(captor.getValue().getMessages().get(2).getContent())
          .contains(" \"details_to_complete\" : \"\"");
      assertThat(captor.getValue().getMessages().get(1).getContent())
          .contains("Context path: cosmos › solar system");
    }

    @Test
    void askSuggestionWithRightModel() {
      new GlobalSettingsService(makeMe.modelFactoryService)
          .getGlobalSettingOthers()
          .setKeyValue(makeMe.aTimestamp().please(), "gpt-future");
      controller.getCompletion(note, params);

      verify(openAiApi).createChatCompletion(captor.capture());
      assertEquals("gpt-future", captor.getValue().getModel());
    }

    @Test
    void askSuggestionWithIncompleteAssistantMessage() {
      params.detailsToComplete = "What goes up,";
      controller.getCompletion(note, params);
      verify(openAiApi).createChatCompletion(captor.capture());
      assertThat(captor.getValue().getMessages().get(2).getContent())
          .contains(" \"details_to_complete\" : \"What goes up,\"");
    }

    @Test
    void askCompletionAndUseStopResponse() {
      AiCompletion aiCompletion = controller.getCompletion(note, params);
      assertEquals("blue planet", aiCompletion.getMoreCompleteContent());
      assertEquals("stop", aiCompletion.getFinishReason());
    }
  }

  @Nested
  class CompleteNoteDetailWithClarifyingQuestion {

    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
      openAIChatCompletionMock.mockChatCompletionAndReturnFunctionCall(
        new ClarifyingQuestion("Are you referring to American football or association football (soccer)?"));
    }

    @Test
    void askCompletionAndUseQuestionResponse() {
      params.detailsToComplete = "Football";
      AiCompletion aiCompletion = controller.getCompletion(note, params);
      assertEquals("question", aiCompletion.getFinishReason());
      assertEquals(
          "Are you referring to American football or association football (soccer)?",
          aiCompletion.getQuestion());
    }

    @Test
    void askCompletionAndUseStopResponseWithQuestionAnswer() {
      params.detailsToComplete = "Football";
      params.questionFromAI =
          "Are you referring to American football or association football (soccer)?";
      params.answerFromUser = "European Football";
      AiCompletion aiCompletion = controller.getCompletion(note, params);
      assertEquals("stop", aiCompletion.getFinishReason());
      assertEquals(
          "European football origins from England.", aiCompletion.getMoreCompleteContent());
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
                  .generateImage("create an image"));
    }

    @Test
    void askEngagingStoryWithRightPrompt() {
      when(openAiApi.createImage(
              argThat(
                  request -> {
                    assertEquals("create an image", request.getPrompt());
                    return true;
                  })))
          .thenReturn(buildImageResult("This is an engaging story."));
      controller.generateImage("create an image");
    }

    @Test
    void generateImage() {
      when(openAiApi.createImage(Mockito.any()))
          .thenReturn(buildImageResult("this is supposed to be a base64 image"));
      final String aiImage = controller.generateImage("create an image").b64encoded();
      assertEquals("this is supposed to be a base64 image", aiImage);
    }
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldGetModelVersionsCorrectly() {
      List<Model> fakeModels = new ArrayList<>();
      Model model1 = new Model();
      model1.setId("gpt-4");
      fakeModels.add(model1);
      Model model2 = new Model();
      model2.setId("any-model");
      fakeModels.add(model2);
      OpenAiResponse<Model> fakeResponse = new OpenAiResponse<>();
      fakeResponse.setData(fakeModels);

      when(openAiApi.listModels()).thenReturn(Single.just(fakeResponse));
      assertThat(controller.getAvailableGptModels()).contains("gpt-4");
    }
  }

  private Single<ImageResult> buildImageResult(String s) {
    ImageResult result = new ImageResult();
    Image image = new Image();
    image.setB64Json(s);
    result.setData(List.of(image));
    return Single.just(result);
  }
}
