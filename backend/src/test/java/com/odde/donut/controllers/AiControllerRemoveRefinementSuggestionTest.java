package com.odde.donut.controllers;

import static com.odde.donut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.donut.controllers.dto.RefinedContentResponseDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import com.odde.donut.services.ai.NoteRefinementLayout;
import com.odde.donut.services.ai.RegeneratedNoteContent;
import com.odde.donut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import com.openai.models.responses.StructuredResponseCreateParams;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerRemoveRefinementSuggestionTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  OpenAiStructuredResponseMock openAiStructuredResponseMock;
  Note testNote;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
    testNote = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).please();
    openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
  }

  @Nested
  class RemoveRefinementSuggestion {
    private NoteRefinementLayout sampleLayout() {
      return nestedLayout(
          "p1", "Main concept", "p1-1", "suggestion to remove", "p2", "Other point");
    }

    @Test
    void shouldReturnRegeneratedContentAfterRemovingSelectedLayoutPoints()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      openAiStructuredResponseMock.stubStructuredResponse(
          new RegeneratedNoteContent("Remaining content."));
      String originalContent = "Original with a suggestion to remove.";
      testNote.setContent(originalContent);

      RefinedContentResponseDTO response =
          controller.removeRefinementSuggestion(
              testNote, layoutSelectionRequest(sampleLayout(), List.of("p1-1", "p2")));

      assertThat(response.getContent()).isEqualTo("Remaining content.");
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);

      @SuppressWarnings({"unchecked", "rawtypes"})
      ArgumentCaptor<StructuredResponseCreateParams<RegeneratedNoteContent>> paramsCaptor =
          ArgumentCaptor.forClass((Class) StructuredResponseCreateParams.class);
      verify(openAiStructuredResponseMock.responseService()).create(paramsCaptor.capture());
      StructuredResponseCreateParams<RegeneratedNoteContent> params = paramsCaptor.getValue();
      String instructions = params.rawParams().instructions().orElse("");
      assertThat(params.rawParams().maxOutputTokens()).isEqualTo(Optional.of(2000L));
      assertThat(instructions)
          .contains("Full refinement layout:")
          .contains("\"id\" : \"p1-1\"")
          .contains("Selected refinement layout item ids to remove")
          .contains("[p1-1, p2]")
          .contains("- p1-1: \"suggestion to remove\"")
          .contains("- p2: \"Other point\"");
    }

    @Test
    void shouldThrowWhenSelectedItemIdsIsEmpty() {
      testNote.setContent("Some note content.");
      assertBadRequestContaining(
          () ->
              controller.removeRefinementSuggestion(
                  testNote, layoutSelectionRequest(sampleLayout(), List.of())),
          "selectedItemIds cannot be empty");
    }

    @Test
    void shouldThrowWhenNoteContentIsEmpty() {
      testNote.setContent("");
      assertBadRequestContaining(
          () ->
              controller.removeRefinementSuggestion(
                  testNote, layoutSelectionRequest(sampleLayout(), List.of("p1-1"))),
          "Note content cannot be empty");
    }
  }
}
