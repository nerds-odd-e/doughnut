package com.odde.donut.controllers;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.odde.donut.configs.ObjectMapperConfig;
import com.odde.donut.controllers.dto.NoteRealm;
import com.odde.donut.controllers.dto.NoteUpdateTitleDTO;
import com.odde.donut.controllers.dto.WikiLink;
import com.odde.donut.entities.Folder;
import com.odde.donut.entities.Note;
import com.odde.donut.exceptions.UnexpectedNoAccessRightException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

class TextContentControllerUpdateNoteTitleTests extends TextContentControllerTestBase {
  @Autowired NoteController noteController;

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

  @Test
  void shouldReresolveNotebookShorthandsWhenRenameIntroducesOrRemovesACollision()
      throws UnexpectedNoAccessRightException {
    Note target = makeMe.aNote().notebookOwnedBy(currentUser.getUser()).title("Target").please();
    Folder otherFolder =
        makeMe.aFolder().notebook(target.getNotebook()).name("Other Folder").please();
    Note referrer = makeMe.aNote().underSameNotebookAs(target).content("See [[Target]].").please();
    Note namesake = makeMe.aNote().folder(otherFolder).title("Other").please();
    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));

    controller.updateNoteTitle(namesake, titleDto("Target"));

    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.AMBIGUOUS));

    controller.updateNoteTitle(namesake, titleDto("Other"));

    assertThat(
        noteController.showNote(referrer).getWikiLinks().get(0).getResolution(),
        equalTo(WikiLink.Resolution.RESOLVED));
  }
}
