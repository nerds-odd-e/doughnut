package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRefinementLayoutSelectionRequestDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItem;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AiControllerExtractNoteTestSupport {

  private AiControllerExtractNoteTestSupport() {}

  static Note newRootNoteWithExtractableContent(MakeMe makeMe, User user) {
    Note note = makeMe.aNote().notebookOwnedBy(user).please();
    note.setContent("Original content with a key suggestion to extract.");
    return note;
  }

  static NoteRefinementLayout layoutWithItem(String id, String text) {
    return new NoteRefinementLayout(
        List.of(new NoteRefinementLayoutItem(id, text, false, List.of())));
  }

  static NoteRefinementLayoutSelectionRequestDTO layoutSelectionRequest(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    NoteRefinementLayoutSelectionRequestDTO requestDTO =
        new NoteRefinementLayoutSelectionRequestDTO();
    requestDTO.setLayout(layout);
    requestDTO.setSelectedItemIds(selectedItemIds);
    return requestDTO;
  }

  static NoteExtractionResult extractionResult(
      String newTitle, String newContent, String updatedOriginalNoteContent) {
    NoteExtractionResult result = new NoteExtractionResult();
    result.setNewNoteTitle(newTitle);
    result.setNewNoteContent(newContent);
    result.setUpdatedOriginalNoteContent(updatedOriginalNoteContent);
    return result;
  }

  static Stream<List<String>> invalidSelectedItemIds() {
    return Stream.of(null, List.of(), List.of("missing-id"));
  }

  static void assertResponseStatus(Executable action, HttpStatus expected) {
    assertThat(assertThrows(ResponseStatusException.class, action).getStatusCode())
        .isEqualTo(expected);
  }
}
