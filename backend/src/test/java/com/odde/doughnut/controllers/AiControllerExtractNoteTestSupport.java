package com.odde.doughnut.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.controllers.dto.NoteRefinementLayoutSelectionRequestDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.entities.User;
import com.odde.doughnut.services.ai.NoteExtractionResult;
import com.odde.doughnut.services.ai.NoteRefinementLayout;
import com.odde.doughnut.services.ai.NoteRefinementLayoutItems;
import com.odde.doughnut.testability.MakeMe;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.function.Executable;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class AiControllerExtractNoteTestSupport {

  static final String EXTRACTABLE_CONTENT = "Original content with a key suggestion to extract.";

  private AiControllerExtractNoteTestSupport() {}

  static Note newRootNoteWithExtractableContent(MakeMe makeMe, User user) {
    return makeMe.aNote().notebookOwnedBy(user).content(EXTRACTABLE_CONTENT).please();
  }

  static NoteRefinementLayout layoutWithItem(String id, String text) {
    return new NoteRefinementLayout(List.of(NoteRefinementLayoutItems.leaf(id, text)));
  }

  static NoteRefinementLayout nestedLayout(
      String parentId,
      String parentText,
      String childId,
      String childText,
      String siblingId,
      String siblingText) {
    return new NoteRefinementLayout(
        List.of(
            NoteRefinementLayoutItems.parent(
                parentId, parentText, List.of(NoteRefinementLayoutItems.leaf(childId, childText))),
            NoteRefinementLayoutItems.leaf(siblingId, siblingText)));
  }

  static NoteRefinementLayoutSelectionRequestDTO layoutSelectionRequest(
      NoteRefinementLayout layout, List<String> selectedItemIds) {
    NoteRefinementLayoutSelectionRequestDTO requestDTO =
        new NoteRefinementLayoutSelectionRequestDTO();
    requestDTO.setRefinementLayout(layout);
    requestDTO.setSelectedItemIds(selectedItemIds);
    return requestDTO;
  }

  static NoteRefinementLayoutSelectionRequestDTO selectSingleLayoutItem(String id, String text) {
    return layoutSelectionRequest(layoutWithItem(id, text), List.of(id));
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

  static void assertBadRequestContaining(Executable action, String substring) {
    ResponseStatusException ex = assertThrows(ResponseStatusException.class, action);
    assertThat(ex.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    assertThat(ex.getReason()).contains(substring);
  }
}
