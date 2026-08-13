package com.odde.doughnut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.doughnut.configs.ObjectMapperConfig;
import com.odde.doughnut.controllers.dto.NoteRealm;
import com.odde.doughnut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.doughnut.entities.Note;
import com.odde.doughnut.exceptions.UnexpectedNoAccessRightException;
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
  void shouldPreserveRecallSettingsWhenUpdatingTitle() throws UnexpectedNoAccessRightException {
    note.getRecallSetting().setLevel(3);
    makeMe.refresh(note);

    controller.updateNoteTitle(note, noteUpdateTitleDTO);

    makeMe.refresh(note);
    assertThat(note.getRecallSetting().getLevel(), equalTo(3));
  }

  @Test
  void shouldNotAllowOthersToChange() {
    Note other = makeMe.aNote("another").notebookOwnedBy(makeMe.aUser().please()).please();
    assertThrows(
        UnexpectedNoAccessRightException.class,
        () -> controller.updateNoteTitle(other, noteUpdateTitleDTO));
  }
}
