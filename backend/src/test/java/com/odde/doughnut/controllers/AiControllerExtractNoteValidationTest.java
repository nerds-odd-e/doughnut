package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItems;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.server.ResponseStatusException;

class AiControllerExtractNoteValidationTest extends ControllerTestBase {
  @Autowired AiController controller;

  @MockitoBean(name = "officialOpenAiClient")
  OpenAIClient officialClient;

  @BeforeEach
  void setup() {
    currentUser.setUser(makeMe.aUser().please());
  }

  @Nested
  class ExtractNotePreviewValidation {
    private Note extractableNote() {
      return newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
    }

    @Test
    void shouldRequireUserToBeLoggedIn() {
      Note testNote = extractableNote();
      currentUser.setUser(null);
      assertThrows(
          ResponseStatusException.class,
          () ->
              controller.extractNotePreview(
                  testNote, selectSingleLayoutItem("p1", "a suggestion")));
    }

    @ParameterizedTest
    @MethodSource(
        "com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport#invalidSelectedItemIds")
    void shouldRejectInvalidSelectedItemIds(List<String> selectedItemIds) {
      assertResponseStatus(
          () ->
              controller.extractNotePreview(
                  extractableNote(),
                  layoutSelectionRequest(layoutWithItem("p1", "a suggestion"), selectedItemIds)),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidLayout() {
      NoteRefinementLayout layout =
          new NoteRefinementLayout(List.of(NoteRefinementLayoutItems.leaf("", "a suggestion")));
      assertResponseStatus(
          () ->
              controller.extractNotePreview(
                  extractableNote(), layoutSelectionRequest(layout, List.of("p1"))),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowWhenAiReturnsNull() {
      new OpenAiStructuredResponseMock(officialClient).stubStructuredResponse(null);
      assertResponseStatus(
          () ->
              controller.extractNotePreview(
                  extractableNote(), selectSingleLayoutItem("p1", "a suggestion")),
          HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldRejectBlankNoteContent() {
      Note testNote = extractableNote();
      testNote.setContent("");
      assertResponseStatus(
          () ->
              controller.extractNotePreview(testNote, selectSingleLayoutItem("p1", "a suggestion")),
          HttpStatus.BAD_REQUEST);
    }
  }
}
