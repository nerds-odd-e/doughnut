package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.odde.donut.algorithms.FrontmatterNoteLevel;
import com.odde.donut.controllers.dto.ApiError;
import com.odde.donut.entities.NoteLevelIndex;
import com.odde.donut.entities.repositories.NoteLevelIndexRepository;
import com.odde.donut.exceptions.ApiException;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerRejectInvalidAuthoredNoteLevelTests
    extends TextContentControllerTestBase {
  static final String INITIAL_CONTENT =
      """
      ---
      type: Note
      note_level: 2
      ---

      unchanged body
      """;

  @Autowired NoteLevelIndexRepository noteLevelIndexRepository;

  @BeforeEach
  void setupNoteContent() {
    note.setContent(INITIAL_CONTENT);
    makeMe.entityPersister.save(note);
    makeMe.noteLevelIndexService.refreshForNote(note);
  }

  @Test
  void rejects_zero_note_level_and_leaves_markdown_and_cache_unchanged() {
    ApiException thrown =
        assertThrows(
            ApiException.class,
            () ->
                controller.updateNoteContent(
                    note, contentDto("---\ntype: Note\nnote_level: 0\n---\n\nbody")));

    assertThat(thrown.getErrorBody().getErrorType(), equalTo(ApiError.ErrorType.BINDING_ERROR));
    assertThat(
        thrown.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
    makeMe.refresh(note);
    assertThat(note.getContent(), equalTo(INITIAL_CONTENT));
    assertThat(cachedLevel(), equalTo(2));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {
        "---\nnote_level: 7\n---\n\nbody",
        "---\nnote_level: hard\n---\n\nbody",
        "---\nnote_level: 2.5\n---\n\nbody",
        "---\nnote_level: true\n---\n\nbody",
        "---\nnote_level:\n---\n\nbody",
        "---\nnote_level:\n  - 2\n---\n\nbody",
        "---\nnote_level:\n  nested: 2\n---\n\nbody",
        "---\nnote_level 2: 3\n---\n\nbody"
      })
  void rejects_invalid_note_level_shapes(String content) {
    ApiException thrown =
        assertThrows(
            ApiException.class, () -> controller.updateNoteContent(note, contentDto(content)));

    assertThat(
        thrown.getErrorBody().getErrors().get("note_level"),
        equalTo(FrontmatterNoteLevel.AUTHORED_NOTE_LEVEL_MESSAGE));
  }

  @Test
  void accepts_valid_integer_note_level() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        type: Note
        note_level: 5
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
    assertThat(cachedLevel(), equalTo(5));
  }

  @Test
  void accepts_quoted_digit_string_note_level() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        type: Note
        note_level: "3"
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
  }

  @Test
  void accepts_absent_note_level_key() throws UnexpectedNoAccessRightException {
    String content =
        """
        ---
        type: Note
        topic: physics
        ---

        body
        """;
    assertThat(
        controller.updateNoteContent(note, contentDto(content)).getNote().getContent(),
        equalTo(content));
    assertThat(noteLevelIndexRepository.findById(note.getId()).isPresent(), equalTo(false));
  }

  private int cachedLevel() {
    return noteLevelIndexRepository
        .findById(note.getId())
        .map(NoteLevelIndex::getLevel)
        .orElseThrow();
  }
}
