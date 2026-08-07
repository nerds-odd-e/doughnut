package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.doughnut.algorithms.FrontmatterAliases;
import com.odde.doughnut.controllers.dto.ApiError;
import com.odde.doughnut.exceptions.ApiException;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TextContentControllerRejectInvalidAuthoredAliasesTests extends TextContentControllerTestBase {
  String initialContent = "unchanged body";

  @BeforeEach
  void setupNoteContent() {
    note.setContent(initialContent);
    makeMe.entityPersister.save(note);
  }

  @Test
  void rejects_scalar_aliases_value() {
    ApiException thrown =
        assertThrows(
            ApiException.class,
            () ->
                controller.updateNoteContent(note, contentDto("---\naliases: color\n---\n\nbody")));

    assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        thrown.getErrorBody().getErrors().get("aliases"),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
    makeMe.refresh(note);
    assertThat(note.getContent(), equalTo(initialContent));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        """
        ---
        aliases:
          - color
          - bad|alias
        ---

        body
        """,
        """
        ---
        aliases:
          - color
          - "[["
        ---

        body
        """
      })
  void rejects_invalid_alias_list_items(String content) {
    ApiException thrown =
        assertThrows(
            ApiException.class, () -> controller.updateNoteContent(note, contentDto(content)));

    assertThat(
        thrown.getErrorBody().getErrors().get("aliases"),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
  }

  @Test
  void accepts_valid_alias_list() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        aliases:
          - color
          - hue
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
  }

  @Test
  void rejects_well_formed_wiki_link_alias_items_on_save() {
    String content =
        """
        ---
        aliases:
          - color
          - "[[Other Note]]"
          - "[[Shared Notebook:Hue|display]]"
        ---

        body
        """;
    ApiException thrown =
        assertThrows(
            ApiException.class, () -> controller.updateNoteContent(note, contentDto(content)));

    assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        thrown.getErrorBody().getErrors().get("aliases"),
        equalTo(FrontmatterAliases.AUTHORED_ALIASES_MESSAGE));
    makeMe.refresh(note);
    assertThat(note.getContent(), equalTo(initialContent));
  }
}
