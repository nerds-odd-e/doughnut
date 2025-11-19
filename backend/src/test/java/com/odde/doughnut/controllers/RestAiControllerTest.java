package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.currentUser.CurrentUser;
import com.odde.doughnut.controllers.dto.SuggestedTitleDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.AuthorizationService;
import com.odde.doughnut.services.GlobalSettingsService;
import com.odde.doughnut.services.NotebookAssistantForNoteServiceFactory;
import com.odde.doughnut.services.ai.OtherAiServices;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.MakeMe;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.theokanning.openai.OpenAiResponse;
import com.theokanning.openai.client.OpenAiApi;
import com.theokanning.openai.image.Image;
import com.theokanning.openai.image.ImageResult;
import com.theokanning.openai.model.Model;
import io.reactivex.Single;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class AiControllerTest {
  AiController controller;
  CurrentUser currentUser;

  Note note;
  @Mock OpenAiApi openAiApi;
  @Autowired MakeMe makeMe;
  @Autowired AuthorizationService authorizationService;
  NotebookAssistantForNoteServiceFactory notebookAssistantForNoteServiceFactory;

  @BeforeEach
  void Setup() {
    GlobalSettingsService globalSettingsService =
        new GlobalSettingsService(makeMe.modelFactoryService);
    notebookAssistantForNoteServiceFactory =
        new NotebookAssistantForNoteServiceFactory(
            openAiApi, globalSettingsService, getTestObjectMapper());
    currentUser = new CurrentUser(makeMe.aUser().please());
    note = makeMe.aNote().please();
    controller =
        new AiController(
            notebookAssistantForNoteServiceFactory,
            new OtherAiServices(openAiApi),
            currentUser,
            authorizationService);
  }

  private com.fasterxml.jackson.databind.ObjectMapper getTestObjectMapper() {
    return new ObjectMapperConfig().objectMapper();
  }

  @Nested
  class GenerateImage {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote =
          makeMe
              .aNote("sanskrit")
              .creatorAndOwner(makeMe.modelFactoryService.toUserModel(currentUser.getUser()))
              .please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      assertThrows(
          ResponseStatusException.class,
          () ->
              new AiController(
                      notebookAssistantForNoteServiceFactory,
                      new OtherAiServices(openAiApi),
                      new CurrentUser(null),
                      authorizationService)
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

    private Single<ImageResult> buildImageResult(String s) {
      ImageResult result = new ImageResult();
      Image image = new Image();
      image.setB64Json(s);
      result.setData(List.of(image));
      return Single.just(result);
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

  @Nested
  class SuggestNoteTitle {
    Note testNote;
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      testNote =
          makeMe
              .aNote()
              .creatorAndOwner(makeMe.modelFactoryService.toUserModel(currentUser.getUser()))
              .please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(openAiApi);
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle("Suggested Title");
      openAIChatCompletionMock.mockChatCompletionAndReturnToolCall(
          suggestedTopic, AiToolName.SUGGEST_NOTE_TITLE.getValue());
    }

    @Test
    void shouldReturnSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      SuggestedTitleDTO result = controller.suggestTitle(testNote);
      assertThat(result.getTitle()).isEqualTo("Suggested Title");
    }

    @Test
    void shouldCallChatCompletionWithRightMessage()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      controller.suggestTitle(testNote);
      verify(openAiApi)
          .createChatCompletion(
              argThat(
                  request -> {
                    String userMessage =
                        request.getMessages().stream()
                            .filter(m -> "user".equals(m.getRole()))
                            .findFirst()
                            .map(m -> m.getTextContent())
                            .orElse("");
                    assertThat(userMessage).contains("Please suggest a better title for the note");
                    return true;
                  }));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      controller =
          new AiController(
              notebookAssistantForNoteServiceFactory,
              new OtherAiServices(openAiApi),
              new CurrentUser(null),
              authorizationService);

      assertThrows(ResponseStatusException.class, () -> controller.suggestTitle(testNote));
    }
  }
}
