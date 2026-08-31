package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;

class TextContentControllerUpdateNoteTitleTests extends TextContentControllerTestBase {
  NoteUpdateTitleDTO noteUpdateTitleDTO = titleDto("new title");

  @Test
  void shouldBeAbleToSaveNoteTitle() throws UnexpectedNoAccessRightException {
    NoteRealm response = controller.updateNoteTitle(note, noteUpdateTitleDTO);
    assertThat(response.getId(), equalTo(note.getId()));
    assertThat(response.getNote().getTitle(), equalTo("new title"));
  }

  @Test
  void shouldPersistTitleWithoutSurroundingCrLfFromJson() throws Exception {
    ObjectMapper objectMapper = new ObjectMapperConfig().objectMapper();
    NoteUpdateTitleDTO titleDto =
        objectMapper.readValue("{\"newTitle\": \"\\r\\nAfter\\r\\n\"}", NoteUpdateTitleDTO.class);
    assertThat(controller.updateNoteTitle(note, titleDto).getNote().getTitle(), equalTo("After"));
  }

  @Test
  void shouldNotAllowOthersToChange() {
    Note other = makeMe.aNote("another").notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateNoteTitle(other, noteUpdateTitleDTO));
  }
}
