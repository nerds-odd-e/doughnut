package com.odde.doughnut.controllers;

import static com.odde.doughnut.controllers.AiControllerExtractNoteTestSupport.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.entities.Note;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.testability.OpenAiStructuredResponseMock;
import com.openai.client.OpenAIClient;
import java.util.List;
import java.util.stream.Stream;
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
  class ExtractNoteValidation {
    @Test
    void shouldRequireUserToBeLoggedIn() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      currentUser.setUser(null);
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertThrows(
          ResponseStatusException.class,
          () -> controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1"))));
    }

    static Stream<List<String>> invalidSelectedItemIds() {
      return AiControllerExtractNoteTestSupport.invalidSelectedItemIds();
    }

    @ParameterizedTest
    @MethodSource("invalidSelectedItemIds")
    void shouldRejectInvalidSelectedItemIds(List<String> selectedItemIds) {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, layoutSelectionRequest(layout, selectedItemIds)),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldRejectInvalidLayout() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      NoteRefinementLayout layout =
          new NoteRefinementLayout(
              List.of(new NoteRefinementLayoutItem("", "a suggestion", false, List.of())));
      assertResponseStatus(
          () -> controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1"))),
          HttpStatus.BAD_REQUEST);
    }

    @Test
    void shouldThrowWhenAiReturnsNull() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      new OpenAiStructuredResponseMock(officialClient).stubStructuredResponse(null);
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1"))),
          HttpStatus.SERVICE_UNAVAILABLE);
    }

    @Test
    void shouldRejectBlankNoteContent() {
      Note testNote = newRootNoteWithExtractableContent(makeMe, currentUser.getUser());
      testNote.setContent("");
      NoteRefinementLayout layout = layoutWithItem("p1", "a suggestion");
      assertResponseStatus(
          () -> controller.extractNote(testNote, layoutSelectionRequest(layout, List.of("p1"))),
          HttpStatus.BAD_REQUEST);
    }
  }
}
