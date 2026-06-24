package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.SuggestedTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.models.ModelListPage;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import com.openai.services.blocking.ModelService;
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

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void Setup() {
    currentUser.setUser(makeMe.aUser().please());
    note = makeMe.aNote().please();
  }

  @Nested
  class GetModelVersions {

    @Test
    void shouldGetModelVersionsCorrectly() {
      ModelService modelService = Mockito.mock(ModelService.class);
      when(officialClient.models()).thenReturn(modelService);

      com.openai.models.models.Model officialModel =
          com.openai.models.models.Model.builder()
              .id("gpt-4")
              .created(1L)
              .ownedBy("openai")
              .build();

      var modelsList = List.of(officialModel);
      ModelListPage mockListPage = Mockito.mock(ModelListPage.class);
      when(modelService.list()).thenReturn(mockListPage);
      when(mockListPage.data()).thenReturn(modelsList);

      assertThat(controller.getAvailableGptModels()).contains("gpt-4");
    }

    @Test
    void shouldThrowWhenOpenAiNotAvailable() {
      testabilitySettings.setOpenAiTokenOverride("");
      assertThrows(OpenAiNotAvailableException.class, () -> controller.getAvailableGptModels());
    }
  }

  @Nested
  class SuggestNoteTitle {
    Note testNote;
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      testNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle("Suggested Title");
      openAiStructuredResponseMock.stubStructuredResponse(suggestedTopic);
    }

    @Test
    void shouldSanitizePathSeparatorsInSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle("TCP/IP: Overview");
      openAiStructuredResponseMock.stubStructuredResponse(suggestedTopic);

      SuggestedTitleDTO result = controller.suggestTitle(testNote);

      assertThat(result.getTitle()).isEqualTo("TCP／／IP： Overview");
    }

    @Test
    void shouldTrimSurroundingWhitespaceFromSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle("\u3000Suggested Title\u3000");
      openAiStructuredResponseMock.stubStructuredResponse(suggestedTopic);

      SuggestedTitleDTO result = controller.suggestTitle(testNote);

      assertThat(result.getTitle()).isEqualTo("Suggested Title");
    }

    @Test
    void shouldCallResponsesApiWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      SuggestedTitleDTO result = controller.suggestTitle(testNote);
      assertThat(result.getTitle()).isEqualTo("Suggested Title");
      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<TitleReplacement>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<TitleReplacement> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      MatcherAssert.assertThat(
          "Instructions should contain the title suggestion prompt",
          instructions.contains("Please suggest a better title for the note"),
          is(true));
      MatcherAssert.assertThat(
          "Should use Responses structured text format",
          params.rawParams().text().flatMap(ResponseTextConfig::format).isPresent(),
          is(true));
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.suggestTitle(testNote));
    }
  }
}
