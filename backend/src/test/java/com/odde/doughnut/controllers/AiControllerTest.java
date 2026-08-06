package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.controllers.dto.SuggestedTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.OpenAiNotAvailableException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.TitleReplacement;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.ResponseTextConfig;
import com.openai.models.responses.StructuredResponseCreateParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class GetModelVersions {

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
    }

    private void stubSuggestedTitle(String title) {
      TitleReplacement suggestedTopic = new TitleReplacement();
      suggestedTopic.setNewTitle(title);
      openAiStructuredResponseMock.stubStructuredResponse(suggestedTopic);
    }

    @Test
    void shouldSanitizePathSeparatorsInSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      stubSuggestedTitle("TCP/IP: Overview");

      SuggestedTitleDTO result = controller.suggestTitle(testNote);

      assertThat(result.getTitle()).isEqualTo("TCP／IP： Overview");
    }

    @Test
    void shouldTrimSurroundingWhitespaceFromSuggestedTitle()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      stubSuggestedTitle("\u3000Suggested Title\u3000");

      SuggestedTitleDTO result = controller.suggestTitle(testNote);

      assertThat(result.getTitle()).isEqualTo("Suggested Title");
    }

    @Test
    void shouldCallResponsesApiWithStructuredInstructions()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      stubSuggestedTitle("Suggested Title");

      SuggestedTitleDTO result = controller.suggestTitle(testNote);

      assertThat(result.getTitle()).isEqualTo("Suggested Title");
      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<TitleReplacement>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<TitleReplacement> params = paramsCaptor.getValue();
      assertThat(params.rawParams().instructions().orElse(""))
          .contains("Please suggest a better title for the note");
      assertThat(params.rawParams().text().flatMap(ResponseTextConfig::format)).isPresent();
    }

    @Test
    void shouldReturnNullTitleWhenAiReturnsNoToolCall()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(null);

      assertThat(controller.suggestTitle(testNote).getTitle()).isNull();
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      currentUser.setUser(null);
      assertThrows(ResponseStatusException.class, () -> controller.suggestTitle(testNote));
    }
  }
}
