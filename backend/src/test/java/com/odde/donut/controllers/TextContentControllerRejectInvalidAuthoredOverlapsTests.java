package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.FrontmatterOverlaps;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextContentControllerRejectInvalidAuthoredOverlapsTests
    extends TextContentControllerTestBase {
  String initialContent = "unchanged body";

  @BeforeEach
  void setupNoteContent() {
    note.setContent(initialContent);
    makeMe.entityPersister.save(note);
  }

  @Test
  void rejects_scalar_overlaps_value() {
    ApiException thrown =
        assertThrows(
            ApiException.class,
            () ->
                controller.updateNoteContent(
                    note, contentDto("---\noverlaps: \"[[Other]]\"\n---\n\nbody")));

    assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        thrown.getErrorBody().getErrors().get("overlaps"),
        equalTo(FrontmatterOverlaps.AUTHORED_OVERLAPS_MESSAGE));
    makeMe.refresh(note);
    assertThat(note.getContent(), equalTo(initialContent));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        ---
        overlaps:
          - plain
        ---

        body
        """,
        """
        ---
        overlaps:
          - "[[Other]]"
          - "[["
        ---

        body
        """,
        """
        ---
        overlaps:
          - /Folder/Title.md
        ---

        body
        """
      })
  void rejects_invalid_overlaps_list_items(String content) {
    ApiException thrown =
        assertThrows(
            ApiException.class, () -> controller.updateNoteContent(note, contentDto(content)));

    assertThat(
        thrown.getErrorBody().getErrors().get("overlaps"),
        equalTo(FrontmatterOverlaps.AUTHORED_OVERLAPS_MESSAGE));
  }

  @Test
  void accepts_valid_overlaps_wiki_link_list() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        type: Note
        overlaps:
          - "[[Other Note]]"
          - "[[Shared Notebook:Hue|display]]"
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
  }

  @Test
  void accepts_valid_overlaps_path_markdown_list_item() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        type: Note
        overlaps:
          - "[Title](/Folder/Title.md)"
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
  }
}
