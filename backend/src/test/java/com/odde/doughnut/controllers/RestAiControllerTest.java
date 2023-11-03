package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.odde.doughnut.controllers.json.AiCompletion;
import com.odde.doughnut.controllers.json.AiCompletionParams;
import com.odde.doughnut.controllers.json.CurrentModelVersionResponse;
import com.odde.doughnut.controllers.json.ModelVersionOption;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.models.UserModel;
import com.odde.doughnut.testability.MakeMe;
import com.theokanning.openai.OpenAiApi;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.completion.chat.ChatCompletionResult;
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

  AiCompletionParams params =
      new AiCompletionParams() {
        {
          this.prompt = "describe Earth";
        }
      };

  @BeforeEach
  void Setup() {
    currentUser = makeMe.aUser().toModelPlease();
    note = makeMe.aNote().please();
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
                    assertThat(request.getMessages()).hasSize(4);
                    assertEquals("describe Earth", request.getMessages().get(3).getContent());
                    assertThat(request.getMessages().get(1).getContent())
                        .contains("Context path: cosmos › solar system");
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
                    assertThat(request.getMessages()).hasSize(5);
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

      List<ModelVersionOption> expected = new ArrayList<>();
      ModelVersionOption modelVersionOption = new ModelVersionOption("gpt-4", "gpt-4", "gpt-4");
      expected.add(modelVersionOption);

      when(openAiApi.listModels()).thenReturn(Single.just(fakeResponse));
      List<ModelVersionOption> actual = controller.getModelVersions();
      assertEquals(expected, actual);
    }
  }

  @Nested
  class GetCurrentModelVersions {
    @Test
    void ShouldUseGPT35ByDefault() {
      CurrentModelVersionResponse currentModelVersions = controller.getCurrentModelVersions();
      assertEquals(
          "gpt-3.5-turbol", currentModelVersions.getCurrentQuestionGenerationModelVersion());
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
}
