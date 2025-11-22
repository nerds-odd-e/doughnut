package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.SuggestedTitleDTO;
import com.odde.doughnut.entities.*;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.services.ai.tools.AiToolName;
import com.odde.doughnut.testability.OpenAIChatCompletionMock;
import com.openai.client.OpenAIClient;
import com.openai.models.images.Image;
import com.openai.models.images.ImageGenerateParams;
import com.openai.models.images.ImagesResponse;
import com.openai.models.models.ModelListPage;
import com.openai.services.blocking.ImageService;
import com.openai.services.blocking.ModelService;
import com.theokanning.openai.client.OpenAiApi;
import java.util.List;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerTest extends ControllerTestBase {
  @Autowired AiController controller;

  Note note;

  @MockitoBean(name = "testableOpenAiApi")
  OpenAiApi openAiApi;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void Setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().please();
  }

  @Nested
  class GenerateImage {
    Note aNote;

    @BeforeEach
    void setup() {
      aNote = makeMe.aNote("sanskrit").creatorAndOwner(currentUser.getUser()).please();
    }

    @Test
    void askWithNoteThatCannotAccess() {
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class, () -> controller.generateImage("create an image"));
    }

    @Test
    void askEngagingStoryWithRightPrompt() {
      ImageService imageService = Mockito.mock(ImageService.class);
      when(officialClient.images()).thenReturn(imageService);
      when(imageService.generate(
              argThat(
                  (ImageGenerateParams params) -> {
                    assertEquals("create an image", params.prompt());
                    return true;
                  })))
          .thenReturn(buildOfficialImageResponse("This is an engaging story."));
      controller.generateImage("create an image");
    }

    @Test
    void generateImage() {
      ImageService imageService = Mockito.mock(ImageService.class);
      when(officialClient.images()).thenReturn(imageService);
      when(imageService.generate(Mockito.any(ImageGenerateParams.class)))
          .thenReturn(buildOfficialImageResponse("this is supposed to be a base64 image"));
      final String aiImage = controller.generateImage("create an image").b64encoded();
      assertEquals("this is supposed to be a base64 image", aiImage);
    }

    private ImagesResponse buildOfficialImageResponse(String b64Json) {
      Image image = Image.builder().b64Json(b64Json).build();
      return ImagesResponse.builder().created(1234567890L).data(List.of(image)).build();
    }
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldGetModelVersionsCorrectly() {
      // Mock the official SDK models API
      ModelService modelService = Mockito.mock(ModelService.class);
      when(officialClient.models()).thenReturn(modelService);

      com.openai.models.models.Model officialModel1 =
          com.openai.models.models.Model.builder()
              .id("gpt-4")
              .created(System.currentTimeMillis() / 1000)
              .ownedBy("openai")
              .build();
      com.openai.models.models.Model officialModel2 =
          com.openai.models.models.Model.builder()
              .id("any-model")
              .created(System.currentTimeMillis() / 1000)
              .ownedBy("openai")
              .build();

      // Mock the models list response
      // officialClient.models() returns ModelService, list() returns ModelListPage, data() returns
      // List<Model>
      var modelsList = List.of(officialModel1, officialModel2);
      ModelListPage mockListPage = Mockito.mock(ModelListPage.class);
      when(modelService.list()).thenReturn(mockListPage);
      when(mockListPage.data()).thenReturn(modelsList);

      assertThat(controller.getAvailableGptModels()).contains("gpt-4");
    }
  }

  @Nested
  class SuggestNoteTitle {
    Note testNote;
    OpenAIChatCompletionMock openAIChatCompletionMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().creatorAndOwner(currentUser.getUser()).please();
      openAIChatCompletionMock = new OpenAIChatCompletionMock(officialClient);
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
      ArgumentCaptor<com.openai.models.chat.completions.ChatCompletionCreateParams> paramsCaptor =
          ArgumentCaptor.forClass(
              com.openai.models.chat.completions.ChatCompletionCreateParams.class);
      verify(openAIChatCompletionMock.completionService()).create(paramsCaptor.capture());
      boolean hasInstruction =
          paramsCaptor.getValue().messages().stream()
              .map(Object::toString)
              .anyMatch(msg -> msg.contains("Please suggest a better title for the note"));
      MatcherAssert.assertThat(
          "A message should contain the Question Designer instruction", hasInstruction, is(true));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.suggestTitle(testNote));
    }
  }
}
