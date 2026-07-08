package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.repositories.NoteRepository;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

class AiControllerExtractNotePreviewTest extends ControllerTestBase {
  @Autowired AiController controller;
  @Autowired NoteRepository noteRepository;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExtractNotePreview {
    OpenAiStructuredResponseMock openAiStructuredResponseMock;

    @BeforeEach
    void setup() {
      openAiStructuredResponseMock = new OpenAiStructuredResponseMock(officialClient);
    }

    @Test
    void shouldReturnExtractionPreviewWithoutPersisting()
        throws UnexpectedNoAccessRightException, JsonProcessingException {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      String originalContent = testNote.getContent();
      long noteCountBefore = noteRepository.count();

      openAiStructuredResponseMock.stubStructuredResponse(
          extractionResult(
              "Extracted Note",
              "Expanded content for the new note.",
              "Updated parent with summary."));

      NoteRefinementLayout layout = layoutWithItem("p1", "key suggestion to extract");
      NoteExtractionResult response =
          controller.extractNotePreview(testNote, layoutSelectionRequest(layout, List.of("p1")));

      assertThat(response.getNewNoteTitle()).isEqualTo("Extracted Note");
      assertThat(response.getNewNoteContent()).isEqualTo("Expanded content for the new note.");
      assertThat(response.getUpdatedOriginalNoteContent())
          .isEqualTo("Updated parent with summary.");
      assertThat(noteRepository.count()).isEqualTo(noteCountBefore);
      makeMe.entityPersister.refresh(testNote);
      assertThat(testNote.getContent()).isEqualTo(originalContent);
    }
  }
}
